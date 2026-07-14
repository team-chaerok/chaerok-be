package com.chaerok.backend.course.dto;

import com.chaerok.backend.course.entity.Course;
import com.chaerok.backend.course.entity.CoursePlace;

import java.util.List;

public record SelectedCourseResponse(
        Long courseId,
        Long regionId,
        String title,
        String status,
        int placeCount,
        boolean completed,
        List<SelectedCoursePlaceResponse> places
) {

    public static SelectedCourseResponse of(
            Course course,
            List<CoursePlace> coursePlaces
    ) {
        int placeCount = coursePlaces.size();

        return new SelectedCourseResponse(
                course.getId(),
                course.getRegion().getId(),
                course.getTitle(),
                course.getStatus().name(),
                placeCount,
                placeCount == 3,
                coursePlaces.stream()
                        .map(SelectedCoursePlaceResponse::from)
                        .toList()
        );
    }
}