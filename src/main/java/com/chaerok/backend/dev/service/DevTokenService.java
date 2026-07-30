package com.chaerok.backend.dev.service;

import com.chaerok.backend.auth.constant.TermsVersion;
import com.chaerok.backend.auth.jwt.JwtTokenProvider;
import com.chaerok.backend.dev.dto.DevTokenResponse;
import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("local")
@RequiredArgsConstructor
@Transactional
public class DevTokenService {

    private static final String LOCAL_PROVIDER_USER_ID = "chaerok-local-dev-user";
    private static final String LOCAL_NICKNAME = "채록 로컬 테스트";
    private static final String LOCAL_EMAIL = "local-dev@chaerok.invalid";

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public DevTokenResponse issueAccessToken() {
        User user = userRepository
                .findByProviderAndProviderUserId(
                        OAuthProvider.GOOGLE,
                        LOCAL_PROVIDER_USER_ID
                )
                .orElseGet(this::createLocalUser);

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getRole()
        );

        return DevTokenResponse.of(user, accessToken);
    }

    private User createLocalUser() {
        User user = User.create(
                OAuthProvider.GOOGLE,
                LOCAL_PROVIDER_USER_ID,
                LOCAL_NICKNAME,
                LOCAL_EMAIL,
                TermsVersion.SERVICE_TERMS,
                TermsVersion.PRIVACY_POLICY
        );

        return userRepository.save(user);
    }
}
