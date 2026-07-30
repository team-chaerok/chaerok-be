package com.chaerok.backend.render.repository;

import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.entity.RenderJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RenderJobRepository
        extends JpaRepository<RenderJob, UUID> {

    List<RenderJob> findAllByFilmRollIdOrderByCreatedAtDesc(
            Long filmRollId
    );

    Optional<RenderJob> findFirstByFilmRollIdOrderByCreatedAtDesc(
            Long filmRollId
    );

    boolean existsByFilmRollIdAndStatusIn(
            Long filmRollId,
            Collection<RenderJobStatus> statuses
    );
}
