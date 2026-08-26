package com.chaerok.backend.notification.outbox.repository;

import com.chaerok.backend.notification.outbox.entity.NotificationOutbox;
import com.chaerok.backend.notification.outbox.entity.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationOutboxRepository
        extends JpaRepository<NotificationOutbox, Long> {

    boolean existsByEventKey(String eventKey);

    @Query("""
            select n.id
            from NotificationOutbox n
            where n.status = :status
              and (
                    n.nextAttemptAt is null
                    or n.nextAttemptAt <= :now
                  )
            order by n.createdAt asc
            """)
    List<Long> findDueIds(
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}