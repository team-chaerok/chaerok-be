package com.chaerok.backend.notification.message;

import java.util.Map;

public record NotificationPayload(
        String title,
        String body,
        String collapseKey,
        String notificationTag,
        Map<String, String> data
) {
    public NotificationPayload {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "알림 제목은 필수입니다."
            );
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException(
                    "알림 본문은 필수입니다."
            );
        }
        if (collapseKey == null || collapseKey.isBlank()) {
            throw new IllegalArgumentException(
                    "알림 collapse key는 필수입니다."
            );
        }
        if (notificationTag == null || notificationTag.isBlank()) {
            throw new IllegalArgumentException(
                    "알림 tag는 필수입니다."
            );
        }
        if (data == null) {
            throw new IllegalArgumentException(
                    "알림 data는 필수입니다."
            );
        }

        data = Map.copyOf(data);
    }
}