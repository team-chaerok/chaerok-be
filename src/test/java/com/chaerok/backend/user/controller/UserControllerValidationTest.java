package com.chaerok.backend.user.controller;

import com.chaerok.backend.auth.jwt.JwtTokenProvider;
import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.entity.UserRole;
import com.chaerok.backend.user.service.UserService;
import com.chaerok.backend.user.service.UserWithdrawalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserWithdrawalService userWithdrawalService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUpAuthentication() {
        AuthenticatedUser principal =
                new AuthenticatedUser(
                        1L,
                        UserRole.USER
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 사용자의 principal을 이용해 내 정보를 조회한다")
    void getMyInfoWithAuthenticatedPrincipal() throws Exception {
        // given
        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(user.getProvider()).thenReturn(OAuthProvider.KAKAO);
        when(user.getNickname()).thenReturn("채록 사용자");
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getRole()).thenReturn(UserRole.USER);

        when(userService.findById(1L))
                .thenReturn(user);

        // when & then
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.provider").value("KAKAO"))
                .andExpect(jsonPath("$.nickname").value("채록 사용자"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(userService).findById(1L);
        verifyNoInteractions(userWithdrawalService);
    }

    @Test
    @DisplayName("닉네임이 공백이면 400을 반환한다")
    void updateNicknameRejectsBlankNickname() throws Exception {
        mockMvc.perform(patch("/api/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("nickname"));

        verifyNoInteractions(userService);
        verifyNoInteractions(userWithdrawalService);
    }

    @Test
    @DisplayName("닉네임이 30자를 초과하면 400을 반환한다")
    void updateNicknameRejectsTooLongNickname() throws Exception {
        mockMvc.perform(patch("/api/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "1234567890123456789012345678901"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("nickname"));

        verifyNoInteractions(userService);
        verifyNoInteractions(userWithdrawalService);
    }

    @Test
    @DisplayName("회원 탈퇴 요청 본문이 없어도 정상 처리한다")
    void withdrawWithoutRequestBody() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isNoContent());

        verify(userWithdrawalService).withdraw(
                1L,
                null
        );
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("회원 탈퇴 요청의 authorizationCode를 서비스에 전달한다")
    void withdrawWithAuthorizationCode() throws Exception {
        mockMvc.perform(delete("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorizationCode": "apple-authorization-code"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(userWithdrawalService).withdraw(
                1L,
                "apple-authorization-code"
        );
        verifyNoInteractions(userService);
    }
}