package com.chaerok.backend.render.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollErrorCode;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.filmroll.service.FilmRollDevelopmentTimingService;
import com.chaerok.backend.global.aws.AwsProperties;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.entity.PhotoStatus;
import com.chaerok.backend.photo.repository.PhotoRepository;
import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.entity.RenderJobStatus;
import com.chaerok.backend.render.exception.RenderErrorCode;
import com.chaerok.backend.render.queue.RenderQueueMessage;
import com.chaerok.backend.render.repository.RenderJobRepository;
import com.chaerok.backend.visit.service.VisitRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-queue-url"
)
public class RenderJobPreparationService {

    private static final List<RenderJobStatus> ACTIVE_JOB_STATUSES =
            List.of(
                    RenderJobStatus.CREATED,
                    RenderJobStatus.QUEUED,
                    RenderJobStatus.PROCESSING
            );

    private final FilmRollRepository filmRollRepository;
    private final PhotoRepository photoRepository;
    private final RenderJobRepository renderJobRepository;
    private final AwsProperties awsProperties;
    private final VisitRequirementService visitRequirementService;
    private final FilmRollDevelopmentTimingService developmentTimingService;

    @Transactional
    public PreparedRenderJob prepare(
            Long userId,
            Long filmRollId
    ) {
        FilmRoll filmRoll = filmRollRepository
                .findByIdAndUserIdForUpdate(
                        filmRollId,
                        userId
                )
                .orElseThrow(() ->
                        new BusinessException(
                                FilmRollErrorCode.FILM_ROLL_NOT_FOUND
                        )
                );

        requireReady(filmRoll);
        requireNoActiveJob(filmRollId);

        List<Photo> photos =
                photoRepository
                        .findAllByFilmRollIdOrderBySequenceAsc(
                                filmRollId
                        );

        validatePhotos(filmRoll, photos);
        visitRequirementService.requireSatisfied(filmRollId);
        developmentTimingService.requireAvailable(filmRoll);

        RenderJob renderJob = RenderJob.create(filmRoll);
        renderJobRepository.saveAndFlush(renderJob);

        RenderQueueMessage message = createMessage(
                renderJob,
                filmRoll,
                photos,
                LocalDateTime.now()
        );

        return new PreparedRenderJob(
                renderJob.getId(),
                filmRoll.getId(),
                userId,
                message
        );
    }

    private void requireReady(FilmRoll filmRoll) {
        if (filmRoll.getStatus() != FilmRollStatus.READY) {
            throw new BusinessException(
                    RenderErrorCode.FILM_ROLL_NOT_READY
            );
        }
    }

    private void requireNoActiveJob(Long filmRollId) {
        if (renderJobRepository.existsByFilmRollIdAndStatusIn(
                filmRollId,
                ACTIVE_JOB_STATUSES
        )) {
            throw new BusinessException(
                    RenderErrorCode.ACTIVE_RENDER_JOB_EXISTS
            );
        }
    }

    private void validatePhotos(
            FilmRoll filmRoll,
            List<Photo> photos
    ) {
        if (photos.isEmpty()) {
            throw new BusinessException(
                    RenderErrorCode.RENDER_PHOTO_NOT_FOUND
            );
        }

        if (photos.size() != filmRoll.getTotalPhotoCount()) {
            throw new BusinessException(
                    RenderErrorCode.RENDER_PHOTO_COUNT_MISMATCH
            );
        }

        validatePhotoSequences(photos);

        boolean hasIncompletePhoto = photos.stream()
                .anyMatch(photo ->
                        photo.getStatus() != PhotoStatus.UPLOADED
                );

        if (hasIncompletePhoto) {
            throw new BusinessException(
                    RenderErrorCode.INCOMPLETE_PHOTO_UPLOAD
            );
        }
    }

    private void validatePhotoSequences(List<Photo> photos) {
        for (int index = 0; index < photos.size(); index++) {
            int expectedSequence = index + 1;
            Integer actualSequence = photos.get(index).getSequence();

            if (!Integer.valueOf(expectedSequence)
                    .equals(actualSequence)) {
                throw new BusinessException(
                        RenderErrorCode.INVALID_PHOTO_SEQUENCE
                );
            }
        }
    }

    private RenderQueueMessage createMessage(
            RenderJob renderJob,
            FilmRoll filmRoll,
            List<Photo> photos,
            LocalDateTime requestedAt
    ) {
        List<RenderQueueMessage.PhotoItem> photoItems =
                photos.stream()
                        .map(photo ->
                                new RenderQueueMessage.PhotoItem(
                                        photo.getId(),
                                        photo.getSequence(),
                                        photo.getOriginalObjectKey(),
                                        photo.getTakenAt()
                                )
                        )
                        .toList();

        return new RenderQueueMessage(
                RenderQueueMessage.CURRENT_SCHEMA_VERSION,
                renderJob.getId(),
                filmRoll.getId(),
                filmRoll.getUser().getId(),
                filmRoll.getRegion().getId(),
                awsProperties.getS3().getBucket(),
                filmRoll.getFilterId(),
                filmRoll.getFilterStrength(),
                filmRoll.getFilterVersion(),
                filmRoll.getTotalPhotoCount(),
                requestedAt,
                photoItems
        );
    }
}
