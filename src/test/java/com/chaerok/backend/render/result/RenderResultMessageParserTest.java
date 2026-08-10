package com.chaerok.backend.render.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RenderResultMessageParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final RenderResultMessageParser parser =
            new RenderResultMessageParser(objectMapper);

    @Test
    @DisplayName("Lambda 완료 결과 JSON을 파싱하고 검증한다")
    void parseCompletedMessage() throws Exception {
        RenderResultMessage expected = completedMessage();
        String body = objectMapper.writeValueAsString(expected);

        RenderResultMessage actual = parser.parse(body);

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("완료 이벤트의 사진 ID가 중복되면 거부한다")
    void rejectDuplicatedPhotoId() {
        UUID renderJobId = UUID.randomUUID();
        String prefix = resultPrefix(renderJobId);

        RenderResultMessage message = new RenderResultMessage(
                1,
                RenderResultMessage.EVENT_COMPLETED,
                "request-1",
                renderJobId,
                2L,
                3L,
                1L,
                "bucket",
                "COMPLETED",
                1,
                false,
                List.of(
                        new RenderResultMessage.FilteredPhotoResult(
                                10L,
                                1,
                                prefix + "filtered/001.jpg",
                                100L
                        ),
                        new RenderResultMessage.FilteredPhotoResult(
                                10L,
                                2,
                                prefix + "filtered/002.jpg",
                                100L
                        )
                ),
                prefix + "export/result.zip",
                100L,
                prefix + "export/result.mp4",
                100L,
                prefix + "manifest.json",
                Instant.parse("2026-08-05T03:42:31Z"),
                null,
                null
        );

        assertThatThrownBy(() -> parser.validate(message))
                .isInstanceOf(InvalidRenderResultMessageException.class)
                .hasMessageContaining("중복된 필터 사진 ID");
    }

    @Test
    @DisplayName("결과 큐의 실패 이벤트가 retryable이면 거부한다")
    void rejectRetryableFailedMessage() {
        RenderResultMessage message = new RenderResultMessage(
                1,
                RenderResultMessage.EVENT_FAILED,
                "request-1",
                UUID.randomUUID(),
                2L,
                3L,
                1L,
                "bucket",
                "FAILED",
                3,
                true,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                Instant.parse("2026-08-05T03:42:31Z"),
                "MEDIA_GENERATION_FAILED",
                "FFmpeg failed"
        );

        assertThatThrownBy(() -> parser.validate(message))
                .isInstanceOf(InvalidRenderResultMessageException.class)
                .hasMessageContaining("최종 실패");
    }

    private RenderResultMessage completedMessage() {
        UUID renderJobId = UUID.randomUUID();
        String prefix = resultPrefix(renderJobId);

        return new RenderResultMessage(
                1,
                RenderResultMessage.EVENT_COMPLETED,
                "request-1",
                renderJobId,
                2L,
                3L,
                1L,
                "bucket",
                "COMPLETED",
                1,
                false,
                List.of(
                        new RenderResultMessage.FilteredPhotoResult(
                                10L,
                                1,
                                prefix + "filtered/001.jpg",
                                100L
                        )
                ),
                prefix + "export/result.zip",
                200L,
                prefix + "export/result.mp4",
                300L,
                prefix + "manifest.json",
                Instant.parse("2026-08-05T03:42:31Z"),
                null,
                null
        );
    }

    private String resultPrefix(UUID renderJobId) {
        return "users/3/rolls/2/render-jobs/"
                + renderJobId
                + "/";
    }
}
