package com.chaerok.backend.render.result;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.aws.AwsProperties;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.entity.PhotoStatus;
import com.chaerok.backend.photo.repository.PhotoRepository;
import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.entity.RenderJobStatus;
import com.chaerok.backend.render.repository.RenderJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-result-consumer-enabled",
        havingValue = "true"
)
public class RenderResultProcessor {

    private static final ZoneId APPLICATION_ZONE_ID =
            ZoneId.of("Asia/Seoul");

    private final RenderJobRepository renderJobRepository;
    private final FilmRollRepository filmRollRepository;
    private final PhotoRepository photoRepository;
    private final AwsProperties awsProperties;

    @Transactional
    public RenderResultProcessingOutcome process(
            RenderResultMessage message,
            String resultMessageId
    ) {
        requireText(resultMessageId, "결과 SQS 메시지 ID");

        RenderJob renderJob = renderJobRepository
                .findByIdForUpdate(message.renderJobId())
                .orElseThrow(() -> new RenderResultConflictException(
                        "렌더링 작업을 찾을 수 없습니다. renderJobId="
                                + message.renderJobId()
                ));

        Long actualFilmRollId = renderJob.getFilmRoll().getId();
        if (!message.filmRollId().equals(actualFilmRollId)) {
            throw conflict("메시지의 filmRollId가 렌더링 작업과 다릅니다.");
        }

        FilmRoll filmRoll = filmRollRepository
                .findByIdAndUserIdForUpdate(
                        message.filmRollId(),
                        message.userId()
                )
                .orElseThrow(() -> conflict(
                        "렌더링 결과에 해당하는 필름 롤을 찾을 수 없습니다."
                ));

        validateIdentity(message, filmRoll);

        if (message.isCompleted()) {
            List<Photo> photos = photoRepository
                    .findAllByFilmRollIdOrderBySequenceAscForUpdate(
                            message.filmRollId()
                    );

            return applyCompleted(
                    message,
                    resultMessageId,
                    renderJob,
                    filmRoll,
                    photos
            );
        }

        return applyFailed(
                message,
                resultMessageId,
                renderJob,
                filmRoll
        );
    }

    private RenderResultProcessingOutcome applyCompleted(
            RenderResultMessage message,
            String resultMessageId,
            RenderJob renderJob,
            FilmRoll filmRoll,
            List<Photo> photos
    ) {
        if (renderJob.getStatus() == RenderJobStatus.COMPLETED) {
            validateDuplicateCompleted(message, renderJob, filmRoll, photos);
            return RenderResultProcessingOutcome.DUPLICATE;
        }

        if (renderJob.getStatus() == RenderJobStatus.FAILED
                && hasNewerRenderJob(message)) {
            log.warn(
                    "재시도 후 늦게 도착한 이전 완료 결과 무시: renderJobId={}, resultMessageId={}",
                    message.renderJobId(),
                    resultMessageId
            );
            return RenderResultProcessingOutcome.STALE_IGNORED;
        }

        if (renderJob.getStatus() == RenderJobStatus.FAILED
                || filmRoll.getStatus() == FilmRollStatus.FAILED
                || filmRoll.getStatus() == FilmRollStatus.EXPIRED) {
            throw conflict("실패 또는 만료된 작업에 완료 결과를 적용할 수 없습니다.");
        }

        validateCompletedPhotos(message, filmRoll, photos);

        LocalDateTime occurredAt =
                toApplicationDateTime(message);

        Map<Long, Photo> photoById = new HashMap<>();
        for (Photo photo : photos) {
            photoById.put(photo.getId(), photo);
        }

        for (RenderResultMessage.FilteredPhotoResult output
                : message.filteredPhotos()) {
            Photo photo = photoById.get(output.photoId());
            photo.completeFromResult(output.objectKey(), occurredAt);
        }

        filmRoll.completeFromResult(
                message.zipObjectKey(),
                message.reelObjectKey(),
                occurredAt
        );

        renderJob.completeFromResult(
                message.attempt(),
                message.requestMessageId(),
                resultMessageId,
                message.bucket(),
                message.zipObjectKey(),
                message.zipFileSize(),
                message.reelObjectKey(),
                message.reelFileSize(),
                message.manifestObjectKey(),
                occurredAt
        );

        return RenderResultProcessingOutcome.APPLIED;
    }

    private RenderResultProcessingOutcome applyFailed(
            RenderResultMessage message,
            String resultMessageId,
            RenderJob renderJob,
            FilmRoll filmRoll
    ) {
        boolean renderCompleted =
                renderJob.getStatus() == RenderJobStatus.COMPLETED;
        boolean filmRollCompleted =
                filmRoll.getStatus() == FilmRollStatus.COMPLETED
                        || filmRoll.getStatus() == FilmRollStatus.EXPIRED;

        if (renderCompleted || filmRollCompleted) {
            if (!renderCompleted || !filmRollCompleted) {
                throw conflict(
                        "렌더링 작업과 필름 롤의 완료 상태가 일치하지 않습니다."
                );
            }

            log.warn(
                    "완료된 작업의 늦은 실패 결과 무시: renderJobId={}, resultMessageId={}",
                    message.renderJobId(),
                    resultMessageId
            );
            return RenderResultProcessingOutcome.STALE_IGNORED;
        }

        boolean renderFailed =
                renderJob.getStatus() == RenderJobStatus.FAILED;
        boolean filmRollFailed =
                filmRoll.getStatus() == FilmRollStatus.FAILED;

        if (renderFailed || filmRollFailed) {
            if (renderFailed
                    && !filmRollFailed
                    && hasNewerRenderJob(message)) {
                log.warn(
                        "재시도 후 늦게 도착한 이전 실패 결과 무시: renderJobId={}, resultMessageId={}",
                        message.renderJobId(),
                        resultMessageId
                );
                return RenderResultProcessingOutcome.STALE_IGNORED;
            }

            if (!renderFailed || !filmRollFailed) {
                throw conflict(
                        "렌더링 작업과 필름 롤의 실패 상태가 일치하지 않습니다."
                );
            }
            return RenderResultProcessingOutcome.DUPLICATE;
        }

        LocalDateTime occurredAt =
                toApplicationDateTime(message);

        filmRoll.failFromResult(
                message.errorCode(),
                message.errorMessage()
        );

        renderJob.failFromResult(
                message.attempt(),
                message.requestMessageId(),
                resultMessageId,
                message.bucket(),
                message.errorCode(),
                message.errorMessage(),
                occurredAt
        );

        return RenderResultProcessingOutcome.APPLIED;
    }

    private static LocalDateTime toApplicationDateTime(
            RenderResultMessage message
    ) {
        return LocalDateTime.ofInstant(
                message.occurredAt(),
                APPLICATION_ZONE_ID
        );
    }

    private boolean hasNewerRenderJob(
            RenderResultMessage message
    ) {
        return renderJobRepository
                .findFirstByFilmRollIdOrderByCreatedAtDesc(
                        message.filmRollId()
                )
                .map(latest ->
                        !latest.getId().equals(message.renderJobId())
                )
                .orElse(false);
    }

    private void validateIdentity(
            RenderResultMessage message,
            FilmRoll filmRoll
    ) {
        if (!message.regionId().equals(filmRoll.getRegion().getId())) {
            throw conflict("메시지의 regionId가 필름 롤과 다릅니다.");
        }

        if (!message.bucket().equals(awsProperties.getS3().getBucket())) {
            throw conflict("메시지의 S3 버킷이 서버 설정과 다릅니다.");
        }
    }

    private void validateCompletedPhotos(
            RenderResultMessage message,
            FilmRoll filmRoll,
            List<Photo> photos
    ) {
        if (photos.size() != filmRoll.getTotalPhotoCount()
                || message.filteredPhotos().size() != photos.size()) {
            throw conflict("필터 결과 사진 수가 DB 사진 수와 다릅니다.");
        }

        Map<Long, Photo> photoById = new HashMap<>();
        for (Photo photo : photos) {
            photoById.put(photo.getId(), photo);
        }

        for (RenderResultMessage.FilteredPhotoResult output
                : message.filteredPhotos()) {
            Photo photo = photoById.get(output.photoId());
            if (photo == null) {
                throw conflict("메시지에 DB에 없는 photoId가 포함되어 있습니다.");
            }

            if (!output.sequence().equals(photo.getSequence())) {
                throw conflict("메시지의 사진 순서가 DB와 다릅니다.");
            }

            if (photo.getStatus() == PhotoStatus.COMPLETED
                    || photo.getStatus() == PhotoStatus.EXPIRED
                    || photo.getStatus() == PhotoStatus.UPLOADING) {
                throw conflict("현재 사진 상태에는 완료 결과를 적용할 수 없습니다.");
            }
        }
    }

    private void validateDuplicateCompleted(
            RenderResultMessage message,
            RenderJob renderJob,
            FilmRoll filmRoll,
            List<Photo> photos
    ) {
        if (filmRoll.getStatus() != FilmRollStatus.COMPLETED
                && filmRoll.getStatus() != FilmRollStatus.EXPIRED) {
            throw conflict("렌더링 작업과 필름 롤의 완료 상태가 일치하지 않습니다.");
        }

        if (message.filteredPhotos().size() != photos.size()) {
            throw conflict("중복 완료 메시지의 사진 수가 기존 DB 값과 다릅니다.");
        }

        if (!message.zipObjectKey().equals(filmRoll.getZipObjectKey())
                || !message.reelObjectKey().equals(filmRoll.getReelObjectKey())
                || !message.manifestObjectKey().equals(
                        renderJob.getManifestObjectKey()
                )) {
            throw conflict("중복 완료 메시지의 결과 경로가 기존 DB 값과 다릅니다.");
        }

        Map<Long, Photo> photoById = new HashMap<>();
        for (Photo photo : photos) {
            photoById.put(photo.getId(), photo);
        }

        for (RenderResultMessage.FilteredPhotoResult output
                : message.filteredPhotos()) {
            Photo photo = photoById.get(output.photoId());
            if (photo == null
                    || photo.getStatus() != PhotoStatus.COMPLETED
                    || !output.sequence().equals(photo.getSequence())
                    || !output.objectKey().equals(
                            photo.getFilteredObjectKey()
                    )) {
                throw conflict("중복 완료 메시지의 사진 결과가 기존 DB 값과 다릅니다.");
            }
        }
    }

    private static RenderResultConflictException conflict(
            String message
    ) {
        return new RenderResultConflictException(message);
    }

    private static void requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) 필수입니다."
            );
        }
    }
}
