package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.exception.FilmRollErrorCode;
import com.chaerok.backend.global.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FilmRollDevelopmentTimingService {

    public void requireAvailable(FilmRoll filmRoll) {
        if (!filmRoll.isExitConfirmed()) {
            throw new BusinessException(
                    FilmRollErrorCode.FILM_ROLL_EXIT_REQUIRED
            );
        }

        if (!filmRoll.isDevelopmentAvailable(LocalDateTime.now())) {
            throw new BusinessException(
                    FilmRollErrorCode.DEVELOPMENT_WAIT_NOT_FINISHED
            );
        }
    }
}
