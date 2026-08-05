package com.chaerok.backend.render.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollConflictException;
import com.chaerok.backend.filmroll.exception.FilmRollNotFoundException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.aws.AwsProperties;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.entity.PhotoStatus;
import com.chaerok.backend.photo.repository.PhotoRepository;
import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.entity.RenderJobStatus;
import com.chaerok.backend.render.queue.RenderQueueMessage;
import com.chaerok.backend.render.repository.RenderJobRepository;
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
                .orElseThrow(FilmRollNotFoundException::new);

        requireReady(filmRoll);
        requireNoActiveJob(filmRollId);

        List<Photo> photos =
                photoRepository
                        .findAllByFilmRollIdOrderBySequenceAsc(
                                filmRollId
                        );

        validatePhotos(filmRoll, photos);

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
            throw new FilmRollConflictException(
                    "READY 상태의 필름 롤만 현상을 요청할 수 있습니다."
            );
        }
    }

    private void requireNoActiveJob(Long filmRollId) {
        if (renderJobRepository.existsByFilmRollIdAndStatusIn(
                filmRollId,
                ACTIVE_JOB_STATUSES
        )) {
            throw new FilmRollConflictException(
                    "이미 진행 중인 현상 작업이 있습니다."
            );
        }
    }

    private void validatePhotos(
            FilmRoll filmRoll,
            List<Photo> photos
    ) {
        if (photos.isEmpty()) {
            throw new FilmRollConflictException(
                    "현상할 사진이 없습니다."
            );
        }

        if (photos.size() != filmRoll.getTotalPhotoCount()) {
            throw new FilmRollConflictException(
                    "필름 롤 사진 수와 저장된 사진 수가 일치하지 않습니다."
            );
        }

        validatePhotoSequences(photos);

        boolean hasIncompletePhoto = photos.stream()
                .anyMatch(photo ->
                        photo.getStatus() != PhotoStatus.UPLOADED
                );

        if (hasIncompletePhoto) {
            throw new FilmRollConflictException(
                    "업로드가 완료되지 않은 사진이 있습니다."
            );
        }
    }

    private void validatePhotoSequences(List<Photo> photos) {
        for (int index = 0; index < photos.size(); index++) {
            int expectedSequence = index + 1;
            Integer actualSequence = photos.get(index).getSequence();

            if (!Integer.valueOf(expectedSequence)
                    .equals(actualSequence)) {
                throw new FilmRollConflictException(
                        "사진 순서는 1부터 빠짐없이 연속되어야 합니다."
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
                                        photo.isHasFace(),
                                        photo.getSceneType(),
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
