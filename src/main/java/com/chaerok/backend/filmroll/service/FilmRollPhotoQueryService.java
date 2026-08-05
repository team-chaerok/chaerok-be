package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollPhotoListResponse;
import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.exception.FilmRollNotFoundException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FilmRollPhotoQueryService {

    private final FilmRollRepository filmRollRepository;
    private final PhotoRepository photoRepository;

    public FilmRollPhotoListResponse getPhotos(
            Long userId,
            Long filmRollId
    ) {
        FilmRoll filmRoll = filmRollRepository
                .findByIdAndUserId(filmRollId, userId)
                .orElseThrow(FilmRollNotFoundException::new);

        List<Photo> photos = photoRepository
                .findAllByFilmRollIdOrderBySequenceAsc(filmRollId);

        return FilmRollPhotoListResponse.of(
                filmRoll.getId(),
                filmRoll.getStatus().name(),
                filmRoll.getTotalPhotoCount(),
                photos
        );
    }
}
