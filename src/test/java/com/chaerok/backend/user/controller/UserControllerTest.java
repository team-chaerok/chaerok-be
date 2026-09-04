package com.chaerok.backend.user.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.user.dto.ReviewModeResponse;
import com.chaerok.backend.user.dto.UpdateNicknameRequest;
import com.chaerok.backend.user.dto.UserResponse;
import com.chaerok.backend.user.dto.UserWithdrawalRequest;
import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.entity.UserRole;
import com.chaerok.backend.user.service.ReviewModeService;
import com.chaerok.backend.user.service.UserService;
import com.chaerok.backend.user.service.UserWithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ReviewModeService reviewModeService;

    @Mock
    private UserWithdrawalService userWithdrawalService;

    @Mock
    private User user;

    private UserController controller;
    private AuthenticatedUser authenticatedUser;

    @BeforeEach
    void setUp() {
        controller = new UserController(
                userService,
                reviewModeService,
                userWithdrawalService
        );

        authenticatedUser = new AuthenticatedUser(
                1L,
                UserRole.USER
        );
    }

    @Test
    @DisplayName("인증된 사용자의 정보를 조회하고 200 응답을 반환한다")
    void getMyInfo() {
        // given
        when(userService.findById(1L))
                .thenReturn(user);

        givenUserInformation(
                1L,
                OAuthProvider.KAKAO,
                "채록 사용자",
                "user@example.com",
                UserRole.USER
        );

        // when
        ResponseEntity<UserResponse> response =
                controller.getMyInfo(authenticatedUser);

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1L);
        assertThat(response.getBody().provider())
                .isEqualTo(OAuthProvider.KAKAO);
        assertThat(response.getBody().nickname())
                .isEqualTo("채록 사용자");
        assertThat(response.getBody().email())
                .isEqualTo("user@example.com");
        assertThat(response.getBody().role())
                .isEqualTo(UserRole.USER);

        verify(userService).findById(1L);
        verifyNoMoreInteractions(userService);
        verifyNoMoreInteractions(reviewModeService);
        verifyNoMoreInteractions(userWithdrawalService);
    }

    @Test
    @DisplayName("인증된 사용자의 닉네임을 수정하고 변경된 정보를 반환한다")
    void updateNickname() {
        // given
        UpdateNicknameRequest request =
                new UpdateNicknameRequest("새 닉네임");

        when(userService.updateNickname(
                1L,
                "새 닉네임"
        )).thenReturn(user);

        givenUserInformation(
                1L,
                OAuthProvider.GOOGLE,
                "새 닉네임",
                "user@example.com",
                UserRole.USER
        );

        // when
        ResponseEntity<UserResponse> response =
                controller.updateNickname(
                        authenticatedUser,
                        request
                );

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1L);
        assertThat(response.getBody().provider())
                .isEqualTo(OAuthProvider.GOOGLE);
        assertThat(response.getBody().nickname())
                .isEqualTo("새 닉네임");
        assertThat(response.getBody().email())
                .isEqualTo("user@example.com");
        assertThat(response.getBody().role())
                .isEqualTo(UserRole.USER);

        verify(userService).updateNickname(
                1L,
                "새 닉네임"
        );
        verifyNoMoreInteractions(userService);
        verifyNoMoreInteractions(reviewModeService);
        verifyNoMoreInteractions(userWithdrawalService);
    }

    @Test
    @DisplayName("authorization code와 함께 회원 탈퇴를 요청하고 204 응답을 반환한다")
    void withdrawWithAuthorizationCode() {
        // given
        UserWithdrawalRequest request =
                new UserWithdrawalRequest(
                        "apple-authorization-code"
                );

        // when
        ResponseEntity<Void> response =
                controller.withdraw(
                        authenticatedUser,
                        request
                );

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(userWithdrawalService).withdraw(
                1L,
                "apple-authorization-code"
        );
        verifyNoMoreInteractions(userWithdrawalService);
        verifyNoMoreInteractions(userService);
        verifyNoMoreInteractions(reviewModeService);
    }

    @Test
    @DisplayName("요청 본문 없이 회원 탈퇴를 요청하면 authorization code를 null로 전달한다")
    void withdrawWithoutRequestBody() {
        // when
        ResponseEntity<Void> response =
                controller.withdraw(
                        authenticatedUser,
                        null
                );

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(userWithdrawalService).withdraw(
                1L,
                null
        );
        verifyNoMoreInteractions(userWithdrawalService);
        verifyNoMoreInteractions(userService);
        verifyNoMoreInteractions(reviewModeService);
    }

    @Test
    @DisplayName("심사용 모드 조회는 인증된 사용자 ID로 조회하고 200 응답을 반환한다")
    void getReviewMode() {
        // given
        ReviewModeResponse expected =
                ReviewModeResponse.disabled();

        when(reviewModeService.getReviewMode(1L))
                .thenReturn(expected);

        // when
        ResponseEntity<ReviewModeResponse> response =
                controller.getReviewMode(authenticatedUser);

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isSameAs(expected);

        verify(reviewModeService).getReviewMode(1L);
        verifyNoMoreInteractions(reviewModeService);
        verifyNoMoreInteractions(userService);
        verifyNoMoreInteractions(userWithdrawalService);
    }

    private void givenUserInformation(
            Long id,
            OAuthProvider provider,
            String nickname,
            String email,
            UserRole role
    ) {
        when(user.getId()).thenReturn(id);
        when(user.getProvider()).thenReturn(provider);
        when(user.getNickname()).thenReturn(nickname);
        when(user.getEmail()).thenReturn(email);
        when(user.getRole()).thenReturn(role);
    }
}