package com.chaerok.backend.course.entity;

import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryDetail;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "course_places")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoursePlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(nullable = false)
    private Integer sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_group", nullable = false, length = 30)
    private PlaceCategoryGroup categoryGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_detail", length = 30)
    private PlaceCategoryDetail categoryDetail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static CoursePlace create(
            Course course,
            Place place,
            Integer sequence
    ) {
        CoursePlace coursePlace = new CoursePlace();
        coursePlace.course = course;
        coursePlace.place = place;
        coursePlace.sequence = sequence;
        coursePlace.categoryGroup = place.getCategoryGroup();
        coursePlace.categoryDetail = place.getCategoryDetail();
        return coursePlace;
    }
}