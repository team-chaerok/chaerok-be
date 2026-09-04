package com.chaerok.backend.auth.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class JwtAuthenticationEntryPointTest {

    private ObjectMapper objectMapper;
    private JwtAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
                )
                .build();

        entryPoint =
                new JwtAuthenticationEntryPoint(objectMapper);
    }

    @Test
    @DisplayName("인증되지 않은 요청이면 401 오류 응답을 반환한다")
    void returnsUnauthorizedResponse() throws Exception {
        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setRequestURI("/api/users/me");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        InsufficientAuthenticationException exception =
                new InsufficientAuthenticationException(
                        "Authentication is required"
                );

        // when
        entryPoint.commence(
                request,
                response,
                exception
        );

        // then
        assertThat(response.getStatus()).isEqualTo(401);

        MediaType responseContentType =
                MediaType.parseMediaType(
                        response.getContentType()
                );

        assertThat(responseContentType.isCompatibleWith(
                MediaType.APPLICATION_JSON
        )).isTrue();
        assertThat(responseContentType.getCharset())
                .isEqualTo(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(response.getCharacterEncoding())
                .isEqualTo("UTF-8");

        JsonNode body = objectMapper.readTree(
                response.getContentAsString()
        );

        assertThat(body.get("code").asText())
                .isEqualTo("UNAUTHORIZED");
        assertThat(body.get("message").asText())
                .isEqualTo("인증이 필요합니다.");
        assertThat(body.get("path").asText())
                .isEqualTo("/api/users/me");

        JsonNode timestamp = body.get("timestamp");

        assertThat(timestamp).isNotNull();
        assertThat(timestamp.isTextual()).isTrue();
        assertThatCode(() ->
                java.time.LocalDateTime.parse(timestamp.asText())
        ).doesNotThrowAnyException();

        assertThat(body.get("errors").isArray()).isTrue();
        assertThat(body.get("errors").isEmpty()).isTrue();
    }

    @Test
    @DisplayName("요청 URI에 쿼리 문자열이 있어도 경로만 오류 응답에 포함한다")
    void returnsRequestUriWithoutQueryString() throws Exception {
        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/api/courses"
                );

        request.setQueryString("page=1&size=10");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        InsufficientAuthenticationException exception =
                new InsufficientAuthenticationException(
                        "Authentication is required"
                );

        // when
        entryPoint.commence(
                request,
                response,
                exception
        );

        // then
        JsonNode body = objectMapper.readTree(
                response.getContentAsString()
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("path").asText())
                .isEqualTo("/api/courses");
    }
}