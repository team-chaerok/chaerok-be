package com.chaerok.backend.course.repository;

import com.chaerok.backend.course.entity.CoursePlace;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoursePlaceRepository extends JpaRepository<CoursePlace, Long> {

    List<CoursePlace> findByCourseIdOrderBySequenceAsc(Long courseId);

    int countByCourseId(Long courseId);

    boolean existsByCourseIdAndPlaceId(
            Long courseId,
            Long placeId
    );

    boolean existsByCourseIdAndCategoryGroup(
            Long courseId,
            PlaceCategoryGroup categoryGroup
    );
}