package com.chaerok.backend.dev.service;

import com.chaerok.backend.auth.jwt.JwtTokenProvider;
import com.chaerok.backend.dev.dto.DevTokenResponse;
import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.entity.UserRole;
import com.chaerok.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevTokenServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("기존 로컬 사용자의 Access Token을 발급한다")
    void issueAccessTokenForExistingLocalUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        when(user.getNickname()).thenReturn("채록 로컬 테스트");
        when(user.getRole()).thenReturn(UserRole.USER);

        when(userRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "chaerok-local-dev-user"
        )).thenReturn(Optional.of(user));

        when(jwtTokenProvider.createAccessToken(10L, UserRole.USER))
                .thenReturn("test-access-token");

        DevTokenService service = new DevTokenService(
                userRepository,
                jwtTokenProvider
        );

        DevTokenResponse response = service.issueAccessToken();

        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("test-access-token");
        verify(jwtTokenProvider).createAccessToken(10L, UserRole.USER);
    }
}
