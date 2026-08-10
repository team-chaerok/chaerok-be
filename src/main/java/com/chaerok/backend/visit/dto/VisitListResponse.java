package com.chaerok.backend.visit.dto;

import com.chaerok.backend.visit.entity.Visit;

import java.time.LocalDateTime;
import java.util.List;

public record VisitListResponse(
        Long filmRollId,
        int visitedCategoryCount,
        int requiredCategoryCount,
        boolean visitRequirementMet,
        List<VisitItemResponse> visits
) {

    public VisitListResponse {
        visits = visits == null
                ? List.of()
                : List.copyOf(visits);
    }

    public static VisitListResponse of(
            Long filmRollId,
            int visitedCategoryCount,
            int requiredCategoryCount,
            boolean visitRequirementMet,
            List<Visit> visits
    ) {
        return new VisitListResponse(
                filmRollId,
                visitedCategoryCount,
                requiredCategoryCount,
                visitRequirementMet,
                visits.stream()
                        .map(VisitItemResponse::from)
                        .toList()
        );
    }

    public record VisitItemResponse(
            Long visitId,
            Long placeId,
            String placeName,
            String categoryGroup,
            LocalDateTime visitedAt
    ) {

        public static VisitItemResponse from(Visit visit) {
            return new VisitItemResponse(
                    visit.getId(),
                    visit.getPlace().getId(),
                    visit.getPlace().getTitle(),
                    visit.getCategoryGroup().name(),
                    visit.getVisitedAt()
            );
        }
    }
}
