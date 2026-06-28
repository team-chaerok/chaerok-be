package com.chaerok.backend.global.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "서버 상태 확인 API")
@RestController
public class HealthCheckController {

    @Operation(
            summary = "서버 상태 확인",
            description = "채록 백엔드 서버가 정상적으로 실행 중인지 확인합니다."
    )
    @GetMapping("/api/health")
    public String healthCheck() {
        return "채록 백엔드 서버가 정상 실행 중입니다.";
    }
}
