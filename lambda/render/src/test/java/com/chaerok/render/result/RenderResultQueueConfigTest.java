package com.chaerok.render.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RenderResultQueueConfigTest {

    @Test
    @DisplayName("결과 큐 URL 환경 변수를 읽는다")
    void readsQueueUrlFromEnvironment() {
        String queueUrl =
                "https://sqs.ap-northeast-2.amazonaws.com/123/"
                        + "chaerok-render-result-dev";

        RenderResultQueueConfig config =
                RenderResultQueueConfig.fromEnvironment(
                        Map.of(
                                RenderResultQueueConfig.ENV_QUEUE_URL,
                                queueUrl
                        )
                );

        assertThat(config.queueUrl()).isEqualTo(queueUrl);
    }

    @Test
    @DisplayName("결과 큐 URL 환경 변수가 없으면 시작을 거부한다")
    void rejectsMissingQueueUrl() {
        assertThatThrownBy(() ->
                RenderResultQueueConfig.fromEnvironment(Map.of())
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        RenderResultQueueConfig.ENV_QUEUE_URL
                );
    }

    @Test
    @DisplayName("올바르지 않은 결과 큐 URL을 거부한다")
    void rejectsInvalidQueueUrl() {
        assertThatThrownBy(() ->
                new RenderResultQueueConfig("not-a-url")
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valid SQS queue URL");
    }
}
