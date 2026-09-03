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

        // Review Mode??FilmRoll/DB??1?쒓컙 ?ㅼ?以꾩쓣 諛붽씀吏 ?딅뒗??
        // ?ъ궗??怨꾩젙???쒗빐???꾩긽 ?붿껌 ??1?쒓컙 ?湲?寃?щ쭔 硫댁젣?쒕떎.
        if (filmRoll.getUser().isReviewMode()) {
            return;
        }

        if (!filmRoll.isDevelopmentAvailable(LocalDateTime.now())) {
            throw new BusinessException(
                    FilmRollErrorCode.DEVELOPMENT_WAIT_NOT_FINISHED
            );
        }
    }
}