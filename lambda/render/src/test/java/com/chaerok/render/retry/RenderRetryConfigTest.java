package com.chaerok.render.retry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RenderRetryConfigTest {

    @Test
    @DisplayName("최대 수신 횟수 환경 변수가 없으면 요청 큐 설정값 3을 사용한다")
    void usesDefaultWhenEnvironmentIsMissing() {
        RenderRetryConfig config =
                RenderRetryConfig.fromEnvironment(Map.of());

        assertThat(config.maxReceiveCount()).isEqualTo(3);
        assertThat(config.isFinalAttempt(2)).isFalse();
        assertThat(config.isFinalAttempt(3)).isTrue();
    }

    @Test
    @DisplayName("최대 수신 횟수 환경 변수를 읽는다")
    void readsMaxReceiveCountFromEnvironment() {
        RenderRetryConfig config =
                RenderRetryConfig.fromEnvironment(
                        Map.of(
                                RenderRetryConfig
                                        .ENV_MAX_RECEIVE_COUNT,
                                "5"
                        )
                );

        assertThat(config.maxReceiveCount()).isEqualTo(5);
        assertThat(config.isFinalAttempt(4)).isFalse();
        assertThat(config.isFinalAttempt(5)).isTrue();
    }

    @Test
    @DisplayName("올바르지 않은 최대 수신 횟수를 거부한다")
    void rejectsInvalidMaxReceiveCount() {
        assertThatThrownBy(() ->
                RenderRetryConfig.fromEnvironment(
                        Map.of(
                                RenderRetryConfig
                                        .ENV_MAX_RECEIVE_COUNT,
                                "zero"
                        )
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        RenderRetryConfig.ENV_MAX_RECEIVE_COUNT
                );
    }
}
