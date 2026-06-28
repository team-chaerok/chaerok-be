package com.chaerok.backend.auth.controller;

import com.chaerok.backend.auth.dto.*;
import com.chaerok.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "OAuth 로그인",
            description = "카카오 또는 구글 ID Token을 검증합니다. 기존 회원은 Access Token과 Refresh Token을 발급하고, 신규 회원은 회원가입에 사용할 Signup Token을 반환합니다."
    )
    @PostMapping("/login")
    public ResponseEntity<OAuthLoginResponse> login(
            @Valid @RequestBody OAuthLoginRequest request
    ) {
        OAuthLoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "회원가입",
            description = "Signup Token과 약관 동의 정보를 검증한 뒤 신규 사용자를 생성하고 Access Token과 Refresh Token을 발급합니다."
    )
    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        TokenResponse response = authService.signup(request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "토큰 갱신",
            description = "유효한 Refresh Token을 검증하고 새로운 Access Token과 Refresh Token을 발급합니다."
    )
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        TokenResponse response = authService.refresh(request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "로그아웃",
            description = "전달받은 Refresh Token을 폐기해 로그아웃 처리합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        authService.logout(request);

        return ResponseEntity.noContent().build();
    }
}