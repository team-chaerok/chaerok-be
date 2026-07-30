package com.chaerok.backend.global.config;

import com.chaerok.backend.auth.security.JwtAuthenticationEntryPoint;
import com.chaerok.backend.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final Environment environment;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean localProfile = environment.acceptsProfiles(Profiles.of("local"));

        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(
                        jwtAuthenticationEntryPoint
                ))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/api/health",
                            "/api/auth/**",
                            "/api/places/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            "/error",
                            "/api/filters/**"
                    ).permitAll();

                    auth.requestMatchers(HttpMethod.GET, "/api/courses/recommend")
                            .permitAll();

                    if (localProfile) {
                        auth.requestMatchers("/api/dev/**").permitAll();
                    }

                    auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
