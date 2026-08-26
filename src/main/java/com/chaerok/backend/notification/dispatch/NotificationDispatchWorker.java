package com.chaerok.backend.notification.dispatch;

import com.chaerok.backend.notification.device.entity.PushDevice;
import com.chaerok.backend.notification.device.repository.PushDeviceRepository;
import com.chaerok.backend.notification.message.NotificationPayload;
import com.chaerok.backend.notification.message.NotificationPayloadFactory;
import com.chaerok.backend.notification.outbox.entity.NotificationOutbox;
import com.chaerok.backend.notification.outbox.repository.NotificationOutboxRepository;
import com.chaerok.backend.notification.sender.PushSendResult;
import com.chaerok.backend.notification.sender.PushSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "firebase",
        name = "enabled",
        havingValue = "true"
)
public class NotificationDispatchWorker {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final PushDeviceRepository pushDeviceRepository;
    private final NotificationPayloadFactory payloadFactory;
    private final PushSender pushSender;

    @Transactional
    public void dispatchOne(
            Long outboxId,
            LocalDateTime now
    ) {
        NotificationOutbox outbox =
                notificationOutboxRepository
                        .findById(outboxId)
                        .orElse(null);

        if (outbox == null || !outbox.isPending()) {
            return;
        }

        List<PushDevice> devices =
                pushDeviceRepository
                        .findAllByUserId(outbox.getUserId());

        if (devices.isEmpty()) {
            outbox.markSent(now);
            return;
        }

        NotificationPayload payload =
                payloadFactory.from(outbox);

        int sentCount = 0;
        int permanentFailureCount = 0;
        boolean retryableFailure = false;

        String retryErrorCode = null;
        String retryErrorMessage = null;
        String permanentErrorCode = null;
        String permanentErrorMessage = null;

        for (PushDevice device : devices) {
            PushSendResult result;
            try {
                result = pushSender.send(
                        device.getFcmRegistrationToken(),
                        payload
                );
            } catch (RuntimeException exception) {
                log.error(
                        "푸시 Sender 예외: outboxId={}, deviceId={}",
                        outboxId,
                        device.getId(),
                        exception
                );
                result = PushSendResult.retryable(
                        "UNEXPECTED_SENDER_ERROR",
                        exception.getMessage()
                );
            }

            switch (result.type()) {
                case SENT -> sentCount++;

                case TOKEN_INVALID -> {
                    pushDeviceRepository.delete(device);
                    log.info(
                            "유효하지 않은 FCM 토큰 제거: outboxId={}, deviceId={}, errorCode={}",
                            outboxId,
                            device.getId(),
                            result.errorCode()
                    );
                }

                case RETRYABLE_FAILURE -> {
                    retryableFailure = true;
                    if (retryErrorCode == null) {
                        retryErrorCode = result.errorCode();
                        retryErrorMessage =
                                result.errorMessage();
                    }
                }

                case PERMANENT_FAILURE -> {
                    permanentFailureCount++;
                    if (permanentErrorCode == null) {
                        permanentErrorCode =
                                result.errorCode();
                        permanentErrorMessage =
                                result.errorMessage();
                    }
                }
            }
        }

        if (retryableFailure) {
            outbox.markRetry(
                    now,
                    retryErrorCode,
                    retryErrorMessage
            );
            return;
        }

        if (sentCount > 0 || permanentFailureCount == 0) {
            outbox.markSent(now);
            return;
        }

        outbox.markFailed(
                now,
                permanentErrorCode,
                permanentErrorMessage
        );
    }
}