package com.chaerok.backend.notification.sender;

import com.chaerok.backend.notification.message.NotificationPayload;
import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebasePushSenderTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    private FirebasePushSender sender;
    private NotificationPayload payload;

    @BeforeEach
    void setUp() {
        sender = new FirebasePushSender("test-project");

        ReflectionTestUtils.setField(
                sender,
                "firebaseMessaging",
                firebaseMessaging
        );

        payload = new NotificationPayload(
                "필름 현상 완료",
                "필름 롤 현상이 완료되었습니다.",
                "film-roll-development",
                "film-roll-1",
                Map.of(
                        "type", "FILM_ROLL_DEVELOPED",
                        "filmRollId", "1"
                )
        );
    }

    @Test
    @DisplayName("FCM 메시지 전송에 성공하면 SENT 결과를 반환한다")
    void returnsSentWhenMessageIsDelivered() throws Exception {
        // given
        when(firebaseMessaging.send(any(Message.class)))
                .thenReturn("firebase-message-id");

        // when
        PushSendResult result = sender.send(
                "registration-token",
                payload
        );

        // then
        assertThat(result.type())
                .isEqualTo(PushSendResultType.SENT);
        assertThat(result.errorCode()).isNull();
        assertThat(result.errorMessage()).isNull();

        ArgumentCaptor<Message> messageCaptor =
                ArgumentCaptor.forClass(Message.class);

        verify(firebaseMessaging)
                .send(messageCaptor.capture());

        assertThat(messageCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("FCM 등록 토큰이 null이면 TOKEN_INVALID 결과를 반환한다")
    void returnsInvalidTokenWhenRegistrationTokenIsNull() {
        // when
        PushSendResult result = sender.send(
                null,
                payload
        );

        // then
        assertThat(result.type())
                .isEqualTo(
                        PushSendResultType.TOKEN_INVALID
                );
        assertThat(result.errorCode())
                .isEqualTo("EMPTY_REGISTRATION_TOKEN");
        assertThat(result.errorMessage())
                .isEqualTo(
                        "FCM registration token이 비어 있습니다."
                );

        verifyNoInteractions(firebaseMessaging);
    }

    @Test
    @DisplayName("FCM 등록 토큰이 공백이면 TOKEN_INVALID 결과를 반환한다")
    void returnsInvalidTokenWhenRegistrationTokenIsBlank() {
        // when
        PushSendResult result = sender.send(
                "   ",
                payload
        );

        // then
        assertThat(result.type())
                .isEqualTo(
                        PushSendResultType.TOKEN_INVALID
                );
        assertThat(result.errorCode())
                .isEqualTo("EMPTY_REGISTRATION_TOKEN");
        assertThat(result.errorMessage())
                .isEqualTo(
                        "FCM registration token이 비어 있습니다."
                );

        verifyNoInteractions(firebaseMessaging);
    }

    @Test
    @DisplayName("등록 해제된 FCM 토큰이면 TOKEN_INVALID 결과를 반환한다")
    void returnsInvalidTokenWhenTokenIsUnregistered()
            throws Exception {
        // given
        FirebaseMessagingException exception =
                firebaseException(
                        MessagingErrorCode.UNREGISTERED,
                        null,
                        "Requested entity was not found."
                );

        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(exception);

        // when
        PushSendResult result = sender.send(
                "unregistered-token",
                payload
        );

        // then
        assertThat(result.type())
                .isEqualTo(
                        PushSendResultType.TOKEN_INVALID
                );
        assertThat(result.errorCode())
                .isEqualTo("UNREGISTERED");
        assertThat(result.errorMessage())
                .isEqualTo(
                        "Requested entity was not found."
                );

        verify(firebaseMessaging)
                .send(any(Message.class));
    }

    @Test
    @DisplayName("FCM 인자가 유효하지 않으면 TOKEN_INVALID 결과를 반환한다")
    void returnsInvalidTokenForInvalidArgument()
            throws Exception {
        // given
        FirebaseMessagingException exception =
                firebaseException(
                        MessagingErrorCode.INVALID_ARGUMENT,
                        null,
                        "Invalid registration token."
                );

        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(exception);

        // when
        PushSendResult result = sender.send(
                "invalid-token",
                payload
        );

        // then
        assertThat(result.type())
                .isEqualTo(
                        PushSendResultType.TOKEN_INVALID
                );
        assertThat(result.errorCode())
                .isEqualTo("INVALID_ARGUMENT");
        assertThat(result.errorMessage())
                .isEqualTo(
                        "Invalid registration token."
                );
    }

    @Test
    @DisplayName("Sender ID가 일치하지 않으면 TOKEN_INVALID 결과를 반환한다")
    void returnsInvalidTokenForSenderIdMismatch()
            throws Exception {
        // given
        FirebaseMessagingException exception =
                firebaseException(
                        MessagingErrorCode.SENDER_ID_MISMATCH,
                        null,
                        "Sender ID does not match."
                );

        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(exception);

        // when
        PushSendResult result = sender.send(
                "registration-token",
                payload
        );

        // then
        assertThat(result.type())
                .isEqualTo(
                        PushSendResultType.TOKEN_INVALID
                );
        assertThat(result.errorCode())
                .isEqualTo("SENDER_ID_MISMATCH");
        assertThat(result.errorMessage())
                .isEqualTo("Sender ID does not match.");
    }

    @Test
    @DisplayName("FCM 서비스를 사용할 수 없으면 재시도 가능한 실패를 반환한다")
    void returnsRetryableFailureWhenFirebaseIsUnavailable()
            throws Exception {
        // given
        FirebaseMessagingException exception =
                firebaseException(
                        MessagingErrorCode.UNAVAILABLE,
                        null,
                        "Firebase service is unavailable."
                );

        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(exception);

        // when
        PushSendResult result = sender.send(
                "registration-token",
                payload
        );

        // then
        assertThat(result.type())
                .isEqualTo(
                        PushSendResultType.RETRYABLE_FAILURE
                );
        assertThat(result.errorCode())
                .isEqualTo("UNAVAILABLE");
        assertThat(result.errorMessage())
                .isEqualTo(
                        "Firebase service is unavailable."
                );
    }

    @Test
    @DisplayName("FCM 내부 오류이면 재시도 가능한 실패를 반환한다")
    void returnsRetryableFailureForInternalError()
            throws Exception {
        // given
        FirebaseMessagingException exception =
                firebaseException(
                        MessagingErrorCode.INTERNAL,
                        null,
                        "Firebase internal error."
                );

        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(exception);

        // when
        PushSendResult result = sender.send(
                "registration-token",
                payload
        );

        // then
        assertThat(result.type())
                .isEqualTo(
                        PushSendResultType.RETRYABLE_FAILURE
                );
        assertThat(result.errorCode())
                .isEqualTo("INTERNAL");
        assertThat(result.errorMessage())
                .isEqualTo("Firebase internal error.");
    }

    @Test
    @DisplayName("FCM 할당량을 초과하면 재시도 가능한 실패를 반환한다")
    void returnsRetryableFailureWhenQuotaIsExceeded()
            throws Exception {
        // given
        FirebaseMessagingException exception =
                firebaseException(
                        MessagingErrorCode.QUOTA_EXCEEDED,
                        null,
                        "Firebase quota exceeded."
                );

        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(exception);

        // when
        PushSendResult result = sender.send(
                "registration-token",
                payload
        );

        // then
        assertThat(result.type())
                .isEqualTo(
                        PushSendResultType.RETRYABLE_FAILURE
                );
        assertThat(result.errorCode())
                .isEqualTo("QUOTA_EXCEEDED");
        assertThat(result.errorMessage())
                .isEqualTo("Firebase quota exceeded.");
    }

    @Test
    @DisplayName("플랫폼의 일시적인 오류이면 재시도 가능한 실패를 반환한다")
    void returnsRetryableFailureForPlatformError()
            throws Exception {
        // given
        FirebaseMessagingException exception =
                firebaseException(
                        null,
                        ErrorCode.DEADLINE_EXCEEDED,
                        "Firebase request deadline exceeded."
                );

        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(exception);

        // when
        PushSendResult result = sender.send(
                "registration-token",
                payload
        );

        // then
        assertThat(result.type())
                .isEqualTo(
                        PushSendResultType.RETRYABLE_FAILURE
                );
        assertThat(result.errorCode())
                .isEqualTo("DEADLINE_EXCEEDED");
        assertThat(result.errorMessage())
                .isEqualTo(
                        "Firebase request deadline exceeded."
                );
    }

    @Test
    @DisplayName("재시도 대상으로 분류되지 않은 FCM 오류이면 영구 실패를 반환한다")
    void returnsPermanentFailureForUnknownFirebaseError()
            throws Exception {
        // given
        FirebaseMessagingException exception =
                firebaseException(
                        null,
                        ErrorCode.PERMISSION_DENIED,
                        "Permission denied."
                );

        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(exception);

        // when
        PushSendResult result = sender.send(
                "registration-token",
                payload
        );

        // then
        assertThat(result.type())
                .isEqualTo(
                        PushSendResultType.PERMANENT_FAILURE
                );
        assertThat(result.errorCode())
                .isEqualTo("PERMISSION_DENIED");
        assertThat(result.errorMessage())
                .isEqualTo("Permission denied.");
    }

    @Test
    @DisplayName("FCM 메시지 생성 또는 전송 준비 중 RuntimeException이 발생하면 재시도 가능한 실패를 반환한다")
    void returnsRetryableFailureForRuntimeException()
            throws Exception {
        // given
        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(
                        new IllegalStateException(
                                "Firebase runtime failure"
                        )
                );

        // when
        PushSendResult result = sender.send(
                "registration-token",
                payload
        );

        // then
        assertThat(result.type())
                .isEqualTo(
                        PushSendResultType.RETRYABLE_FAILURE
                );
        assertThat(result.errorCode())
                .isEqualTo("FIREBASE_RUNTIME_ERROR");
        assertThat(result.errorMessage())
                .isEqualTo("Firebase runtime failure");
    }

    private FirebaseMessagingException firebaseException(
            MessagingErrorCode messagingErrorCode,
            ErrorCode platformErrorCode,
            String message
    ) {
        FirebaseMessagingException exception =
                org.mockito.Mockito.mock(
                        FirebaseMessagingException.class
                );

        when(exception.getMessage())
                .thenReturn(message);

        if (messagingErrorCode != null) {
            when(exception.getMessagingErrorCode())
                    .thenReturn(messagingErrorCode);
        }

        if (platformErrorCode != null) {
            when(exception.getErrorCode())
                    .thenReturn(platformErrorCode);
        }

        return exception;
    }
}