package com.chaerok.backend.visit.service;

import com.chaerok.backend.filmroll.exception.FilmRollNotFoundException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.visit.dto.VisitListResponse;
import com.chaerok.backend.visit.entity.Visit;
import com.chaerok.backend.visit.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitQueryService {

    private final FilmRollRepository filmRollRepository;
    private final VisitRepository visitRepository;
    private final VisitRequirementService visitRequirementService;

    public VisitListResponse getVisits(
            Long userId,
            Long filmRollId
    ) {
        filmRollRepository
                .findByIdAndUserId(filmRollId, userId)
                .orElseThrow(FilmRollNotFoundException::new);

        List<Visit> visits =
                visitRepository
                        .findAllWithPlaceByFilmRollId(filmRollId);

        VisitRequirementService.Progress progress =
                visitRequirementService.getProgress(filmRollId);

        return VisitListResponse.of(
                filmRollId,
                progress.visitedCategoryCount(),
                progress.requiredCategoryCount(),
                progress.satisfied(),
                visits
        );
    }
}
