package com.chaerok.backend.auth.security;

import com.chaerok.backend.auth.jwt.JwtTokenProvider;
import com.chaerok.backend.user.entity.UserRole;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        filter = new JwtAuthenticationFilter(jwtTokenProvider);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 Access Token이면 사용자를 인증한다")
    void authenticatesUserWithValidAccessToken() throws Exception {
        // given
        String accessToken = "valid-access-token";

        request.addHeader(
                "Authorization",
                "Bearer " + accessToken
        );

        when(jwtTokenProvider.isAccessToken(accessToken))
                .thenReturn(true);
        when(jwtTokenProvider.getUserId(accessToken))
                .thenReturn(1L);
        when(jwtTokenProvider.getRole(accessToken))
                .thenReturn(UserRole.USER);

        // when
        filter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        assertThat(authentication)
                .isInstanceOf(
                        UsernamePasswordAuthenticationToken.class
                );
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal())
                .isEqualTo(
                        new AuthenticatedUser(
                                1L,
                                UserRole.USER
                        )
                );
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        InOrder inOrder = inOrder(jwtTokenProvider, filterChain);

        inOrder.verify(jwtTokenProvider)
                .isAccessToken(accessToken);
        inOrder.verify(jwtTokenProvider)
                .getUserId(accessToken);
        inOrder.verify(jwtTokenProvider)
                .getRole(accessToken);
        inOrder.verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    @DisplayName("ADMIN Access Token이면 ROLE_ADMIN 권한으로 인증한다")
    void authenticatesAdminWithAdminAuthority() throws Exception {
        // given
        String accessToken = "admin-access-token";

        request.addHeader(
                "Authorization",
                "Bearer " + accessToken
        );

        when(jwtTokenProvider.isAccessToken(accessToken))
                .thenReturn(true);
        when(jwtTokenProvider.getUserId(accessToken))
                .thenReturn(2L);
        when(jwtTokenProvider.getRole(accessToken))
                .thenReturn(UserRole.ADMIN);

        // when
        filter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal())
                .isEqualTo(
                        new AuthenticatedUser(
                                2L,
                                UserRole.ADMIN
                        )
                );
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증하지 않고 다음 필터를 실행한다")
    void continuesWithoutAuthenticationHeader() throws Exception {
        // when
        filter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        assertThat(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        ).isNull();

        verifyNoInteractions(jwtTokenProvider);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer 형식이 아니면 인증하지 않는다")
    void ignoresAuthorizationHeaderWithoutBearerPrefix()
            throws Exception {
        // given
        request.addHeader(
                "Authorization",
                "Basic encoded-credentials"
        );

        // when
        filter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        assertThat(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        ).isNull();

        verifyNoInteractions(jwtTokenProvider);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Access Token이 아닌 토큰이면 인증하지 않는다")
    void ignoresTokenThatIsNotAccessToken() throws Exception {
        // given
        String refreshToken = "refresh-token";

        request.addHeader(
                "Authorization",
                "Bearer " + refreshToken
        );

        when(jwtTokenProvider.isAccessToken(refreshToken))
                .thenReturn(false);

        // when
        filter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        assertThat(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        ).isNull();

        verify(jwtTokenProvider)
                .isAccessToken(refreshToken);
        verify(jwtTokenProvider, never())
                .getUserId(refreshToken);
        verify(jwtTokenProvider, never())
                .getRole(refreshToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰 검증 중 예외가 발생하면 인증 정보를 제거한다")
    void clearsAuthenticationWhenTokenValidationFails()
            throws Exception {
        // given
        String invalidToken = "invalid-access-token";

        request.addHeader(
                "Authorization",
                "Bearer " + invalidToken
        );

        Authentication previousAuthentication =
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(
                                99L,
                                UserRole.USER
                        ),
                        null
                );

        SecurityContextHolder.getContext()
                .setAuthentication(previousAuthentication);

        when(jwtTokenProvider.isAccessToken(invalidToken))
                .thenThrow(
                        new IllegalArgumentException(
                                "Invalid token"
                        )
                );

        // when
        filter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        assertThat(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        ).isNull();

        verify(jwtTokenProvider)
                .isAccessToken(invalidToken);
        verify(jwtTokenProvider, never())
                .getUserId(invalidToken);
        verify(jwtTokenProvider, never())
                .getRole(invalidToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("사용자 정보 조회 중 예외가 발생하면 인증 정보를 제거한다")
    void clearsAuthenticationWhenUserInformationCannotBeRead()
            throws Exception {
        // given
        String accessToken = "malformed-access-token";

        request.addHeader(
                "Authorization",
                "Bearer " + accessToken
        );

        when(jwtTokenProvider.isAccessToken(accessToken))
                .thenReturn(true);
        when(jwtTokenProvider.getUserId(accessToken))
                .thenThrow(
                        new IllegalArgumentException(
                                "Invalid subject"
                        )
                );

        // when
        filter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // then
        assertThat(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        ).isNull();

        verify(jwtTokenProvider)
                .isAccessToken(accessToken);
        verify(jwtTokenProvider)
                .getUserId(accessToken);
        verify(jwtTokenProvider, never())
                .getRole(accessToken);
        verify(filterChain).doFilter(request, response);
    }
}