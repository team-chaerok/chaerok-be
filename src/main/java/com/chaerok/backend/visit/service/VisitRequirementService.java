package com.chaerok.backend.visit.service;

import com.chaerok.backend.place.entity.PlaceCategoryGroup;
import com.chaerok.backend.visit.exception.VisitRequirementNotMetException;
import com.chaerok.backend.visit.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitRequirementService {

    private static final Set<PlaceCategoryGroup>
            REQUIRED_CATEGORY_GROUPS = Set.of(
                    PlaceCategoryGroup.TOURISM,
                    PlaceCategoryGroup.FOOD,
                    PlaceCategoryGroup.CAFE_DESSERT
            );

    public static final int REQUIRED_CATEGORY_COUNT =
            REQUIRED_CATEGORY_GROUPS.size();

    private final VisitRepository visitRepository;

    public Progress getProgress(Long filmRollId) {
        List<PlaceCategoryGroup> visitedGroups =
                visitRepository
                        .findDistinctCategoryGroupsByFilmRollId(
                                filmRollId
                        );

        EnumSet<PlaceCategoryGroup> visitedRequiredGroups =
                EnumSet.noneOf(PlaceCategoryGroup.class);

        for (PlaceCategoryGroup group : visitedGroups) {
            if (group != null
                    && REQUIRED_CATEGORY_GROUPS.contains(group)) {
                visitedRequiredGroups.add(group);
            }
        }

        return new Progress(
                visitedRequiredGroups.size(),
                REQUIRED_CATEGORY_COUNT,
                visitedRequiredGroups.containsAll(
                        REQUIRED_CATEGORY_GROUPS
                )
        );
    }

    public boolean isSatisfied(Long filmRollId) {
        return getProgress(filmRollId).satisfied();
    }

    public void requireSatisfied(Long filmRollId) {
        if (!isSatisfied(filmRollId)) {
            throw new VisitRequirementNotMetException();
        }
    }

    public record Progress(
            int visitedCategoryCount,
            int requiredCategoryCount,
            boolean satisfied
    ) {
    }
}
