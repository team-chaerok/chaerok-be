package com.chaerok.backend.user.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.user.dto.UpdateNicknameRequest;
import com.chaerok.backend.user.dto.UserResponse;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.service.UserService;
import com.chaerok.backend.user.service.UserWithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserWithdrawalService userWithdrawalService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(
                authenticatedUser.userId()
        );

        return ResponseEntity.ok(
                UserResponse.from(user)
        );
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<UserResponse> updateNickname(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        User user = userService.updateNickname(
                authenticatedUser.userId(),
                request.nickname()
        );

        return ResponseEntity.ok(
                UserResponse.from(user)
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        userWithdrawalService.withdraw(
                authenticatedUser.userId()
        );

        return ResponseEntity.noContent().build();
    }
}