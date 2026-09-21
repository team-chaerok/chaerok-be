package com.chaerok.backend.filmroll.scheduler;

import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.filmroll.service.FilmRollStaleCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "chaerok.film-roll",
        name = "stale-cleanup-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class FilmRollStaleCleanupScheduler {

    private static final int BATCH_SIZE = 100;

    private final FilmRollRepository filmRollRepository;
    private final FilmRollStaleCleanupService cleanupService;

    @Value("${chaerok.film-roll.stale-capturing-hours:168}")
    private long configuredStaleHours;

    @Scheduled(
            fixedDelayString =
                    "${chaerok.film-roll.stale-cleanup-poll-delay-ms:3600000}",
            initialDelayString =
                    "${chaerok.film-roll.stale-cleanup-initial-delay-ms:60000}"
    )
    public void expireStaleCapturingFilmRolls() {
        long staleHours = Math.max(1L, configuredStaleHours);
        LocalDateTime expiredAt = LocalDateTime.now();
        LocalDateTime cutoff = expiredAt.minusHours(staleHours);

        List<Long> filmRollIds =
                filmRollRepository.findStaleCapturingIds(
                        FilmRollStatus.CAPTURING,
                        cutoff,
                        PageRequest.of(0, BATCH_SIZE)
                );

        int expiredCount = 0;

        for (Long filmRollId : filmRollIds) {
            try {
                if (cleanupService.expireIfStale(
                        filmRollId,
                        cutoff,
                        expiredAt
                )) {
                    expiredCount++;
                }
            } catch (RuntimeException exception) {
                log.error(
                        "장기 미활동 필름 롤 자동 만료 실패: filmRollId={}",
                        filmRollId,
                        exception
                );
            }
        }

        if (!filmRollIds.isEmpty()) {
            log.info(
                    "장기 미활동 필름 롤 정리 완료: "
                            + "candidates={}, expired={}, staleHours={}",
                    filmRollIds.size(),
                    expiredCount,
                    staleHours
            );
        }
    }
}
