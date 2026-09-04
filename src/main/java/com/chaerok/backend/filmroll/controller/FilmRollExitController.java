package com.chaerok.backend.filmroll.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.filmroll.dto.FilmRollExitResponse;
import com.chaerok.backend.filmroll.service.FilmRollExitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Film Roll",
        description = "필름 롤 지역 이탈 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/film-rolls")
public class FilmRollExitController {

    private final FilmRollExitService filmRollExitService;

    @Operation(
            summary = "필름 롤 지역 이탈 확정",
            description = """
                    프론트에서 GPS와 행정구역 판정을 완료하고 사용자가
                    지역 이탈을 확인한 뒤 호출합니다. 백엔드는 좌표를 받지 않고
                    이탈 확정 시각을 저장합니다. Visit 3유형 조건과 사진 1장 이상을
                    모두 충족한 CAPTURING 필름 롤은 현상 가능 시각을 저장합니다.
                    일반 사용자와 심사용 계정 모두 기존과 동일하게 1시간 뒤 현상 가능 시각을 저장하며,
                    심사용 계정은 /develop 요청 시 서버에서 1시간 대기 검사만 면제합니다.
                    두 조건 중 하나라도 부족하면 이탈 사실만 기록하고 EXPIRED로 종료합니다.
                    """
    )
    @PostMapping("/{filmRollId}/exit")
    public ResponseEntity<FilmRollExitResponse> confirmExit(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long filmRollId
    ) {
        return ResponseEntity.ok(
                filmRollExitService.confirmExit(
                        authenticatedUser.userId(),
                        filmRollId
                )
        );
    }
}
