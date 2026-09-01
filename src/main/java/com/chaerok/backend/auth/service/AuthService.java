package com.chaerok.backend.auth.service;

import com.chaerok.backend.auth.dto.*;
import com.chaerok.backend.auth.entity.RefreshToken;
import com.chaerok.backend.auth.jwt.JwtTokenProvider;
import com.chaerok.backend.auth.jwt.SignupTokenInfo;
import com.chaerok.backend.auth.oauth.dto.OAuthUserInfo;
import com.chaerok.backend.auth.oauth.verifier.OAuthTokenVerifier;
import com.chaerok.backend.auth.oauth.verifier.OAuthTokenVerifierResolver;
import com.chaerok.backend.global.exception.DuplicateUserException;
import com.chaerok.backend.global.exception.InvalidTokenException;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.service.UserService;
import com.chaerok.backend.auth.constant.TermsVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final OAuthTokenVerifierResolver verifierResolver;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public OAuthLoginResponse login(OAuthLoginRequest request) {
        OAuthTokenVerifier verifier =
                verifierResolver.resolve(request.provider());

        OAuthUserInfo oauthUserInfo =
                verifier.verify(
                        request.idToken(),
                        request.nonce()
                );

        Optional<User> existingUser =
                userService.findByOAuthProvider(
                        oauthUserInfo.provider(),
                        oauthUserInfo.providerUserId()
                );

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            String accessToken =
                    jwtTokenProvider.createAccessToken(
                            user.getId(),
                            user.getRole()
                    );

            String refreshToken =
                    jwtTokenProvider.createRefreshToken(user.getId());

            refreshTokenService.save(
                    user,
                    refreshToken,
                    jwtTokenProvider.getExpiration(refreshToken)
            );

            return OAuthLoginResponse.existingUser(
                    accessToken,
                    refreshToken
            );
        }

        String nickname = oauthUserInfo.nickname() != null
                ? oauthUserInfo.nickname()
                : "채록 사용자";

        String signupToken =
                jwtTokenProvider.createSignupToken(
                        oauthUserInfo.provider(),
                        oauthUserInfo.providerUserId(),
                        nickname,
                        oauthUserInfo.email()
                );

        return OAuthLoginResponse.newUser(signupToken);
    }

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        SignupTokenInfo tokenInfo =
                jwtTokenProvider.getSignupTokenInfo(
                        request.signupToken()
                );

        Optional<User> existingUser =
                userService.findByOAuthProvider(
                        tokenInfo.provider(),
                        tokenInfo.providerUserId()
                );

        if (existingUser.isPresent()) {
            throw new DuplicateUserException(
                    "이미 가입된 사용자입니다."
            );
        }

        User user = userService.createUser(
                tokenInfo.provider(),
                tokenInfo.providerUserId(),
                request.nickname(),
                tokenInfo.email(),
                TermsVersion.SERVICE_TERMS,
                TermsVersion.PRIVACY_POLICY
        );

        String accessToken =
                jwtTokenProvider.createAccessToken(
                        user.getId(),
                        user.getRole()
                );

        String refreshToken =
                jwtTokenProvider.createRefreshToken(
                        user.getId()
                );

        refreshTokenService.save(
                user,
                refreshToken,
                jwtTokenProvider.getExpiration(refreshToken)
        );

        return new TokenResponse(
                accessToken,
                refreshToken
        );
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        String oldRefreshToken = request.refreshToken();

        Long tokenUserId =
                jwtTokenProvider.getRefreshTokenUserId(
                        oldRefreshToken
                );

        RefreshToken savedToken =
                refreshTokenService.findValidToken(
                        oldRefreshToken
                );

        User user = savedToken.getUser();

        if (!user.getId().equals(tokenUserId)) {
            throw new InvalidTokenException(
                    "Refresh Token의 사용자 정보가 일치하지 않습니다."
            );
        }

        // 기존 Refresh Token 폐기
        refreshTokenService.delete(oldRefreshToken);

        String newAccessToken =
                jwtTokenProvider.createAccessToken(
                        user.getId(),
                        user.getRole()
                );

        String newRefreshToken =
                jwtTokenProvider.createRefreshToken(
                        user.getId()
                );

        refreshTokenService.save(
                user,
                newRefreshToken,
                jwtTokenProvider.getExpiration(newRefreshToken)
        );

        return new TokenResponse(
                newAccessToken,
                newRefreshToken
        );
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        Long tokenUserId =
                jwtTokenProvider.getRefreshTokenUserId(
                        refreshToken
                );

        RefreshToken savedToken =
                refreshTokenService.findValidToken(
                        refreshToken
                );

        if (!savedToken.getUser().getId().equals(tokenUserId)) {
            throw new InvalidTokenException(
                    "Refresh Token의 사용자 정보가 일치하지 않습니다."
            );
        }

        refreshTokenService.delete(refreshToken);
    }
}