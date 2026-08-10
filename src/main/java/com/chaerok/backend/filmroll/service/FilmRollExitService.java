package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollExitResponse;
import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollConflictException;
import com.chaerok.backend.filmroll.exception.FilmRollNotFoundException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
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
                .orElseThrow(FilmRollNotFoundException::new);

        if (filmRoll.isExitConfirmed()) {
            return FilmRollExitResponse.from(filmRoll);
        }

        if (filmRoll.getStatus() != FilmRollStatus.CAPTURING) {
            throw new FilmRollConflictException(
                    "촬영 중인 필름 롤만 지역 이탈을 확정할 수 있습니다."
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
