package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollDevelopmentResponse;
import com.chaerok.backend.render.dto.RenderRequestResponse;
import com.chaerok.backend.render.service.RenderRequestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollDevelopmentServiceTest {

    @Mock
    private FilmRollCommandService filmRollCommandService;

    @Mock
    private RenderRequestService renderRequestService;

    @Test
    @DisplayName("현상 준비 후 기존 Render 요청 흐름을 재사용하고 공개 응답만 반환한다")
    void developRequestsRender() {
        PreparedFilmRollDevelopment preparation =
                new PreparedFilmRollDevelopment(
                        100L,
                        "READY",
                        3,
                        null,
                        true
                );
        LocalDateTime queuedAt =
                LocalDateTime.of(2026, 8, 5, 18, 20);
        RenderRequestResponse internalResponse =
                new RenderRequestResponse(
                        UUID.randomUUID(),
                        100L,
                        "QUEUED",
                        "QUEUED",
                        "internal-sqs-message-id",
                        queuedAt
                );

        when(filmRollCommandService.prepareDevelopment(
                1L,
                100L
        )).thenReturn(preparation);
        when(renderRequestService.requestRender(1L, 100L))
                .thenReturn(internalResponse);

        FilmRollDevelopmentService service =
                new FilmRollDevelopmentService(
                        filmRollCommandService,
                        renderRequestService
                );

        FilmRollDevelopmentResponse response =
                service.develop(1L, 100L);

        assertThat(response.filmRollId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo("QUEUED");
        assertThat(response.totalPhotoCount()).isEqualTo(3);
        assertThat(response.requestedAt()).isEqualTo(queuedAt);
        verify(renderRequestService).requestRender(1L, 100L);
    }

    @Test
    @DisplayName("이미 현상 대기 중이면 새 RenderJob을 요청하지 않는다")
    void developIsIdempotentWhenAlreadyQueued() {
        LocalDateTime requestedAt =
                LocalDateTime.of(2026, 8, 5, 18, 20);
        PreparedFilmRollDevelopment preparation =
                new PreparedFilmRollDevelopment(
                        100L,
                        "QUEUED",
                        3,
                        requestedAt,
                        false
                );

        when(filmRollCommandService.prepareDevelopment(
                1L,
                100L
        )).thenReturn(preparation);

        FilmRollDevelopmentService service =
                new FilmRollDevelopmentService(
                        filmRollCommandService,
                        renderRequestService
                );

        FilmRollDevelopmentResponse response =
                service.develop(1L, 100L);

        assertThat(response.filmRollId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo("QUEUED");
        assertThat(response.totalPhotoCount()).isEqualTo(3);
        assertThat(response.requestedAt()).isEqualTo(requestedAt);
        verify(renderRequestService, never())
                .requestRender(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong()
                );
    }
}
