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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "review_mode", nullable = false)
    private boolean reviewMode;

    @Column(name = "terms_agreed_at", nullable = false, updatable = false)
    private LocalDateTime termsAgreedAt;

    @Column(name = "privacy_agreed_at", nullable = false, updatable = false)
    private LocalDateTime privacyAgreedAt;

    @Column(name = "terms_version", nullable = false, length = 20, updatable = false)
    private String termsVersion;

    @Column(name = "privacy_version", nullable = false, length = 20, updatable = false)
    private String privacyVersion;

    private User(
            OAuthProvider provider,
            String providerUserId,
            String nickname,
            String email,
            String termsVersion,
            String privacyVersion
    ) {
        LocalDateTime agreedAt = LocalDateTime.now();

        this.provider = provider;
        this.providerUserId = providerUserId;
        this.nickname = nickname;
        this.email = email;
        this.role = UserRole.USER;
        this.termsAgreedAt = agreedAt;
        this.privacyAgreedAt = agreedAt;
        this.termsVersion = termsVersion;
        this.privacyVersion = privacyVersion;
    }

    public static User create(
            OAuthProvider provider,
            String providerUserId,
            String nickname,
            String email,
            String termsVersion,
            String privacyVersion
    ) {
        return new User(
                provider,
                providerUserId,
                nickname,
                email,
                termsVersion,
                privacyVersion
        );
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}