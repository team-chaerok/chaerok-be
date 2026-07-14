package com.chaerok.backend.course.controller;

import com.chaerok.backend.auth.security.AuthenticatedUser;
import com.chaerok.backend.course.dto.CourseAddPlacesRequest;
import com.chaerok.backend.course.dto.CourseCreateRequest;
import com.chaerok.backend.course.dto.CourseRecommendResponse;
import com.chaerok.backend.course.dto.SelectedCourseResponse;
import com.chaerok.backend.course.service.CourseCommandService;
import com.chaerok.backend.course.service.CourseRecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Course", description = "추천 코스 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseRecommendService courseRecommendService;
    private final CourseCommandService courseCommandService;

    @Operation(
            summary = "추천 코스 후보 조회",
            description = """
                    regionId 또는 anchorPlaceId를 기준으로 대표 장소 Anchor와 Kakao Local 음식점·카페 후보를 조합한 추천 코스 후보를 조회한다.
                    사용자 원본 좌표는 전달받지 않는다.
                    """
    )
    @GetMapping("/recommend")
    public ResponseEntity<CourseRecommendResponse> recommendCourses(
            @RequestParam Long regionId,
            @RequestParam(required = false) Long anchorPlaceId
    ) {
        return ResponseEntity.ok(courseRecommendService.recommendCourses(
                regionId,
                anchorPlaceId
        ));
    }

    @Operation(
            summary = "사용자 선택 코스 생성",
            description = """
                    사용자가 선택한 장소 1~3개로 ACTIVE 코스를 생성한다.
                    기존 ACTIVE 코스가 있으면 INACTIVE 처리하고 새 ACTIVE 코스를 생성한다.
                    """
    )
    @PostMapping
    public ResponseEntity<SelectedCourseResponse> createCourse(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        return ResponseEntity.ok(courseCommandService.createCourse(
                authenticatedUser.userId(),
                request
        ));
    }

    @Operation(
            summary = "ACTIVE 코스 장소 추가",
            description = """
                    로그인 사용자의 ACTIVE 코스에 장소를 추가한다.
                    ACTIVE 코스는 최대 3개 장소까지만 저장할 수 있다.
                    """
    )
    @PostMapping("/active/places")
    public ResponseEntity<SelectedCourseResponse> addPlacesToActiveCourse(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CourseAddPlacesRequest request
    ) {
        return ResponseEntity.ok(
                courseCommandService.addPlacesToActiveCourse(
                        authenticatedUser.userId(),
                        request
                )
        );
    }

    @Operation(
            summary = "ACTIVE 코스 조회",
            description = "로그인 사용자의 현재 ACTIVE 코스를 조회한다."
    )
    @GetMapping("/active")
    public ResponseEntity<SelectedCourseResponse> getActiveCourse(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(courseCommandService.getActiveCourse(
                authenticatedUser.userId()
        ));
    }
}