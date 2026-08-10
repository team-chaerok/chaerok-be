package com.chaerok.backend.render.repository;

import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.entity.RenderJobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select renderJob
            from RenderJob renderJob
            where renderJob.id = :renderJobId
            """)
    Optional<RenderJob> findByIdForUpdate(
            @Param("renderJobId") UUID renderJobId
    );
}
