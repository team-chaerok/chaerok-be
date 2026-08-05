package com.chaerok.backend.filmroll.repository;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FilmRollRepository
        extends JpaRepository<FilmRoll, Long> {

    Optional<FilmRoll> findByIdAndUserId(
            Long filmRollId,
            Long userId
    );

    Optional<FilmRoll> findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
            Long userId,
            List<FilmRollStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select filmRoll
            from FilmRoll filmRoll
            where filmRoll.id = :filmRollId
              and filmRoll.user.id = :userId
            """)
    Optional<FilmRoll> findByIdAndUserIdForUpdate(
            @Param("filmRollId") Long filmRollId,
            @Param("userId") Long userId
    );

    List<FilmRoll> findAllByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    boolean existsByUserIdAndStatusIn(
            Long userId,
            List<FilmRollStatus> statuses
    );
}
