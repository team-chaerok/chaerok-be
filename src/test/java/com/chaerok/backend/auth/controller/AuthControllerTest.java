package com.chaerok.backend.auth.controller;

import com.chaerok.backend.auth.dto.OAuthLoginRequest;
import com.chaerok.backend.auth.dto.OAuthLoginResponse;
import com.chaerok.backend.auth.dto.RefreshTokenRequest;
import com.chaerok.backend.auth.dto.SignupRequest;
import com.chaerok.backend.auth.dto.TokenResponse;
import com.chaerok.backend.auth.service.AuthService;
import com.chaerok.backend.user.entity.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService);
    }

    @Test
    @DisplayName("기존 OAuth 사용자가 로그인하면 토큰과 200 응답을 반환한다")
    void loginExistingUser() {
        // given
        OAuthLoginRequest request = new OAuthLoginRequest(
                OAuthProvider.KAKAO,
                "kakao-id-token",
                null
        );

        OAuthLoginResponse serviceResponse =
                OAuthLoginResponse.existingUser(
                        "access-token",
                        "refresh-token"
                );

        when(authService.login(request))
                .thenReturn(serviceResponse);

        // when
        ResponseEntity<OAuthLoginResponse> response =
                controller.login(request);

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isSameAs(serviceResponse);
        assertThat(response.getBody().registered()).isTrue();
        assertThat(response.getBody().tokens())
                .isEqualTo(
                        new TokenResponse(
                                "access-token",
                                "refresh-token"
                        )
                );
        assertThat(response.getBody().signupToken()).isNull();

        verify(authService).login(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    @DisplayName("신규 OAuth 사용자가 로그인하면 회원가입 토큰과 200 응답을 반환한다")
    void loginNewUser() {
        // given
        OAuthLoginRequest request = new OAuthLoginRequest(
                OAuthProvider.APPLE,
                "apple-id-token",
                "apple-nonce"
        );

        OAuthLoginResponse serviceResponse =
                OAuthLoginResponse.newUser("signup-token");

        when(authService.login(request))
                .thenReturn(serviceResponse);

        // when
        ResponseEntity<OAuthLoginResponse> response =
                controller.login(request);

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isSameAs(serviceResponse);
        assertThat(response.getBody().registered()).isFalse();
        assertThat(response.getBody().tokens()).isNull();
        assertThat(response.getBody().signupToken())
                .isEqualTo("signup-token");

        verify(authService).login(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    @DisplayName("회원가입을 완료하면 Access Token과 Refresh Token을 반환한다")
    void signup() {
        // given
        SignupRequest request = new SignupRequest(
                "signup-token",
                "채록 사용자",
                true,
                true
        );

        TokenResponse serviceResponse = new TokenResponse(
                "access-token",
                "refresh-token"
        );

        when(authService.signup(request))
                .thenReturn(serviceResponse);

        // when
        ResponseEntity<TokenResponse> response =
                controller.signup(request);

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isSameAs(serviceResponse);
        assertThat(response.getBody().accessToken())
                .isEqualTo("access-token");
        assertThat(response.getBody().refreshToken())
                .isEqualTo("refresh-token");

        verify(authService).signup(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    @DisplayName("Refresh Token을 갱신하면 새로운 토큰과 200 응답을 반환한다")
    void refresh() {
        // given
        RefreshTokenRequest request =
                new RefreshTokenRequest("old-refresh-token");

        TokenResponse serviceResponse = new TokenResponse(
                "new-access-token",
                "new-refresh-token"
        );

        when(authService.refresh(request))
                .thenReturn(serviceResponse);

        // when
        ResponseEntity<TokenResponse> response =
                controller.refresh(request);

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isSameAs(serviceResponse);
        assertThat(response.getBody().accessToken())
                .isEqualTo("new-access-token");
        assertThat(response.getBody().refreshToken())
                .isEqualTo("new-refresh-token");

        verify(authService).refresh(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    @DisplayName("로그아웃하면 Refresh Token을 폐기하고 204 응답을 반환한다")
    void logout() {
        // given
        RefreshTokenRequest request =
                new RefreshTokenRequest("refresh-token");

        // when
        ResponseEntity<Void> response =
                controller.logout(request);

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(authService).logout(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    @DisplayName("로그인 요청은 서비스에 위임한 뒤 응답을 반환한다")
    void delegatesLoginBeforeReturningResponse() {
        // given
        OAuthLoginRequest request = new OAuthLoginRequest(
                OAuthProvider.GOOGLE,
                "google-id-token",
                null
        );

        OAuthLoginResponse serviceResponse =
                OAuthLoginResponse.newUser("signup-token");

        when(authService.login(request))
                .thenReturn(serviceResponse);

        // when
        ResponseEntity<OAuthLoginResponse> response =
                controller.login(request);

        // then
        InOrder inOrder = inOrder(authService);

        inOrder.verify(authService).login(request);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isSameAs(serviceResponse);
    }
}