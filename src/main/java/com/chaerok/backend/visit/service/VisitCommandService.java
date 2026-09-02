package com.chaerok.backend.visit.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollErrorCode;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.entity.PhotoStatus;
import com.chaerok.backend.photo.repository.PhotoRepository;
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

    private static final String DUPLICATE_PHOTO_CONSTRAINT =
            "uk_visits_photo";

    private final FilmRollRepository filmRollRepository;
    private final PlaceRepository placeRepository;
    private final PhotoRepository photoRepository;
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

        Photo photo = photoRepository
                .findByIdAndFilmRollId(
                        request.photoId(),
                        filmRollId
                )
                .orElseThrow(() ->
                        new BusinessException(
                                FilmRollErrorCode.PHOTO_NOT_FOUND
                        )
                );

        requireUploaded(photo);
        requireUnusedPhoto(photo.getId());

        Visit visit = Visit.create(
                filmRoll,
                place,
                photo
        );

        Visit savedVisit;

        try {
            savedVisit = visitRepository.saveAndFlush(visit);
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(
                    exception,
                    DUPLICATE_VISIT_CONSTRAINT
            )) {
                throw new BusinessException(
                        VisitErrorCode.VISIT_ALREADY_EXISTS
                );
            }

            if (hasConstraint(
                    exception,
                    DUPLICATE_PHOTO_CONSTRAINT
            )) {
                throw new BusinessException(
                        VisitErrorCode.VISIT_PHOTO_ALREADY_USED
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

    private void requireUploaded(Photo photo) {
        if (photo.getStatus() != PhotoStatus.UPLOADED) {
            throw new BusinessException(
                    VisitErrorCode.VISIT_PHOTO_NOT_READY
            );
        }
    }

    private void requireUnusedPhoto(Long photoId) {
        if (visitRepository.existsByPhoto_Id(photoId)) {
            throw new BusinessException(
                    VisitErrorCode.VISIT_PHOTO_ALREADY_USED
            );
        }
    }

    private boolean hasConstraint(
            DataIntegrityViolationException exception,
            String constraintName
    ) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && constraintName.equals(
                    violation.getConstraintName()
            )) {
                return true;
            }

            String message = current.getMessage();
            if (message != null
                    && message.contains(constraintName)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}