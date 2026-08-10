package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollDevelopmentResponse;
import com.chaerok.backend.render.dto.RenderRequestResponse;
import com.chaerok.backend.render.service.RenderRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-queue-url"
)
public class FilmRollDevelopmentService {

    private final FilmRollCommandService filmRollCommandService;
    private final RenderRequestService renderRequestService;

    public FilmRollDevelopmentResponse develop(
            Long userId,
            Long filmRollId
    ) {
        PreparedFilmRollDevelopment preparation =
                filmRollCommandService.prepareDevelopment(
                        userId,
                        filmRollId
                );

        if (!preparation.renderRequestRequired()) {
            return new FilmRollDevelopmentResponse(
                    preparation.filmRollId(),
                    preparation.status(),
                    preparation.totalPhotoCount(),
                    preparation.requestedAt()
            );
        }

        RenderRequestResponse renderResponse =
                renderRequestService.requestRender(
                        userId,
                        filmRollId
                );

        return new FilmRollDevelopmentResponse(
                renderResponse.filmRollId(),
                renderResponse.filmRollStatus(),
                preparation.totalPhotoCount(),
                renderResponse.queuedAt() != null
                        ? renderResponse.queuedAt()
                        : preparation.requestedAt()
        );
    }
}
