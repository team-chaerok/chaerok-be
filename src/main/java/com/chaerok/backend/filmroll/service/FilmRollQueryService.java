package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollResponse;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollNotFoundException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FilmRollQueryService {

    private final FilmRollRepository filmRollRepository;

    public Optional<FilmRollResponse> findCurrentFilmRoll(Long userId) {
        return filmRollRepository
                .findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                        userId,
                        FilmRollStatus.incompleteStatuses()
                )
                .map(FilmRollResponse::from);
    }

    public FilmRollResponse getFilmRoll(
            Long userId,
            Long filmRollId
    ) {
        return filmRollRepository
                .findByIdAndUserId(filmRollId, userId)
                .map(FilmRollResponse::from)
                .orElseThrow(FilmRollNotFoundException::new);
    }
}
