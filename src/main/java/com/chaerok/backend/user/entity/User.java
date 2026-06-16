package com.chaerok.backend.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_provider_provider_user_id",
                        columnNames = {"provider", "provider_user_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column
    private String email;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private User(
            OAuthProvider provider,
            String providerUserId,
            String nickname,
            String email
    ) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.nickname = nickname;
        this.email = email;
    }

    public static User create(
            OAuthProvider provider,
            String providerUserId,
            String nickname,
            String email
    ) {
        return new User(provider, providerUserId, nickname, email);
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}