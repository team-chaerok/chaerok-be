package com.chaerok.backend.auth.jwt;

import com.chaerok.backend.auth.exception.AuthErrorCode;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.UserRole;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class JwtTokenProvider {

    private static final String ISSUER = "chaerok";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final long accessExpiration;
    private final long refreshExpiration;
    private final long signupExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration,
            @Value("${jwt.signup-expiration}") long signupExpiration
    ) {
        SecretKey secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        this.jwtEncoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(secretKey)
        );

        this.jwtDecoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.signupExpiration = signupExpiration;
    }

    public String createAccessToken(Long userId, UserRole role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(accessExpiration);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(userId))
                .claim("type", TokenType.ACCESS.name())
                .claim("role", role.name())
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(refreshExpiration);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(userId))
                .claim("type", TokenType.REFRESH.name())
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public Jwt parseToken(String token) {
        return jwtDecoder.decode(token);
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    public UserRole getRole(String token) {
        String role = parseToken(token).getClaimAsString("role");
        return UserRole.valueOf(role);
    }

    public boolean isAccessToken(String token) {
        return hasTokenType(token, TokenType.ACCESS);
    }

    public boolean isRefreshToken(String token) {
        return hasTokenType(token, TokenType.REFRESH);
    }

    private boolean hasTokenType(String token, TokenType tokenType) {
        String actualType = parseToken(token)
                .getClaimAsString("type");

        return tokenType.name().equals(actualType);
    }

    public String createSignupToken(
            OAuthProvider provider,
            String providerUserId,
            String nickname,
            String email
    ) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(signupExpiration);

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(providerUserId)
                .claim("type", TokenType.SIGNUP.name())
                .claim("provider", provider.name())
                .claim("nickname", nickname);

        if (email != null) {
            claimsBuilder.claim("email", email);
        }

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(
                        header,
                        claimsBuilder.build()
                )
        ).getTokenValue();
    }

    public boolean isSignupToken(String token) {
        return hasTokenType(token, TokenType.SIGNUP);
    }

    public SignupTokenInfo getSignupTokenInfo(String token) {
        try {
            Jwt jwt = parseToken(token);

            String tokenType = jwt.getClaimAsString("type");

            if (!TokenType.SIGNUP.name().equals(tokenType)) {
                throw new BusinessException(
                        AuthErrorCode.INVALID_SIGNUP_TOKEN_TYPE
                );
            }

            OAuthProvider provider = OAuthProvider.valueOf(
                    jwt.getClaimAsString("provider")
            );

            return new SignupTokenInfo(
                    provider,
                    jwt.getSubject(),
                    jwt.getClaimAsString("email")
            );
        } catch (JwtException exception) {
            throw new BusinessException(
                    AuthErrorCode.INVALID_OR_EXPIRED_SIGNUP_TOKEN,
                    "Invalid or expired signup token.",
                    exception
            );
        }
    }

    public Long getRefreshTokenUserId(String token) {
        try {
            Jwt jwt = parseToken(token);

            String tokenType = jwt.getClaimAsString("type");

            if (!TokenType.REFRESH.name().equals(tokenType)) {
                throw new BusinessException(
                        AuthErrorCode.INVALID_REFRESH_TOKEN_TYPE
                );
            }

            return Long.valueOf(jwt.getSubject());
        } catch (JwtException | NumberFormatException exception) {
            throw new BusinessException(
                    AuthErrorCode.INVALID_OR_EXPIRED_REFRESH_TOKEN,
                    "Invalid or expired refresh token.",
                    exception
            );
        }
    }

    public LocalDateTime getExpiration(String token) {
        try {
            Instant expiresAt = parseToken(token).getExpiresAt();

            if (expiresAt == null) {
                throw new BusinessException(
                        AuthErrorCode.TOKEN_EXPIRATION_MISSING
                );
            }

            return LocalDateTime.ofInstant(
                    expiresAt,
                    ZoneId.systemDefault()
            );
        } catch (JwtException exception) {
            throw new BusinessException(
                    AuthErrorCode.INVALID_TOKEN,
                    "Invalid token.",
                    exception
            );
        }
    }
}