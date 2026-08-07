package com.chaerok.backend.visit.repository;

import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.visit.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    boolean existsByFilmRollIdAndPlaceId(
            Long filmRollId,
            Long placeId
    );

    @Query("""
            select distinct visit.categoryGroup
            from Visit visit
            where visit.filmRoll.id = :filmRollId
            """)
    List<PlaceCategoryGroup> findDistinctCategoryGroupsByFilmRollId(
            @Param("filmRollId") Long filmRollId
    );

    @Query("""
            select visit
            from Visit visit
            join fetch visit.place
            where visit.filmRoll.id = :filmRollId
            order by visit.visitedAt asc, visit.id asc
            """)
    List<Visit> findAllWithPlaceByFilmRollId(
            @Param("filmRollId") Long filmRollId
    );
}
