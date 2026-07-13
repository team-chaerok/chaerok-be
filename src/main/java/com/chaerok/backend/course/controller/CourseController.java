package com.chaerok.backend.course.controller;

import com.chaerok.backend.course.dto.CourseRecommendResponse;
import com.chaerok.backend.course.service.CourseRecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Course", description = "추천 코스 후보 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseRecommendService courseRecommendService;

    @Operation(
            summary = "추천 코스 후보 조회",
            description = "regionId 또는 anchorPlaceId를 기준으로 대표 장소 Anchor와 Kakao Local 음식점·카페 후보를 조합한 추천 코스 후보를 조회한다. 사용자 원본 좌표는 전달받지 않는다."
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
}