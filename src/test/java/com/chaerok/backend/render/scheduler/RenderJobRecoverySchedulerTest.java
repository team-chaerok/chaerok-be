package com.chaerok.backend.render.scheduler;

import com.chaerok.backend.render.entity.RenderJobStatus;
import com.chaerok.backend.render.repository.RenderJobRepository;
import com.chaerok.backend.render.service.RenderJobRecoveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderJobRecoverySchedulerTest {

    @Mock
    private RenderJobRepository renderJobRepository;

    @Mock
    private RenderJobRecoveryService recoveryService;

    @Test
    @DisplayName("오래된 CREATED 작업을 조회해 복구 서비스에 전달한다")
    void recoversStaleCreatedJobs() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        when(renderJobRepository.findStaleIds(
                eq(RenderJobStatus.CREATED),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(
                List.of(first, second)
        );

        RenderJobRecoveryScheduler scheduler =
                new RenderJobRecoveryScheduler(
                        renderJobRepository,
                        recoveryService
                );

        scheduler.recoverStaleCreatedJobs();

        verify(recoveryService)
                .recover(
                        eq(first),
                        any(LocalDateTime.class)
                );

        verify(recoveryService)
                .recover(
                        eq(second),
                        any(LocalDateTime.class)
                );
    }

    @Test
    @DisplayName("한 작업 복구가 실패해도 다음 CREATED 작업을 계속 처리한다")
    void continuesAfterIndividualFailure() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        when(renderJobRepository.findStaleIds(
                eq(RenderJobStatus.CREATED),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(
                List.of(first, second)
        );

        doThrow(new RuntimeException("first failure"))
                .when(recoveryService)
                .recover(
                        eq(first),
                        any(LocalDateTime.class)
                );

        RenderJobRecoveryScheduler scheduler =
                new RenderJobRecoveryScheduler(
                        renderJobRepository,
                        recoveryService
                );

        scheduler.recoverStaleCreatedJobs();

        verify(recoveryService)
                .recover(
                        eq(second),
                        any(LocalDateTime.class)
                );
    }
}