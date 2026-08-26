package com.chaerok.backend.notification.device.entity;

import com.chaerok.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "push_devices",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_push_devices_fcm_registration_token",
                        columnNames = "fcm_registration_token"
                )
        },
        indexes = {
                @Index(
                        name = "idx_push_devices_user_id",
                        columnList = "user_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(
            name = "fcm_registration_token",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String fcmRegistrationToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PushPlatform platform;

    @Column(name = "last_registered_at", nullable = false)
    private LocalDateTime lastRegisteredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static PushDevice create(
            User user,
            String token,
            PushPlatform platform,
            LocalDateTime registeredAt
    ) {
        PushDevice device = new PushDevice();
        device.registerTo(
                user,
                token,
                platform,
                registeredAt
        );
        return device;
    }

    public void registerTo(
            User user,
            String token,
            PushPlatform platform,
            LocalDateTime registeredAt
    ) {
        if (user == null) {
            throw new IllegalArgumentException("사용자는 필수입니다.");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("FCM 등록 토큰은 필수입니다.");
        }
        if (platform == null) {
            throw new IllegalArgumentException("푸시 플랫폼은 필수입니다.");
        }
        if (registeredAt == null) {
            throw new IllegalArgumentException("기기 등록 시각은 필수입니다.");
        }

        this.user = user;
        this.fcmRegistrationToken = token;
        this.platform = platform;
        this.lastRegisteredAt = registeredAt;
    }
}