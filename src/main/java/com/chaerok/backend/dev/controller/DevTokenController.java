package com.chaerok.backend.dev.controller;

import com.chaerok.backend.dev.dto.DevTokenResponse;
import com.chaerok.backend.dev.service.DevTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Local Development", description = "local 프로필 전용 개발 도구")
@RestController
@Profile("local")
@RequiredArgsConstructor
@RequestMapping("/api/dev")
public class DevTokenController {

    private final DevTokenService devTokenService;

    @Operation(
            summary = "로컬 테스트 Access Token 발급",
            description = "local 프로필에서만 테스트 사용자의 Access Token을 발급합니다."
    )
    @PostMapping("/token")
    public ResponseEntity<DevTokenResponse> issueToken() {
        return ResponseEntity.ok(devTokenService.issueAccessToken());
    }
}
