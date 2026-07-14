package com.chaerok.backend.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CourseAddPlacesRequest(
        @Valid
        @Size(min = 1, max = 3, message = "추가할 장소는 1개 이상 3개 이하로 선택해야 합니다.")
        List<CoursePlaceSaveRequest> places
) {
}