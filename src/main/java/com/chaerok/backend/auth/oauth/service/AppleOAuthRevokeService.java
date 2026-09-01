package com.chaerok.backend.auth.oauth.service;

import com.chaerok.backend.auth.oauth.dto.AppleTokenResponse;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class AppleOAuthRevokeService {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";

    private final WebClient webClient;
    private final String clientId;
    private final String teamId;
    private final String keyId;
    private final String privateKey;
    private final String tokenUri;
    private final String revokeUri;

    public AppleOAuthRevokeService(
            WebClient.Builder webClientBuilder,
            @Value("${oauth.apple.client-id}") String clientId,
            @Value("${oauth.apple.team-id}") String teamId,
            @Value("${oauth.apple.key-id}") String keyId,
            @Value("${oauth.apple.private-key}") String privateKey,
            @Value("${oauth.apple.token-uri}") String tokenUri,
            @Value("${oauth.apple.revoke-uri}") String revokeUri
    ) {
        this.webClient = webClientBuilder.build();
        this.clientId = clientId;
        this.teamId = teamId;
        this.keyId = keyId;
        this.privateKey = privateKey;
        this.tokenUri = tokenUri;
        this.revokeUri = revokeUri;
    }

    String createClientSecret() {
        Instant now = Instant.now();

        return Jwts.builder()
                .header()
                .keyId(keyId)
                .and()
                .issuer(teamId)
                .subject(clientId)
                .audience()
                .add(APPLE_AUDIENCE)
                .and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(loadPrivateKey(), Jwts.SIG.ES256)
                .compact();
    }

    private PrivateKey loadPrivateKey() {
        try {
            String normalizedKey = privateKey
                    .replace("\\n", "\n")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(normalizedKey);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");

            return keyFactory.generatePrivate(keySpec);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Apple Private Key를 불러오지 못했습니다.",
                    exception
            );
        }
    }

    private AppleTokenResponse exchangeAuthorizationCode(String authorizationCode) {
        return webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("client_id", clientId)
                        .with("client_secret", createClientSecret())
                        .with("code", authorizationCode)
                        .with("grant_type", "authorization_code")
                )
                .retrieve()
                .bodyToMono(AppleTokenResponse.class)
                .block();
    }

    public void revoke(String authorizationCode) {
        AppleTokenResponse tokenResponse =
                exchangeAuthorizationCode(authorizationCode);

        if (tokenResponse == null || tokenResponse.refreshToken() == null) {
            throw new IllegalStateException(
                    "Apple Refresh Token을 발급받지 못했습니다."
            );
        }

        revokeToken(tokenResponse.refreshToken());
    }

    private void revokeToken(String refreshToken) {
        webClient.post()
                .uri(revokeUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("client_id", clientId)
                        .with("client_secret", createClientSecret())
                        .with("token", refreshToken)
                        .with("token_type_hint", "refresh_token")
                )
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}