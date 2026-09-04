package com.chaerok.backend.auth.oauth.service;

import com.chaerok.backend.auth.exception.AuthErrorCode;
import com.chaerok.backend.auth.oauth.verifier.AppleTokenVerifier;
import com.chaerok.backend.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleOAuthRevokeServiceTest {

    private static final String TOKEN_URI =
            "https://apple.test/auth/token";

    private static final String REVOKE_URI =
            "https://apple.test/auth/revoke";

    @Mock
    private AppleTokenVerifier appleTokenVerifier;

    private String validPrivateKey;

    @BeforeEach
    void setUp() throws Exception {
        validPrivateKey = createPrivateKey();
    }

    @Test
    @DisplayName("잘못된 Private Key면 client secret 생성에 실패한다")
    void createClientSecretFailsWithInvalidPrivateKey() {
        AppleOAuthRevokeService service =
                createService(
                        request -> Mono.error(
                                new IllegalStateException(
                                        "HTTP 호출은 발생하면 안 됩니다."
                                )
                        ),
                        "invalid-private-key"
                );

        assertThatThrownBy(service::createClientSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Apple Private Key를 불러오지 못했습니다."
                );
    }

    @Test
    @DisplayName("Apple 사용자 검증 후 Refresh Token을 revoke한다")
    void revokeAppleTokenSuccessfully() {
        AtomicInteger tokenRequestCount =
                new AtomicInteger();

        AtomicInteger revokeRequestCount =
                new AtomicInteger();

        ExchangeFunction exchangeFunction = request -> {
            if (request.url().toString().equals(TOKEN_URI)) {
                tokenRequestCount.incrementAndGet();

                return Mono.just(
                        ClientResponse
                                .create(HttpStatus.OK)
                                .header(
                                        "Content-Type",
                                        MediaType.APPLICATION_JSON_VALUE
                                )
                                .body("""
                                        {
                                          "refresh_token": "apple-refresh-token",
                                          "id_token": "apple-id-token"
                                        }
                                        """)
                                .build()
                );
            }

            if (request.url().toString().equals(REVOKE_URI)) {
                revokeRequestCount.incrementAndGet();

                return Mono.just(
                        ClientResponse
                                .create(HttpStatus.OK)
                                .build()
                );
            }

            return Mono.error(
                    new IllegalStateException(
                            "예상하지 못한 요청입니다."
                    )
            );
        };

        AppleOAuthRevokeService service =
                createService(
                        exchangeFunction,
                        validPrivateKey
                );

        Jwt jwt = Jwt.withTokenValue("apple-id-token")
                .header("alg", "RS256")
                .subject("apple-user-id")
                .issuedAt(Instant.now())
                .expiresAt(
                        Instant.now().plusSeconds(3600)
                )
                .build();

        when(appleTokenVerifier.decodeAndValidate(
                "apple-id-token"
        )).thenReturn(jwt);

        assertThatCode(() ->
                service.revoke(
                        "authorization-code",
                        "apple-user-id"
                )
        ).doesNotThrowAnyException();

        assertThat(tokenRequestCount.get())
                .isEqualTo(1);

        assertThat(revokeRequestCount.get())
                .isEqualTo(1);

        verify(appleTokenVerifier)
                .decodeAndValidate("apple-id-token");
    }

    @Test
    @DisplayName("Apple Token 응답에 Refresh Token이 없으면 예외가 발생한다")
    void revokeFailsWhenRefreshTokenIsMissing() {
        ExchangeFunction exchangeFunction = request ->
                Mono.just(
                        ClientResponse
                                .create(HttpStatus.OK)
                                .header(
                                        "Content-Type",
                                        MediaType.APPLICATION_JSON_VALUE
                                )
                                .body("""
                                        {
                                          "id_token": "apple-id-token"
                                        }
                                        """)
                                .build()
                );

        AppleOAuthRevokeService service =
                createService(
                        exchangeFunction,
                        validPrivateKey
                );

        assertThatThrownBy(() ->
                service.revoke(
                        "authorization-code",
                        "apple-user-id"
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Apple Token을 발급받지 못했습니다."
                );

        verify(
                appleTokenVerifier,
                never()
        ).decodeAndValidate("apple-id-token");
    }

    @Test
    @DisplayName("Apple Token 응답에 ID Token이 없으면 예외가 발생한다")
    void revokeFailsWhenIdTokenIsMissing() {
        ExchangeFunction exchangeFunction = request ->
                Mono.just(
                        ClientResponse
                                .create(HttpStatus.OK)
                                .header(
                                        "Content-Type",
                                        MediaType.APPLICATION_JSON_VALUE
                                )
                                .body("""
                                        {
                                          "refresh_token": "apple-refresh-token"
                                        }
                                        """)
                                .build()
                );

        AppleOAuthRevokeService service =
                createService(
                        exchangeFunction,
                        validPrivateKey
                );

        assertThatThrownBy(() ->
                service.revoke(
                        "authorization-code",
                        "apple-user-id"
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Apple Token을 발급받지 못했습니다."
                );
    }

    @Test
    @DisplayName("Apple ID Token의 사용자와 탈퇴 사용자가 다르면 revoke하지 않는다")
    void revokeRejectsAppleUserMismatch() {
        AtomicInteger revokeRequestCount =
                new AtomicInteger();

        ExchangeFunction exchangeFunction = request -> {
            if (request.url().toString().equals(TOKEN_URI)) {
                return Mono.just(
                        ClientResponse
                                .create(HttpStatus.OK)
                                .header(
                                        "Content-Type",
                                        MediaType.APPLICATION_JSON_VALUE
                                )
                                .body("""
                                        {
                                          "refresh_token": "apple-refresh-token",
                                          "id_token": "apple-id-token"
                                        }
                                        """)
                                .build()
                );
            }

            if (request.url().toString().equals(REVOKE_URI)) {
                revokeRequestCount.incrementAndGet();

                return Mono.just(
                        ClientResponse
                                .create(HttpStatus.OK)
                                .build()
                );
            }

            return Mono.error(
                    new IllegalStateException(
                            "예상하지 못한 요청입니다."
                    )
            );
        };

        AppleOAuthRevokeService service =
                createService(
                        exchangeFunction,
                        validPrivateKey
                );

        Jwt jwt = Jwt.withTokenValue("apple-id-token")
                .header("alg", "RS256")
                .subject("different-user-id")
                .issuedAt(Instant.now())
                .expiresAt(
                        Instant.now().plusSeconds(3600)
                )
                .build();

        when(appleTokenVerifier.decodeAndValidate(
                "apple-id-token"
        )).thenReturn(jwt);

        assertThatThrownBy(() ->
                service.revoke(
                        "authorization-code",
                        "apple-user-id"
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(
                        exception.getErrorCode()
                ).isEqualTo(
                        AuthErrorCode.APPLE_WITHDRAWAL_USER_MISMATCH
                )
        );

        assertThat(revokeRequestCount.get())
                .isZero();
    }

    private AppleOAuthRevokeService createService(
            ExchangeFunction exchangeFunction,
            String privateKey
    ) {
        WebClient webClient =
                WebClient.builder()
                        .exchangeFunction(exchangeFunction)
                        .build();

        return new AppleOAuthRevokeService(
                webClient,
                appleTokenVerifier,
                "com.teamchaerok.chaerok",
                "test-team-id",
                "test-key-id",
                privateKey,
                TOKEN_URI,
                REVOKE_URI
        );
    }

    private String createPrivateKey() throws Exception {
        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("EC");

        generator.initialize(
                new ECGenParameterSpec(
                        "secp256r1"
                )
        );

        KeyPair keyPair =
                generator.generateKeyPair();

        String encoded =
                Base64.getEncoder()
                        .encodeToString(
                                keyPair
                                        .getPrivate()
                                        .getEncoded()
                        );

        return """
                -----BEGIN PRIVATE KEY-----
                %s
                -----END PRIVATE KEY-----
                """.formatted(encoded);
    }
}