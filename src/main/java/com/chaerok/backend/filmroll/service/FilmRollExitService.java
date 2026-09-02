package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollExitResponse;
import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollErrorCode;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.visit.service.VisitRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FilmRollExitService {

    private final FilmRollRepository filmRollRepository;
    private final VisitRequirementService visitRequirementService;

    @Transactional
    public FilmRollExitResponse confirmExit(
            Long userId,
            Long filmRollId
    ) {
        FilmRoll filmRoll = filmRollRepository
                .findByIdAndUserIdForUpdate(filmRollId, userId)
                .orElseThrow(() ->
                new BusinessException(
                        FilmRollErrorCode.FILM_ROLL_NOT_FOUND
                )
        );

        if (filmRoll.isExitConfirmed()) {
            return FilmRollExitResponse.from(filmRoll);
        }

        if (filmRoll.getStatus() != FilmRollStatus.CAPTURING) {
            throw new BusinessException(
                    FilmRollErrorCode.FILM_ROLL_NOT_CAPTURING_FOR_EXIT
            );
        }

        boolean visitRequirementMet =
                visitRequirementService.isSatisfied(filmRollId);

        filmRoll.confirmExit(LocalDateTime.now());

        if (!visitRequirementMet
                || filmRoll.getTotalPhotoCount() == 0) {
            filmRoll.expireAfterExit();
        }

        return FilmRollExitResponse.from(filmRoll);
    }
}
