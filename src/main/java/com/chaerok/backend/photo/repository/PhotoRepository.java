package com.chaerok.backend.photo.repository;

import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.entity.PhotoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhotoRepository
        extends JpaRepository<Photo, Long> {

    Optional<Photo> findByIdAndFilmRollId(
            Long photoId,
            Long filmRollId
    );

    Optional<Photo> findByFilmRollIdAndSequence(
            Long filmRollId,
            Integer sequence
    );

    List<Photo> findAllByFilmRollIdOrderBySequenceAsc(
            Long filmRollId
    );

    long countByFilmRollId(Long filmRollId);

    boolean existsByFilmRollIdAndSequence(
            Long filmRollId,
            Integer sequence
    );

    boolean existsByFilmRollIdAndStatusNot(
            Long filmRollId,
            PhotoStatus status
    );
}
