package com.chaerok.backend.notification.outbox.repository;

import com.chaerok.backend.notification.outbox.entity.NotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOutboxRepository
        extends JpaRepository<NotificationOutbox, Long> {

    boolean existsByEventKey(String eventKey);
}