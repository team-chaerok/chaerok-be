package com.chaerok.backend.notification.sender;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.chaerok.backend.notification.message.NotificationPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "firebase",
        name = "enabled",
        havingValue = "true"
)
public class FirebasePushSender implements PushSender {

    private static final String FIREBASE_APP_NAME =
            "chaerok-fcm";

    private final String projectId;

    private volatile FirebaseMessaging firebaseMessaging;

    public FirebasePushSender(
            @Value("${firebase.project-id:}")
            String projectId
    ) {
        this.projectId = projectId == null
                ? ""
                : projectId.trim();
    }

    @Override
    @SuppressWarnings("deprecation")
    public PushSendResult send(
            String registrationToken,
            NotificationPayload payload
    ) {
        if (registrationToken == null
                || registrationToken.isBlank()) {
            return PushSendResult.invalidToken(
                    "EMPTY_REGISTRATION_TOKEN",
                    "FCM registration token이 비어 있습니다."
            );
        }

        try {
            Message message = Message.builder()
                    .setToken(registrationToken)
                    .putAllData(payload.data())
                    .setNotification(
                            Notification.builder()
                                    .setTitle(payload.title())
                                    .setBody(payload.body())
                                    .build()
                    )
                    .setAndroidConfig(
                            AndroidConfig.builder()
                                    .setCollapseKey(
                                            payload.collapseKey()
                                    )
                                    .setPriority(
                                            AndroidConfig.Priority.HIGH
                                    )
                                    .setNotification(
                                            AndroidNotification.builder()
                                                    .setTitle(
                                                            payload.title()
                                                    )
                                                    .setBody(
                                                            payload.body()
                                                    )
                                                    .setTag(
                                                            payload.notificationTag()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .setApnsConfig(
                            ApnsConfig.builder()
                                    .putHeader(
                                            "apns-push-type",
                                            "alert"
                                    )
                                    .putHeader(
                                            "apns-priority",
                                            "10"
                                    )
                                    .putHeader(
                                            "apns-collapse-id",
                                            payload.collapseKey()
                                    )
                                    .setAps(
                                            Aps.builder()
                                                    .setSound("default")
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            messaging().send(message);
            return PushSendResult.sent();
        } catch (FirebaseMessagingException exception) {
            return classify(exception);
        } catch (IOException exception) {
            log.error(
                    "Firebase ADC 초기화 실패",
                    exception
            );
            return PushSendResult.retryable(
                    "FIREBASE_CREDENTIALS_UNAVAILABLE",
                    exception.getMessage()
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Firebase FCM 전송 준비 중 예외",
                    exception
            );
            return PushSendResult.retryable(
                    "FIREBASE_RUNTIME_ERROR",
                    exception.getMessage()
            );
        }
    }

    private FirebaseMessaging messaging() throws IOException {
        FirebaseMessaging existing = firebaseMessaging;
        if (existing != null) {
            return existing;
        }

        synchronized (this) {
            if (firebaseMessaging != null) {
                return firebaseMessaging;
            }

            FirebaseApp app;
            try {
                app = FirebaseApp.getInstance(FIREBASE_APP_NAME);
            } catch (IllegalStateException ignored) {
                FirebaseOptions.Builder options =
                        FirebaseOptions.builder()
                                .setCredentials(
                                        GoogleCredentials
                                                .getApplicationDefault()
                                );

                if (!projectId.isBlank()) {
                    options.setProjectId(projectId);
                }

                app = FirebaseApp.initializeApp(
                        options.build(),
                        FIREBASE_APP_NAME
                );
            }

            firebaseMessaging =
                    FirebaseMessaging.getInstance(app);
            return firebaseMessaging;
        }
    }

    private PushSendResult classify(
            FirebaseMessagingException exception
    ) {
        MessagingErrorCode messagingCode =
                exception.getMessagingErrorCode();

        String errorCode = errorCode(exception);
        String errorMessage = exception.getMessage();

        if (messagingCode == MessagingErrorCode.UNREGISTERED
                || messagingCode
                == MessagingErrorCode.INVALID_ARGUMENT
                || messagingCode
                == MessagingErrorCode.SENDER_ID_MISMATCH) {
            return PushSendResult.invalidToken(
                    errorCode,
                    errorMessage
            );
        }

        if (messagingCode == MessagingErrorCode.UNAVAILABLE
                || messagingCode == MessagingErrorCode.INTERNAL
                || messagingCode
                == MessagingErrorCode.QUOTA_EXCEEDED) {
            return PushSendResult.retryable(
                    errorCode,
                    errorMessage
            );
        }

        ErrorCode platformCode = exception.getErrorCode();
        if (platformCode == ErrorCode.UNAVAILABLE
                || platformCode == ErrorCode.INTERNAL
                || platformCode == ErrorCode.RESOURCE_EXHAUSTED
                || platformCode == ErrorCode.DEADLINE_EXCEEDED) {
            return PushSendResult.retryable(
                    errorCode,
                    errorMessage
            );
        }

        return PushSendResult.permanent(
                errorCode,
                errorMessage
        );
    }

    private String errorCode(
            FirebaseMessagingException exception
    ) {
        if (exception.getMessagingErrorCode() != null) {
            return exception
                    .getMessagingErrorCode()
                    .name();
        }
        if (exception.getErrorCode() != null) {
            return exception
                    .getErrorCode()
                    .name();
        }
        return "FCM_UNKNOWN";
    }
}
