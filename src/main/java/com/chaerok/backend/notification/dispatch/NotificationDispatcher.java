package com.chaerok.backend.notification.dispatch;

import com.chaerok.backend.notification.outbox.entity.NotificationStatus;
import com.chaerok.backend.notification.outbox.repository.NotificationOutboxRepository;
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
        prefix = "firebase",
        name = "enabled",
        havingValue = "true"
)
public class NotificationDispatcher {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationDispatchWorker dispatchWorker;

    @Value("${firebase.dispatch-batch-size:20}")
    private int configuredBatchSize;

    @Scheduled(
            fixedDelayString =
                    "${firebase.dispatch-delay-ms:5000}"
    )
    public void dispatch() {
        LocalDateTime now = LocalDateTime.now();
        int batchSize = Math.max(
                1,
                Math.min(configuredBatchSize, 100)
        );

        List<Long> outboxIds =
                notificationOutboxRepository.findDueIds(
                        NotificationStatus.PENDING,
                        now,
                        PageRequest.of(0, batchSize)
                );

        for (Long outboxId : outboxIds) {
            try {
                dispatchWorker.dispatchOne(
                        outboxId,
                        LocalDateTime.now()
                );
            } catch (RuntimeException exception) {
                log.error(
                        "알림 Outbox 처리 실패: outboxId={}",
                        outboxId,
                        exception
                );
            }
        }
    }
}