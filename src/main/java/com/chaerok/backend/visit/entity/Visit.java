package com.chaerok.backend.visit.entity;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "visits",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_visits_film_roll_place",
                        columnNames = {"film_roll_id", "place_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_visits_place_id",
                        columnList = "place_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "film_roll_id", nullable = false)
    private FilmRoll filmRoll;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_group", nullable = false, length = 30)
    private PlaceCategoryGroup categoryGroup;

    @CreationTimestamp
    @Column(name = "visited_at", nullable = false, updatable = false)
    private LocalDateTime visitedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Visit(
            FilmRoll filmRoll,
            Place place
    ) {
        if (filmRoll == null) {
            throw new IllegalArgumentException(
                    "필름 롤은 필수입니다."
            );
        }

        if (place == null) {
            throw new IllegalArgumentException(
                    "장소는 필수입니다."
            );
        }

        PlaceCategoryGroup categoryGroup = place.getCategoryGroup();
        if (categoryGroup == null) {
            throw new IllegalArgumentException(
                    "장소 유형은 필수입니다."
            );
        }

        this.filmRoll = filmRoll;
        this.place = place;
        this.categoryGroup = categoryGroup;
    }

    public static Visit create(
            FilmRoll filmRoll,
            Place place
    ) {
        return new Visit(filmRoll, place);
    }
}
