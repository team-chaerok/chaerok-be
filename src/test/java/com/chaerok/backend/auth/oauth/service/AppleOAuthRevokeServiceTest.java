package com.chaerok.backend.auth.oauth.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleOAuthRevokeServiceTest {

    @Test
    void 잘못된_Private_Key면_client_secret_생성에_실패한다() {
        // given
        AppleOAuthRevokeService service = new AppleOAuthRevokeService(
                WebClient.builder(),
                "com.teamchaerok.chaerok",
                "test-team-id",
                "test-key-id",
                "invalid-private-key",
                "https://appleid.apple.com/auth/token",
                "https://appleid.apple.com/auth/revoke"
        );

        // when & then
        assertThatThrownBy(service::createClientSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Apple Private Key를 불러오지 못했습니다.");
    }
}