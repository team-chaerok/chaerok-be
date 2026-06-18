package com.chaerok.backend.user.repository;

import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderUserId(
            OAuthProvider provider,
            String providerUserId
    );

    boolean existsByProviderAndProviderUserId(
            OAuthProvider provider,
            String providerUserId
    );
}