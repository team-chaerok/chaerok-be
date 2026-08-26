package com.chaerok.backend.notification.device.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.notification.device.dto.PushDeviceRegisterRequest;
import com.chaerok.backend.notification.device.dto.PushDeviceUnregisterRequest;
import com.chaerok.backend.notification.device.service.PushDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Notification",
        description = "푸시 알림 기기 등록 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications/devices")
public class PushDeviceController {

    private final PushDeviceService pushDeviceService;

    @Operation(
            summary = "FCM 기기 등록",
            description = """
                    현재 로그인 사용자의 FCM registration token을 등록합니다.
                    동일 토큰을 다시 등록하면 현재 사용자에게 다시 연결합니다.
                    """
    )
    @PutMapping
    public ResponseEntity<Void> register(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @Valid
            @RequestBody
            PushDeviceRegisterRequest request
    ) {
        pushDeviceService.register(
                authenticatedUser.userId(),
                request.token(),
                request.platform()
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "FCM 기기 등록 해제",
            description = """
                    현재 로그인 사용자와 연결된 FCM registration token을 해제합니다.
                    """
    )
    @PostMapping("/unregister")
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @Valid
            @RequestBody
            PushDeviceUnregisterRequest request
    ) {
        pushDeviceService.unregister(
                authenticatedUser.userId(),
                request.token()
        );
        return ResponseEntity.noContent().build();
    }
}