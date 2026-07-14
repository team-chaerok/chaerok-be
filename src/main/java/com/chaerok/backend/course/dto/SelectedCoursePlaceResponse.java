package com.chaerok.backend.course.dto;

import com.chaerok.backend.course.entity.CoursePlace;
import com.chaerok.backend.place.entity.Place;

public record SelectedCoursePlaceResponse(
        Long placeId,
        String source,
        String title,
        String categoryGroup,
        String categoryDetail,
        String address,
        Integer sequence
) {

    public static SelectedCoursePlaceResponse from(CoursePlace coursePlace) {
        Place place = coursePlace.getPlace();

        return new SelectedCoursePlaceResponse(
                place.getId(),
                place.getSource().name(),
                place.getTitle(),
                place.getCategoryGroup().name(),
                place.getCategoryDetail() == null
                        ? null
                        : place.getCategoryDetail().name(),
                place.getAddress(),
                coursePlace.getSequence()
        );
    }
}