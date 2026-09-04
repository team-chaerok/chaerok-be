package com.chaerok.backend.notification.dispatch;

import com.chaerok.backend.notification.outbox.entity.NotificationStatus;
import com.chaerok.backend.notification.outbox.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationOutboxRepository notificationOutboxRepository;

    @Mock
    private NotificationDispatchWorker dispatchWorker;

    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new NotificationDispatcher(
                notificationOutboxRepository,
                dispatchWorker
        );

        ReflectionTestUtils.setField(
                dispatcher,
                "configuredBatchSize",
                20
        );
    }

    @Test
    @DisplayName("전송할 PENDING Outbox를 조회하고 순서대로 처리한다")
    void dispatchesDueOutboxesInOrder() {
        // given
        when(notificationOutboxRepository.findDueIds(
                eq(NotificationStatus.PENDING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(
                11L,
                12L,
                13L
        ));

        LocalDateTime beforeDispatch =
                LocalDateTime.now();

        // when
        dispatcher.dispatch();

        LocalDateTime afterDispatch =
                LocalDateTime.now();

        // then
        ArgumentCaptor<LocalDateTime> queryTimeCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(notificationOutboxRepository)
                .findDueIds(
                        eq(NotificationStatus.PENDING),
                        queryTimeCaptor.capture(),
                        pageableCaptor.capture()
                );

        assertThat(queryTimeCaptor.getValue())
                .isBetween(
                        beforeDispatch,
                        afterDispatch
                );

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);

        InOrder inOrder = inOrder(dispatchWorker);

        inOrder.verify(dispatchWorker)
                .dispatchOne(
                        eq(11L),
                        any(LocalDateTime.class)
                );
        inOrder.verify(dispatchWorker)
                .dispatchOne(
                        eq(12L),
                        any(LocalDateTime.class)
                );
        inOrder.verify(dispatchWorker)
                .dispatchOne(
                        eq(13L),
                        any(LocalDateTime.class)
                );
    }

    @Test
    @DisplayName("조회된 Outbox가 없으면 Worker를 실행하지 않는다")
    void doesNotRunWorkerWhenNoOutboxIsDue() {
        // given
        when(notificationOutboxRepository.findDueIds(
                eq(NotificationStatus.PENDING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of());

        // when
        dispatcher.dispatch();

        // then
        verify(notificationOutboxRepository)
                .findDueIds(
                        eq(NotificationStatus.PENDING),
                        any(LocalDateTime.class),
                        any(Pageable.class)
                );

        verifyNoInteractions(dispatchWorker);
    }

    @Test
    @DisplayName("하나의 Outbox 처리에 실패해도 다음 Outbox를 계속 처리한다")
    void continuesAfterWorkerFailure() {
        // given
        when(notificationOutboxRepository.findDueIds(
                eq(NotificationStatus.PENDING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(
                21L,
                22L,
                23L
        ));

        doThrow(
                new IllegalStateException(
                        "Outbox dispatch failed"
                )
        ).when(dispatchWorker)
                .dispatchOne(
                        eq(22L),
                        any(LocalDateTime.class)
                );

        // when
        dispatcher.dispatch();

        // then
        InOrder inOrder = inOrder(dispatchWorker);

        inOrder.verify(dispatchWorker)
                .dispatchOne(
                        eq(21L),
                        any(LocalDateTime.class)
                );
        inOrder.verify(dispatchWorker)
                .dispatchOne(
                        eq(22L),
                        any(LocalDateTime.class)
                );
        inOrder.verify(dispatchWorker)
                .dispatchOne(
                        eq(23L),
                        any(LocalDateTime.class)
                );
    }

    @ParameterizedTest(name = "설정값 {0}은 배치 크기 {1}로 제한된다")
    @CsvSource({
            "-10, 1",
            "0, 1",
            "1, 1",
            "20, 20",
            "100, 100",
            "101, 100",
            "500, 100"
    })
    @DisplayName("설정된 배치 크기를 1 이상 100 이하로 제한한다")
    void clampsConfiguredBatchSize(
            int configuredBatchSize,
            int expectedBatchSize
    ) {
        // given
        ReflectionTestUtils.setField(
                dispatcher,
                "configuredBatchSize",
                configuredBatchSize
        );

        when(notificationOutboxRepository.findDueIds(
                eq(NotificationStatus.PENDING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of());

        // when
        dispatcher.dispatch();

        // then
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(notificationOutboxRepository)
                .findDueIds(
                        eq(NotificationStatus.PENDING),
                        any(LocalDateTime.class),
                        pageableCaptor.capture()
                );

        assertThat(pageableCaptor.getValue().getPageNumber())
                .isZero();
        assertThat(pageableCaptor.getValue().getPageSize())
                .isEqualTo(expectedBatchSize);

        verifyNoInteractions(dispatchWorker);
    }

    @Test
    @DisplayName("각 Outbox 처리 시 현재 시간을 Worker에 전달한다")
    void passesCurrentTimeToWorker() {
        // given
        when(notificationOutboxRepository.findDueIds(
                eq(NotificationStatus.PENDING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(31L));

        LocalDateTime beforeDispatch =
                LocalDateTime.now();

        // when
        dispatcher.dispatch();

        LocalDateTime afterDispatch =
                LocalDateTime.now();

        // then
        ArgumentCaptor<LocalDateTime> dispatchTimeCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        verify(dispatchWorker).dispatchOne(
                eq(31L),
                dispatchTimeCaptor.capture()
        );

        assertThat(dispatchTimeCaptor.getValue())
                .isBetween(
                        beforeDispatch,
                        afterDispatch
                );
    }
}