package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.exception.FilmRollDevelopmentWaitException;
import com.chaerok.backend.filmroll.exception.FilmRollExitRequiredException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FilmRollDevelopmentTimingService {

    public void requireAvailable(FilmRoll filmRoll) {
        if (!filmRoll.isExitConfirmed()) {
            throw new FilmRollExitRequiredException();
        }

        if (!filmRoll.isDevelopmentAvailable(LocalDateTime.now())) {
            throw new FilmRollDevelopmentWaitException();
        }
    }
}
