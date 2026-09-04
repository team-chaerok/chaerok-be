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

    @Test
    void 토큰을_SHA_256_해시로_변환한다() {
        // given
        String token = "abc";

        // when
        String result = tokenHashUtil.hash(token);

        // then
        assertThat(result).isEqualTo(
                "ba7816bf8f01cfea414140de5dae2223"
                        + "b00361a396177a9cb410ff61f20015ad"
        );
        assertThat(result).matches("[0-9a-f]{64}");
    }

    @Test
    void 서로_다른_토큰은_다른_해시값을_생성한다() {
        // given
        String firstToken = "first-refresh-token";
        String secondToken = "second-refresh-token";

        // when
        String firstHash = tokenHashUtil.hash(firstToken);
        String secondHash = tokenHashUtil.hash(secondToken);

        // then
        assertThat(firstHash).isNotEqualTo(secondHash);
    }
}