package com.chaerok.backend.course.repository;

import com.chaerok.backend.course.entity.Course;
import com.chaerok.backend.course.entity.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByUserIdAndStatus(
            Long userId,
            CourseStatus status
    );

    Optional<Course> findByUserIdAndStatus(
            Long userId,
            CourseStatus status
    );
}