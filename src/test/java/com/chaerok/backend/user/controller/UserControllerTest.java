package com.chaerok.backend.user.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.user.dto.ReviewModeResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ReviewModeService reviewModeService;

    @Mock
    private UserWithdrawalService userWithdrawalService;

    private UserController controller;
    private AuthenticatedUser authenticatedUser;

    @BeforeEach
    void setUp() {
        controller = new UserController(
                userService,
                reviewModeService,
                userWithdrawalService
        );
        authenticatedUser = new AuthenticatedUser(1L, UserRole.USER);
    }

    @Test
    @DisplayName("심사용 모드 조회는 인증된 사용자 ID로 조회하고 200 응답을 반환한다")
    void getReviewMode() {
        ReviewModeResponse expected = ReviewModeResponse.disabled();
        when(reviewModeService.getReviewMode(1L))
                .thenReturn(expected);

        ResponseEntity<ReviewModeResponse> response =
                controller.getReviewMode(authenticatedUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(reviewModeService).getReviewMode(1L);
    }
}
