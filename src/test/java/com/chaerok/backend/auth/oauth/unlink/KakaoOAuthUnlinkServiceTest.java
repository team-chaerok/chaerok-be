package com.chaerok.backend.auth.oauth.unlink;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoOAuthUnlinkServiceTest {

    private MockRestServiceServer mockServer;

    private KakaoOAuthUnlinkService kakaoOAuthUnlinkService;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder =
                RestClient.builder();

        mockServer =
                MockRestServiceServer
                        .bindTo(restClientBuilder)
                        .build();

        kakaoOAuthUnlinkService =
                new KakaoOAuthUnlinkService(
                        restClientBuilder,
                        "admin-key"
                );
    }

    @Test
    @DisplayName("카카오 사용자 연결 해제 API를 호출한다")
    void unlinkCallsKakaoApi() {
        String providerUserId = "123456789";

        MultiValueMap<String, String> expectedBody =
                new LinkedMultiValueMap<>();

        expectedBody.add(
                "target_id_type",
                "user_id"
        );

        expectedBody.add(
                "target_id",
                providerUserId
        );

        mockServer.expect(
                        once(),
                        requestTo(
                                "https://kapi.kakao.com/v1/user/unlink"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        "Authorization",
                        "KakaoAK admin-key"
                ))
                .andExpect(content().contentType(
                        MediaType.APPLICATION_FORM_URLENCODED
                ))
                .andExpect(content().formData(expectedBody))
                .andRespond(withSuccess());

        kakaoOAuthUnlinkService.unlink(providerUserId);

        mockServer.verify();
    }

    @Test
    @DisplayName("카카오 연결 해제 실패 시 예외를 전파하지 않는다")
    void unlinkDoesNotThrowWhenKakaoApiFails() {
        String providerUserId = "123456789";

        mockServer.expect(
                        once(),
                        requestTo(
                                "https://kapi.kakao.com/v1/user/unlink"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(
                                HttpStatus.INTERNAL_SERVER_ERROR
                        )
                );

        assertThatCode(() ->
                kakaoOAuthUnlinkService.unlink(providerUserId)
        ).doesNotThrowAnyException();

        mockServer.verify();
    }
}