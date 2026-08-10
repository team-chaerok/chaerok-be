package com.chaerok.backend.render.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-result-consumer-enabled",
        havingValue = "true"
)
public class RenderResultMessageParser {

    private final ObjectMapper objectMapper;

    public RenderResultMessage parse(String messageBody) {
        if (messageBody == null || messageBody.isBlank()) {
            throw new InvalidRenderResultMessageException(
                    "렌더링 결과 메시지 본문이 비어 있습니다."
            );
        }

        try {
            RenderResultMessage message = objectMapper.readValue(
                    messageBody,
                    RenderResultMessage.class
            );
            validate(message);
            return message;
        } catch (JsonProcessingException exception) {
            throw new InvalidRenderResultMessageException(
                    "렌더링 결과 메시지 JSON을 읽을 수 없습니다.",
                    exception
            );
        }
    }

    void validate(RenderResultMessage message) {
        if (message.schemaVersion()
                != RenderResultMessage.CURRENT_SCHEMA_VERSION) {
            throw invalid("지원하지 않는 schemaVersion입니다.");
        }

        requireText(message.eventType(), "eventType");
        requireText(message.requestMessageId(), "requestMessageId");
        requireNonNull(message.renderJobId(), "renderJobId");
        requirePositive(message.filmRollId(), "filmRollId");
        requirePositive(message.userId(), "userId");
        requirePositive(message.regionId(), "regionId");
        requireText(message.bucket(), "bucket");
        requireText(message.status(), "status");
        requireNonNull(message.occurredAt(), "occurredAt");

        if (message.attempt() < 1) {
            throw invalid("attempt는 1 이상이어야 합니다.");
        }

        if (message.isCompleted()) {
            validateCompleted(message);
            return;
        }

        if (message.isFailed()) {
            validateFailed(message);
            return;
        }

        throw invalid("지원하지 않는 eventType입니다.");
    }

    private void validateCompleted(RenderResultMessage message) {
        if (!"COMPLETED".equals(message.status())) {
            throw invalid("완료 이벤트의 status는 COMPLETED여야 합니다.");
        }

        if (message.retryable()) {
            throw invalid("완료 이벤트는 retryable일 수 없습니다.");
        }

        if (message.filteredPhotos().isEmpty()) {
            throw invalid("완료 이벤트에는 필터 사진이 필요합니다.");
        }

        Set<Long> photoIds = new HashSet<>();
        Set<Integer> sequences = new HashSet<>();

        String resultPrefix = resultPrefix(message);
        String filteredPrefix = resultPrefix + "filtered/";
        String exportPrefix = resultPrefix + "export/";

        for (RenderResultMessage.FilteredPhotoResult photo
                : message.filteredPhotos()) {
            requirePositive(photo.photoId(), "filteredPhotos.photoId");
            requirePositive(photo.sequence(), "filteredPhotos.sequence");
            requireText(photo.objectKey(), "filteredPhotos.objectKey");
            requireNonNegative(photo.fileSize(), "filteredPhotos.fileSize");

            String expectedPhotoKey = filteredPrefix
                    + "%03d.jpg".formatted(photo.sequence());

            if (!expectedPhotoKey.equals(photo.objectKey())) {
                throw invalid(
                        "필터 사진 S3 객체 키가 결과 경로 규칙과 다릅니다."
                );
            }

            if (!photoIds.add(photo.photoId())) {
                throw invalid("중복된 필터 사진 ID가 있습니다.");
            }

            if (!sequences.add(photo.sequence())) {
                throw invalid("중복된 필터 사진 순서가 있습니다.");
            }
        }

        requireText(message.zipObjectKey(), "zipObjectKey");
        requireNonNegative(message.zipFileSize(), "zipFileSize");
        requireText(message.reelObjectKey(), "reelObjectKey");
        requireNonNegative(message.reelFileSize(), "reelFileSize");
        requireText(message.manifestObjectKey(), "manifestObjectKey");

        if (!message.zipObjectKey().startsWith(exportPrefix)
                || !message.zipObjectKey().endsWith(".zip")) {
            throw invalid("ZIP S3 객체 키가 결과 경로 규칙과 다릅니다.");
        }

        if (!message.reelObjectKey().startsWith(exportPrefix)
                || !message.reelObjectKey().endsWith(".mp4")) {
            throw invalid("릴스 S3 객체 키가 결과 경로 규칙과 다릅니다.");
        }

        if (!(resultPrefix + "manifest.json")
                .equals(message.manifestObjectKey())) {
            throw invalid("manifest S3 객체 키가 결과 경로 규칙과 다릅니다.");
        }

        if (message.errorCode() != null
                || message.errorMessage() != null) {
            throw invalid("완료 이벤트에는 오류 정보가 없어야 합니다.");
        }
    }

    private void validateFailed(RenderResultMessage message) {
        if (!"FAILED".equals(message.status())) {
            throw invalid("실패 이벤트의 status는 FAILED여야 합니다.");
        }

        if (message.retryable()) {
            throw invalid("결과 큐의 실패 이벤트는 최종 실패여야 합니다.");
        }

        if (!message.filteredPhotos().isEmpty()
                || message.zipObjectKey() != null
                || message.zipFileSize() != null
                || message.reelObjectKey() != null
                || message.reelFileSize() != null
                || message.manifestObjectKey() != null) {
            throw invalid("실패 이벤트에는 완료 결과 경로가 없어야 합니다.");
        }

        requireText(message.errorCode(), "errorCode");
        requireText(message.errorMessage(), "errorMessage");
    }

    private static String resultPrefix(
            RenderResultMessage message
    ) {
        return "users/"
                + message.userId()
                + "/rolls/"
                + message.filmRollId()
                + "/render-jobs/"
                + message.renderJobId()
                + "/";
    }

    private static InvalidRenderResultMessageException invalid(
            String message
    ) {
        return new InvalidRenderResultMessageException(message);
    }

    private static void requireNonNull(
            Object value,
            String fieldName
    ) {
        if (value == null) {
            throw invalid(fieldName + "은(는) 필수입니다.");
        }
    }

    private static void requirePositive(
            Number value,
            String fieldName
    ) {
        if (value == null || value.longValue() < 1L) {
            throw invalid(fieldName + "은(는) 1 이상이어야 합니다.");
        }
    }

    private static void requireNonNegative(
            Long value,
            String fieldName
    ) {
        if (value == null || value < 0L) {
            throw invalid(fieldName + "은(는) 0 이상이어야 합니다.");
        }
    }

    private static void requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw invalid(fieldName + "은(는) 필수입니다.");
        }
    }
}
