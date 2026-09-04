package com.chaerok.backend.global.config;

import com.chaerok.backend.auth.jwt.JwtTokenProvider;
import com.chaerok.backend.auth.security.JwtAuthenticationEntryPoint;
import com.chaerok.backend.auth.security.JwtAuthenticationFilter;
import com.chaerok.backend.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityTestController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        SecurityTestController.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "monitoring.username=test-monitor",
        "monitoring.password=test-password"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("공개 API는 인증 토큰 없이 접근할 수 있다")
    void publicApiWithoutToken() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("보호 API는 인증 토큰이 없으면 401을 반환한다")
    void protectedApiWithoutToken() throws Exception {
        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message")
                        .value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("정상 Access Token으로 보호 API에 접근할 수 있다")
    void protectedApiWithAccessToken() throws Exception {
        String accessToken = "access-token";

        when(jwtTokenProvider.isAccessToken(accessToken))
                .thenReturn(true);
        when(jwtTokenProvider.getUserId(accessToken))
                .thenReturn(1L);
        when(jwtTokenProvider.getRole(accessToken))
                .thenReturn(UserRole.USER);

        mockMvc.perform(
                        get("/api/test/protected")
                                .header(
                                        "Authorization",
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Refresh Token으로 보호 API에 접근할 수 없다")
    void protectedApiWithRefreshToken() throws Exception {
        String refreshToken = "refresh-token";

        when(jwtTokenProvider.isAccessToken(refreshToken))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/test/protected")
                                .header(
                                        "Authorization",
                                        "Bearer " + refreshToken
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("USER 권한으로 관리자 API에 접근하면 403을 반환한다")
    void adminApiWithUserRole() throws Exception {
        String accessToken = "user-access-token";

        when(jwtTokenProvider.isAccessToken(accessToken))
                .thenReturn(true);
        when(jwtTokenProvider.getUserId(accessToken))
                .thenReturn(1L);
        when(jwtTokenProvider.getRole(accessToken))
                .thenReturn(UserRole.USER);

        mockMvc.perform(
                        get("/api/admin/test")
                                .header(
                                        "Authorization",
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN 권한으로 관리자 API에 접근할 수 있다")
    void adminApiWithAdminRole() throws Exception {
        String accessToken = "admin-access-token";

        when(jwtTokenProvider.isAccessToken(accessToken))
                .thenReturn(true);
        when(jwtTokenProvider.getUserId(accessToken))
                .thenReturn(1L);
        when(jwtTokenProvider.getRole(accessToken))
                .thenReturn(UserRole.ADMIN);

        mockMvc.perform(
                        get("/api/admin/test")
                                .header(
                                        "Authorization",
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("local profile이 아니면 dev API는 인증이 필요하다")
    void devApiRequiresAuthenticationOutsideLocalProfile()
            throws Exception {

        mockMvc.perform(get("/api/dev/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("actuator health는 인증 없이 접근할 수 있다")
    void actuatorHealthWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("actuator prometheus는 인증 없이는 접근할 수 없다")
    void prometheusWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("MONITOR 계정은 actuator prometheus에 접근할 수 있다")
    void prometheusWithMonitorAuthentication() throws Exception {
        mockMvc.perform(
                        get("/actuator/prometheus")
                                .with(httpBasic(
                                        "test-monitor",
                                        "test-password"
                                ))
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("허용되지 않은 actuator API는 접근을 거부한다")
    void unknownActuatorEndpointIsDenied() throws Exception {
        mockMvc.perform(
                        get("/actuator/test")
                                .with(httpBasic(
                                        "test-monitor",
                                        "test-password"
                                ))
                )
                .andExpect(status().isForbidden());
    }
}