package com.chaerok.backend.notification.sender;

import com.chaerok.backend.notification.message.NotificationPayload;

public interface PushSender {

    PushSendResult send(
            String registrationToken,
            NotificationPayload payload
    );
}