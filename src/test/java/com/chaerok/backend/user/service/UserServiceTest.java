package com.chaerok.backend.user.service;

import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.exception.UserErrorCode;
import com.chaerok.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private User user;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository);
    }

    @Test
    @DisplayName("사용자 ID로 사용자를 조회한다")
    void findById() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User result = service.findById(1L);

        assertThat(result).isSameAs(user);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 USER_NOT_FOUND 예외를 반환한다")
    void rejectsMissingUser() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.findById(1L)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND)
        );
    }

    @Test
    @DisplayName("OAuth Provider와 Provider User ID로 사용자를 조회한다")
    void findByOAuthProvider() {
        when(userRepository.findByProviderAndProviderUserId(
                OAuthProvider.KAKAO,
                "provider-user-id"
        )).thenReturn(Optional.of(user));

        Optional<User> result =
                service.findByOAuthProvider(
                        OAuthProvider.KAKAO,
                        "provider-user-id"
                );

        assertThat(result).contains(user);
    }

    @Test
    @DisplayName("OAuth 사용자 정보가 없으면 빈 Optional을 반환한다")
    void returnsEmptyWhenOAuthUserDoesNotExist() {
        when(userRepository.findByProviderAndProviderUserId(
                OAuthProvider.KAKAO,
                "provider-user-id"
        )).thenReturn(Optional.empty());

        Optional<User> result =
                service.findByOAuthProvider(
                        OAuthProvider.KAKAO,
                        "provider-user-id"
                );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("신규 OAuth 사용자를 생성한다")
    void createUser() {
        when(userRepository.existsByProviderAndProviderUserId(
                OAuthProvider.KAKAO,
                "provider-user-id"
        )).thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        User result = service.createUser(
                OAuthProvider.KAKAO,
                "provider-user-id",
                "채록",
                "chaerok@example.com",
                "terms-v1",
                "privacy-v1"
        );

        assertThat(result).isNotNull();

        verify(userRepository)
                .existsByProviderAndProviderUserId(
                        OAuthProvider.KAKAO,
                        "provider-user-id"
                );
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("이미 가입된 OAuth 사용자는 중복 생성하지 않는다")
    void rejectsAlreadyRegisteredUser() {
        when(userRepository.existsByProviderAndProviderUserId(
                OAuthProvider.KAKAO,
                "provider-user-id"
        )).thenReturn(true);

        assertThatThrownBy(() ->
                service.createUser(
                        OAuthProvider.KAKAO,
                        "provider-user-id",
                        "채록",
                        "chaerok@example.com",
                        "terms-v1",
                        "privacy-v1"
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(UserErrorCode.ALREADY_REGISTERED)
        );

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    @DisplayName("사용자 닉네임을 변경한다")
    void updateNickname() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User result =
                service.updateNickname(1L, "새닉네임");

        assertThat(result).isSameAs(user);
        verify(user).updateNickname("새닉네임");
    }

    @Test
    @DisplayName("사용자를 삭제한다")
    void deleteUser() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        service.deleteUser(1L);

        verify(userRepository).delete(user);
    }
}