package com.chaerok.backend.course.service;

import com.chaerok.backend.course.dto.CoursePlaceSaveRequest;
import com.chaerok.backend.place.external.TourApiPlaceItem;

public record ResolvedCoursePlace(
        CoursePlaceSaveRequest request,
        TourApiPlaceItem tourApiPlace
) {

    public static ResolvedCoursePlace of(
            CoursePlaceSaveRequest request,
            TourApiPlaceItem tourApiPlace
    ) {
        return new ResolvedCoursePlace(
                request,
                tourApiPlace
        );
    }

    public boolean hasTourApiPlace() {
        return tourApiPlace != null;
    }
}