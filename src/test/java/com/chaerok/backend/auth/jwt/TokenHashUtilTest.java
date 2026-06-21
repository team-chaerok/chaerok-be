package com.chaerok.backend.auth.jwt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHashUtilTest {

    private final TokenHashUtil tokenHashUtil =
            new TokenHashUtil();

    @Test
    void 같은_토큰은_같은_해시값을_생성한다() {
        // given
        String token = "refresh-token";

        // when
        String firstHash = tokenHashUtil.hash(token);
        String secondHash = tokenHashUtil.hash(token);

        // then
        assertThat(firstHash).isEqualTo(secondHash);
        assertThat(firstHash).hasSize(64);
        assertThat(firstHash).isNotEqualTo(token);
    }
}