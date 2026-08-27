package com.chaerok.backend.render.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.global.aws.AwsProperties;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.entity.PhotoStatus;
import com.chaerok.backend.photo.repository.PhotoRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.entity.RenderJobStatus;
import com.chaerok.backend.render.repository.RenderJobRepository;
import com.chaerok.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderJobRecoveryPreparationServiceTest {

    @Mock
    private RenderJobRepository renderJobRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private RenderJob renderJob;

    @Mock
    private FilmRoll filmRoll;

    @Mock
    private Photo photo;

    @Mock
    private User user;

    @Mock
    private Region region;

    private RenderJobRecoveryPreparationService service;

    @BeforeEach
    void setUp() {
        AwsProperties awsProperties = new AwsProperties();
        awsProperties.getS3().setBucket("bucket");

        service = new RenderJobRecoveryPreparationService(
                renderJobRepository,
                photoRepository,
                awsProperties
        );
    }

    @Test
    @DisplayName("5분 이상 CREATED 작업은 같은 renderJobId로 SQS 메시지를 복구한다")
    void preparesStaleCreatedJob() {
        UUID renderJobId = UUID.randomUUID();

        LocalDateTime createdAt =
                LocalDateTime.of(2026, 8, 27, 15, 0);

        LocalDateTime createdBefore =
                LocalDateTime.of(2026, 8, 27, 15, 5);

        LocalDateTime takenAt =
                LocalDateTime.of(2026, 8, 27, 14, 50);

        when(renderJobRepository.findByIdForUpdate(renderJobId))
                .thenReturn(Optional.of(renderJob));

        when(renderJob.getStatus())
                .thenReturn(RenderJobStatus.CREATED);

        when(renderJob.getCreatedAt())
                .thenReturn(createdAt);

        when(renderJob.getFilmRoll())
                .thenReturn(filmRoll);

        when(renderJob.getId())
                .thenReturn(renderJobId);

        when(filmRoll.getStatus())
                .thenReturn(FilmRollStatus.READY);

        when(filmRoll.getId())
                .thenReturn(2L);

        when(filmRoll.getTotalPhotoCount())
                .thenReturn(1);

        when(filmRoll.getUser())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(6L);

        when(filmRoll.getRegion())
                .thenReturn(region);

        when(region.getId())
                .thenReturn(1L);

        when(filmRoll.getFilterId())
                .thenReturn("gongju");

        when(filmRoll.getFilterStrength())
                .thenReturn(0.8);

        when(filmRoll.getFilterVersion())
                .thenReturn(1);

        when(photoRepository
                .findAllByFilmRollIdOrderBySequenceAsc(2L))
                .thenReturn(List.of(photo));

        when(photo.getId())
                .thenReturn(10L);

        when(photo.getSequence())
                .thenReturn(1);

        when(photo.getStatus())
                .thenReturn(PhotoStatus.UPLOADED);

        when(photo.getOriginalObjectKey())
                .thenReturn(
                        "users/6/rolls/2/original/001.jpg"
                );

        when(photo.getTakenAt())
                .thenReturn(takenAt);

        Optional<PreparedRenderJob> result =
                service.prepare(
                        renderJobId,
                        createdBefore
                );

        assertThat(result).isPresent();

        PreparedRenderJob prepared = result.orElseThrow();

        assertThat(prepared.renderJobId())
                .isEqualTo(renderJobId);

        assertThat(prepared.filmRollId())
                .isEqualTo(2L);

        assertThat(prepared.userId())
                .isEqualTo(6L);

        assertThat(prepared.message().renderJobId())
                .isEqualTo(renderJobId);

        assertThat(prepared.message().filmRollId())
                .isEqualTo(2L);

        assertThat(prepared.message().bucket())
                .isEqualTo("bucket");

        assertThat(prepared.message().filterId())
                .isEqualTo("gongju");

        assertThat(prepared.message().requestedAt())
                .isEqualTo(createdAt);

        assertThat(prepared.message().photos())
                .hasSize(1);

        assertThat(prepared.message().photos().get(0).photoId())
                .isEqualTo(10L);

        assertThat(prepared.message().photos().get(0).sequence())
                .isEqualTo(1);

        assertThat(
                prepared.message()
                        .photos()
                        .get(0)
                        .originalObjectKey()
        ).isEqualTo(
                "users/6/rolls/2/original/001.jpg"
        );
    }

    @Test
    @DisplayName("CREATED가 아닌 작업은 복구하지 않는다")
    void skipsNonCreatedJob() {
        UUID renderJobId = UUID.randomUUID();

        when(renderJobRepository.findByIdForUpdate(renderJobId))
                .thenReturn(Optional.of(renderJob));

        when(renderJob.getStatus())
                .thenReturn(RenderJobStatus.QUEUED);

        Optional<PreparedRenderJob> result =
                service.prepare(
                        renderJobId,
                        LocalDateTime.now()
                );

        assertThat(result).isEmpty();

        verifyNoInteractions(photoRepository);
    }

    @Test
    @DisplayName("아직 stale 기준 시간이 지나지 않은 CREATED 작업은 복구하지 않는다")
    void skipsFreshCreatedJob() {
        UUID renderJobId = UUID.randomUUID();

        LocalDateTime createdBefore =
                LocalDateTime.of(2026, 8, 27, 15, 0);

        when(renderJobRepository.findByIdForUpdate(renderJobId))
                .thenReturn(Optional.of(renderJob));

        when(renderJob.getStatus())
                .thenReturn(RenderJobStatus.CREATED);

        when(renderJob.getCreatedAt())
                .thenReturn(
                        createdBefore.plusSeconds(1)
                );

        Optional<PreparedRenderJob> result =
                service.prepare(
                        renderJobId,
                        createdBefore
                );

        assertThat(result).isEmpty();

        verifyNoInteractions(photoRepository);
    }
}