package com.chaerok.backend.render.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-queue-url"
)
public class RenderJobRecoveryPreparationService {

    private final RenderJobRepository renderJobRepository;
    private final PhotoRepository photoRepository;
    private final AwsProperties awsProperties;

    @Transactional
    public Optional<PreparedRenderJob> prepare(
            UUID renderJobId,
            LocalDateTime createdBefore
    ) {
        RenderJob renderJob = renderJobRepository
                .findByIdForUpdate(renderJobId)
                .orElse(null);

        if (renderJob == null
                || renderJob.getStatus() != RenderJobStatus.CREATED) {
            return Optional.empty();
        }

        LocalDateTime createdAt = renderJob.getCreatedAt();

        if (createdAt == null
                || createdAt.isAfter(createdBefore)) {
            return Optional.empty();
        }

        FilmRoll filmRoll = renderJob.getFilmRoll();

        if (filmRoll.getStatus() != FilmRollStatus.READY) {
            throw new IllegalStateException(
                    "CREATED 렌더링 작업의 필름 롤이 READY 상태가 아닙니다. "
                            + "renderJobId="
                            + renderJobId
                            + ", filmRollStatus="
                            + filmRoll.getStatus()
            );
        }

        List<Photo> photos =
                photoRepository
                        .findAllByFilmRollIdOrderBySequenceAsc(
                                filmRoll.getId()
                        );

        validatePhotos(filmRoll, photos);

        RenderQueueMessage message =
                createMessage(
                        renderJob,
                        filmRoll,
                        photos,
                        createdAt
                );

        return Optional.of(
                new PreparedRenderJob(
                        renderJob.getId(),
                        filmRoll.getId(),
                        filmRoll.getUser().getId(),
                        message
                )
        );
    }

    private void validatePhotos(
            FilmRoll filmRoll,
            List<Photo> photos
    ) {
        if (photos.isEmpty()) {
            throw new IllegalStateException(
                    "CREATED 렌더링 작업에 현상할 사진이 없습니다."
            );
        }

        if (photos.size() != filmRoll.getTotalPhotoCount()) {
            throw new IllegalStateException(
                    "CREATED 렌더링 작업의 사진 수가 일치하지 않습니다."
            );
        }

        for (int index = 0; index < photos.size(); index++) {
            Photo photo = photos.get(index);
            int expectedSequence = index + 1;

            if (!Integer.valueOf(expectedSequence)
                    .equals(photo.getSequence())) {
                throw new IllegalStateException(
                        "CREATED 렌더링 작업의 사진 순서가 올바르지 않습니다."
                );
            }

            if (photo.getStatus() != PhotoStatus.UPLOADED) {
                throw new IllegalStateException(
                        "CREATED 렌더링 작업에 업로드 완료되지 않은 사진이 있습니다."
                );
            }

            if (photo.getOriginalObjectKey() == null
                    || photo.getOriginalObjectKey().isBlank()) {
                throw new IllegalStateException(
                        "CREATED 렌더링 작업의 원본 객체 키가 없습니다."
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