package com.chaerok.backend.filmroll.repository;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FilmRollRepository
        extends JpaRepository<FilmRoll, Long> {

    Optional<FilmRoll> findByIdAndUserId(
            Long filmRollId,
            Long userId
    );

    List<FilmRoll> findAllByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    boolean existsByUserIdAndStatusIn(
            Long userId,
            List<FilmRollStatus> statuses
    );
}
