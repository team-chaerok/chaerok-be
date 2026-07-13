package com.chaerok.backend.course.dto;

import java.util.List;

public record CourseRecommendResponse(
        Long regionId,
        String recommendationType,
        Long anchorPlaceId,
        List<CourseResponse> courses
) {
}