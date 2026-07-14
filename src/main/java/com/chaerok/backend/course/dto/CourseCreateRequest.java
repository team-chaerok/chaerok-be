package com.chaerok.backend.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CourseCreateRequest(
        @NotNull(message = "지역 ID는 필수입니다.")
        Long regionId,

        @NotBlank(message = "코스 제목은 필수입니다.")
        String title,

        @Valid
        @Size(min = 1, max = 3, message = "코스 장소는 1개 이상 3개 이하로 선택해야 합니다.")
        List<CoursePlaceSaveRequest> places
) {
}