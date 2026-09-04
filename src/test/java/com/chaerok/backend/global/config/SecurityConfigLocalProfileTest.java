package com.chaerok.backend.global.config;

import com.chaerok.backend.auth.jwt.JwtTokenProvider;
import com.chaerok.backend.auth.security.JwtAuthenticationEntryPoint;
import com.chaerok.backend.auth.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityTestController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class
})
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "monitoring.username=test-monitor",
        "monitoring.password=test-password"
})
class SecurityConfigLocalProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("local profile에서는 dev API를 인증 없이 접근할 수 있다")
    void devApiIsPublicInLocalProfile() throws Exception {
        mockMvc.perform(get("/api/dev/test"))
                .andExpect(status().isOk());
    }
}