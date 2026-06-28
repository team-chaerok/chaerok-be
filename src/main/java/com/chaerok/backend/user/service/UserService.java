package com.chaerok.backend.user.service;

import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    public Optional<User> findByOAuthProvider(
            OAuthProvider provider,
            String providerUserId
    ) {
        return userRepository.findByProviderAndProviderUserId(
                provider,
                providerUserId
        );
    }

    @Transactional
    public User createUser(
            OAuthProvider provider,
            String providerUserId,
            String nickname,
            String email,
            String termsVersion,
            String privacyVersion
    ) {
        if (userRepository.existsByProviderAndProviderUserId(
                provider,
                providerUserId
        )) {
            throw new IllegalArgumentException("이미 가입된 사용자입니다.");
        }

        User user = User.create(
                provider,
                providerUserId,
                nickname,
                email,
                termsVersion,
                privacyVersion
        );

        return userRepository.save(user);
    }

    @Transactional
    public User updateNickname(Long userId, String nickname) {
        User user = findById(userId);
        user.updateNickname(nickname);

        return user;
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = findById(userId);
        userRepository.delete(user);
    }
}