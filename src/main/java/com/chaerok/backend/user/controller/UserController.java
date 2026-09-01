package com.chaerok.backend.user.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.user.dto.UpdateNicknameRequest;
import com.chaerok.backend.user.dto.UserResponse;
import com.chaerok.backend.user.dto.UserWithdrawalRequest;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.service.UserService;
import com.chaerok.backend.user.service.UserWithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserWithdrawalService userWithdrawalService;

    @Operation(
            summary = "내 정보 조회",
            description = "인증된 사용자의 기본 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(
                authenticatedUser.userId()
        );
        UserResponse response = UserResponse.from(user);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "닉네임 수정",
            description = "인증된 사용자의 닉네임을 수정하고 변경된 사용자 정보를 반환합니다."
    )
    @PatchMapping("/me/nickname")
    public ResponseEntity<UserResponse> updateNickname(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        User user = userService.updateNickname(
                authenticatedUser.userId(),
                request.nickname()
        );
        UserResponse response = UserResponse.from(user);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "인증된 사용자의 계정을 삭제합니다. Apple 사용자는 재인증 authorizationCode가 필요합니다."
    )
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody(required = false) UserWithdrawalRequest request
    ) {
        userWithdrawalService.withdraw(
                authenticatedUser.userId(),
                request != null ? request.authorizationCode() : null
        );

        return ResponseEntity.noContent().build();
    }
}
