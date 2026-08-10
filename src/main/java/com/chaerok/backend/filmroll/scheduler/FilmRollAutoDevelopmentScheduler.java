package com.chaerok.backend.filmroll.scheduler;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.filmroll.service.FilmRollDevelopmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-queue-url"
)
public class FilmRollAutoDevelopmentScheduler {

    private static final List<FilmRollStatus> AUTO_DEVELOP_STATUSES =
            List.of(
                    FilmRollStatus.CAPTURING,
                    FilmRollStatus.READY
            );

    private final FilmRollRepository filmRollRepository;
    private final FilmRollDevelopmentService developmentService;

    @Scheduled(
            fixedDelayString =
                    "${chaerok.film-roll.auto-develop-poll-delay-ms:60000}"
    )
    public void developDueFilmRolls() {
        List<FilmRoll> dueFilmRolls =
                filmRollRepository.findDueForAutoDevelopment(
                        AUTO_DEVELOP_STATUSES,
                        LocalDateTime.now()
                );

        for (FilmRoll filmRoll : dueFilmRolls) {
            try {
                developmentService.develop(
                        filmRoll.getUser().getId(),
                        filmRoll.getId()
                );
            } catch (RuntimeException exception) {
                log.warn(
                        "자동 현상 요청 실패: filmRollId={}, error={}",
                        filmRoll.getId(),
                        exception.getMessage()
                );
            }
        }
    }
}
