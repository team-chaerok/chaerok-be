package com.chaerok.backend.global.config;

import com.chaerok.backend.auth.security.JwtAuthenticationEntryPoint;
import com.chaerok.backend.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final Environment environment;

    @Value("${monitoring.username}")
    private String monitoringUsername;

    @Value("${monitoring.password}")
    private String monitoringPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager monitoringUserDetailsService(
            PasswordEncoder passwordEncoder
    ) {
        UserDetails monitoringUser = User.withUsername(monitoringUsername)
                .password(passwordEncoder.encode(monitoringPassword))
                .roles("MONITOR")
                .build();

        return new InMemoryUserDetailsManager(monitoringUser);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .securityMatcher("/actuator/**")
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health")
                        .permitAll()
                        .requestMatchers("/actuator/prometheus")
                        .hasRole("MONITOR")
                        .anyRequest()
                        .denyAll()
                );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

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