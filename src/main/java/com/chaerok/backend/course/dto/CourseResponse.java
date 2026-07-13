package com.chaerok.backend.course.dto;

import java.util.List;

public record CourseResponse(
        String title,
        double score,
        List<CoursePlaceResponse> places
) {
}