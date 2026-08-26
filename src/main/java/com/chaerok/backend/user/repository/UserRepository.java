package com.chaerok.backend.user.repository;

import com.chaerok.backend.user.entity.OAuthProvider;
import com.chaerok.backend.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select appUser
            from User appUser
            where appUser.id = :userId
            """)
    Optional<User> findByIdForUpdate(
            @Param("userId") Long userId
    );

    Optional<User> findByProviderAndProviderUserId(
            OAuthProvider provider,
            String providerUserId
    );

    boolean existsByProviderAndProviderUserId(
            OAuthProvider provider,
            String providerUserId
    );
}
