package com.chaerok.backend.visit.dto;

import com.chaerok.backend.visit.entity.Visit;

import java.time.LocalDateTime;

public record VisitCreateResponse(
        Long visitId,
        Long filmRollId,
        Long placeId,
        Long photoId,
        String placeName,
        String categoryGroup,
        int visitedCategoryCount,
        int requiredCategoryCount,
        boolean visitRequirementMet,
        LocalDateTime visitedAt
) {

    public static VisitCreateResponse of(
            Visit visit,
            int visitedCategoryCount,
            int requiredCategoryCount,
            boolean visitRequirementMet
    ) {
        return new VisitCreateResponse(
                visit.getId(),
                visit.getFilmRoll().getId(),
                visit.getPlace().getId(),
                visit.getPhoto().getId(),
                visit.getPlace().getTitle(),
                visit.getCategoryGroup().name(),
                visitedCategoryCount,
                requiredCategoryCount,
                visitRequirementMet,
                visit.getVisitedAt()
        );
    }
}