package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.photo.repository.PhotoRepository;
import com.chaerok.backend.visit.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmRollStaleCleanupService {

    private final FilmRollRepository filmRollRepository;
    private final PhotoRepository photoRepository;
    private final VisitRepository visitRepository;

    @Transactional
    public boolean expireIfStale(
            Long filmRollId,
            LocalDateTime cutoff,
            LocalDateTime expiredAt
    ) {
        if (filmRollId == null
                || cutoff == null
                || expiredAt == null) {
            throw new IllegalArgumentException(
                    "필름 롤 ID, stale 기준 시각, 만료 시각은 필수입니다."
            );
        }

        FilmRoll filmRoll = filmRollRepository
                .findByIdForUpdate(filmRollId)
                .orElse(null);

        if (filmRoll == null
                || filmRoll.getStatus() != FilmRollStatus.CAPTURING
                || filmRoll.isExitConfirmed()) {
            return false;
        }

        LocalDateTime updatedAt = filmRoll.getUpdatedAt();
        if (updatedAt == null
                || !updatedAt.isBefore(cutoff)) {
            return false;
        }

        if (photoRepository
                .existsByFilmRollIdAndUpdatedAtGreaterThanEqual(
                        filmRollId,
                        cutoff
                )) {
            return false;
        }

        if (visitRepository
                .existsByFilmRollIdAndCreatedAtGreaterThanEqual(
                        filmRollId,
                        cutoff
                )) {
            return false;
        }

        filmRoll.confirmExit(expiredAt);
        filmRoll.expireAfterExit();

        log.info(
                "장기 미활동 CAPTURING 필름 롤 자동 만료: "
                        + "filmRollId={}, userId={}, clientFilmRollId={}, "
                        + "lastUpdatedAt={}, cutoff={}, expiredAt={}",
                filmRoll.getId(),
                filmRoll.getUser().getId(),
                filmRoll.getClientFilmRollId(),
                updatedAt,
                cutoff,
                expiredAt
        );

        return true;
    }
}
