package com.chaerok.backend.auth.controller;

import com.chaerok.backend.auth.jwt.JwtTokenProvider;
import com.chaerok.backend.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("로그인 요청에 provider가 없으면 400을 반환한다")
    void loginRejectsMissingProvider() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "kakao-id-token"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("provider"));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("로그인 요청의 ID Token이 공백이면 400을 반환한다")
    void loginRejectsBlankIdToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "KAKAO",
                                  "idToken": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("idToken"));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("회원가입 토큰이 공백이면 400을 반환한다")
    void signupRejectsBlankSignupToken() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "signupToken": " ",
                                  "nickname": "채록",
                                  "termsAgreed": true,
                                  "privacyAgreed": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("signupToken"));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("회원가입 닉네임이 공백이면 400을 반환한다")
    void signupRejectsBlankNickname() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "signupToken": "signup-token",
                                  "nickname": " ",
                                  "termsAgreed": true,
                                  "privacyAgreed": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("nickname"));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("회원가입 닉네임이 30자를 초과하면 400을 반환한다")
    void signupRejectsTooLongNickname() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "signupToken": "signup-token",
                                  "nickname": "1234567890123456789012345678901",
                                  "termsAgreed": true,
                                  "privacyAgreed": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("nickname"));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("서비스 이용약관에 동의하지 않으면 400을 반환한다")
    void signupRejectsTermsNotAgreed() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "signupToken": "signup-token",
                                  "nickname": "채록",
                                  "termsAgreed": false,
                                  "privacyAgreed": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("termsAgreed"));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("개인정보 수집 및 이용에 동의하지 않으면 400을 반환한다")
    void signupRejectsPrivacyNotAgreed() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "signupToken": "signup-token",
                                  "nickname": "채록",
                                  "termsAgreed": true,
                                  "privacyAgreed": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("privacyAgreed"));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Refresh Token이 공백이면 토큰 갱신 요청에 400을 반환한다")
    void refreshRejectsBlankRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("refreshToken"));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Refresh Token이 공백이면 로그아웃 요청에 400을 반환한다")
    void logoutRejectsBlankRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field")
                        .value("refreshToken"));

        verifyNoInteractions(authService);
    }
}