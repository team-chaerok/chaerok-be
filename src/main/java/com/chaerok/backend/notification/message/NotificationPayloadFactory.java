package com.chaerok.backend.notification.message;

import com.chaerok.backend.notification.outbox.entity.NotificationOutbox;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NotificationPayloadFactory {

    private static final String DEVELOPMENT_COLLAPSE_KEY =
            "chaerok-development";

    public NotificationPayload from(NotificationOutbox outbox) {
        if (outbox == null) {
            throw new IllegalArgumentException(
                    "알림 Outbox는 필수입니다."
            );
        }

        String title;
        String body;

        switch (outbox.getType()) {
            case RENDER_STARTED -> {
                title = "필름 현상이 시작됐어요";
                body = "사진을 필름 감성으로 현상하고 있어요.";
            }
            case RENDER_COMPLETED -> {
                title = "필름 현상이 완료됐어요";
                body = "채록에 담긴 여행의 기록을 확인해보세요.";
            }
            case RENDER_FAILED -> {
                title = "필름 현상에 문제가 생겼어요";
                body = "앱에서 현상 상태를 확인해주세요.";
            }
            default -> throw new IllegalStateException(
                    "지원하지 않는 알림 유형입니다: "
                            + outbox.getType()
            );
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put(
                "notificationType",
                outbox.getType().name()
        );
        data.put(
                "filmRollId",
                outbox.getFilmRollId().toString()
        );
        data.put(
                "renderJobId",
                outbox.getRenderJobId().toString()
        );
        data.put(
                "eventKey",
                outbox.getEventKey()
        );
        data.put(
                "screen",
                "filmRollResult"
        );

        return new NotificationPayload(
                title,
                body,
                DEVELOPMENT_COLLAPSE_KEY,
                outbox.getEventKey(),
                data
        );
    }
}