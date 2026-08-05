package com.chaerok.backend.photo.repository;

import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.entity.PhotoStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select photo
            from Photo photo
            where photo.filmRoll.id = :filmRollId
            order by photo.sequence asc
            """)
    List<Photo> findAllByFilmRollIdOrderBySequenceAscForUpdate(
            @Param("filmRollId") Long filmRollId
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
