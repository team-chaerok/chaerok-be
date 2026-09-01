package com.chaerok.backend.visit.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollErrorCode;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.place.entity.Place;
import com.chaerok.backend.place.exception.PlaceErrorCode;
import com.chaerok.backend.place.repository.PlaceRepository;
import com.chaerok.backend.visit.dto.VisitCreateRequest;
import com.chaerok.backend.visit.dto.VisitCreateResponse;
import com.chaerok.backend.visit.entity.Visit;
import com.chaerok.backend.visit.exception.VisitErrorCode;
import com.chaerok.backend.visit.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitCommandService {

    private static final String DUPLICATE_VISIT_CONSTRAINT =
            "uk_visits_film_roll_place";

    private final FilmRollRepository filmRollRepository;
    private final PlaceRepository placeRepository;
    private final VisitRepository visitRepository;
    private final VisitRequirementService visitRequirementService;

    @Transactional
    public VisitCreateResponse createVisit(
            Long userId,
            Long filmRollId,
            VisitCreateRequest request
    ) {
        FilmRoll filmRoll = filmRollRepository
                .findByIdAndUserIdForUpdate(
                        filmRollId,
                        userId
                )
                .orElseThrow(() ->
                        new BusinessException(
                                FilmRollErrorCode.FILM_ROLL_NOT_FOUND
                        )
                );

        requireVisitable(filmRoll);

        Place place = placeRepository
                .findById(request.placeId())
                .orElseThrow(() ->
                        new BusinessException(
                                PlaceErrorCode.PLACE_NOT_FOUND
                        )
                );

        requireSameRegion(filmRoll, place);
        requireNotVisited(filmRollId, place.getId());

        Visit visit = Visit.create(filmRoll, place);
        Visit savedVisit;

        try {
            savedVisit = visitRepository.saveAndFlush(visit);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateVisitConstraint(exception)) {
                throw new BusinessException(
                        VisitErrorCode.VISIT_ALREADY_EXISTS
                );
            }
            throw exception;
        }

        VisitRequirementService.Progress progress =
                visitRequirementService.getProgress(filmRollId);

        return VisitCreateResponse.of(
                savedVisit,
                progress.visitedCategoryCount(),
                progress.requiredCategoryCount(),
                progress.satisfied()
        );
    }

    private void requireVisitable(FilmRoll filmRoll) {
        if (filmRoll.getStatus() != FilmRollStatus.CAPTURING
                || filmRoll.isExitConfirmed()) {
            throw new BusinessException(
                    VisitErrorCode.FILM_ROLL_NOT_VISITABLE
            );
        }
    }

    private void requireSameRegion(
            FilmRoll filmRoll,
            Place place
    ) {
        if (!Objects.equals(
                filmRoll.getRegion().getId(),
                place.getRegion().getId()
        )) {
            throw new BusinessException(
                    VisitErrorCode.PLACE_REGION_MISMATCH
            );
        }
    }

    private void requireNotVisited(
            Long filmRollId,
            Long placeId
    ) {
        if (visitRepository.existsByFilmRollIdAndPlaceId(
                filmRollId,
                placeId
        )) {
            throw new BusinessException(
                    VisitErrorCode.VISIT_ALREADY_EXISTS
            );
        }
    }

    private boolean isDuplicateVisitConstraint(
            DataIntegrityViolationException exception
    ) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && DUPLICATE_VISIT_CONSTRAINT.equals(
                    violation.getConstraintName()
            )) {
                return true;
            }

            String message = current.getMessage();
            if (message != null
                    && message.contains(DUPLICATE_VISIT_CONSTRAINT)) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }

}
