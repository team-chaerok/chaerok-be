package com.chaerok.backend.auth.oauth.verifier;

import com.chaerok.backend.auth.oauth.dto.OAuthUserInfo;
import com.chaerok.backend.global.exception.InvalidTokenException;
import com.chaerok.backend.user.entity.OAuthProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class AppleTokenVerifier implements OAuthTokenVerifier {

    private final JwtDecoder jwtDecoder;

    public AppleTokenVerifier(
            @Value("${oauth.apple.client-id}") String clientId,
            @Value("${oauth.apple.issuer}") String issuer,
            @Value("${oauth.apple.jwk-set-uri}") String jwkSetUri
    ) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(issuer);

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            if (jwt.getAudience().contains(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "Apple ID Token의 audience가 일치하지 않습니다.",
                    null
            );

            return OAuth2TokenValidatorResult.failure(error);
        };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        audienceValidator
                )
        );

        this.jwtDecoder = decoder;
    }

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.APPLE;
    }

    @Override
    public OAuthUserInfo verify(String idToken) {
        try {
            Jwt jwt = jwtDecoder.decode(idToken);

            return new OAuthUserInfo(
                    OAuthProvider.APPLE,
                    jwt.getSubject(),
                    null,
                    jwt.getClaimAsString("email")
            );
        } catch (JwtException exception) {
            throw new InvalidTokenException(
                    "유효하지 않은 Apple ID Token입니다.",
                    exception
            );
        }
    }
}