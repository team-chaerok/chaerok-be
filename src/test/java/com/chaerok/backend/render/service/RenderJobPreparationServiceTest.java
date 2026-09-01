package com.chaerok.backend.render.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.filmroll.service.FilmRollDevelopmentTimingService;
import com.chaerok.backend.global.aws.AwsProperties;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.entity.PhotoStatus;
import com.chaerok.backend.photo.repository.PhotoRepository;
import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.exception.RenderErrorCode;
import com.chaerok.backend.render.repository.RenderJobRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.visit.exception.VisitErrorCode;
import com.chaerok.backend.visit.service.VisitRequirementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderJobPreparationServiceTest {

    @Mock
    private FilmRollRepository filmRollRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private RenderJobRepository renderJobRepository;

    @Mock
    private AwsProperties awsProperties;

    @Mock
    private VisitRequirementService visitRequirementService;

    @Mock
    private FilmRollDevelopmentTimingService developmentTimingService;

    @Mock
    private FilmRoll filmRoll;

    @Mock
    private Photo firstPhoto;

    @Mock
    private Photo thirdPhoto;

    @Mock
    private User user;

    @Mock
    private Region region;

    @Mock
    private AwsProperties.S3 s3Properties;

    @Test
    @DisplayName("사진 순서가 1부터 연속되지 않으면 SQS 작업 생성 전에 거부한다")
    void rejectsNonContiguousPhotoSequences() {
        when(filmRollRepository.findByIdAndUserIdForUpdate(2L, 6L))
                .thenReturn(Optional.of(filmRoll));
        when(filmRoll.getStatus())
                .thenReturn(FilmRollStatus.READY);
        when(renderJobRepository.existsByFilmRollIdAndStatusIn(
                eq(2L),
                anyList()
        )).thenReturn(false);
        when(photoRepository
                .findAllByFilmRollIdOrderBySequenceAsc(2L))
                .thenReturn(List.of(firstPhoto, thirdPhoto));
        when(filmRoll.getTotalPhotoCount()).thenReturn(2);
        when(firstPhoto.getSequence()).thenReturn(1);
        when(thirdPhoto.getSequence()).thenReturn(3);

        RenderJobPreparationService service =
                new RenderJobPreparationService(
                        filmRollRepository,
                        photoRepository,
                        renderJobRepository,
                        awsProperties,
                        visitRequirementService,
                        developmentTimingService
                );

        assertThatThrownBy(() -> service.prepare(6L, 2L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(RenderErrorCode.INVALID_PHOTO_SEQUENCE)
                );

        verify(renderJobRepository, never())
                .saveAndFlush(any(RenderJob.class));
    }

    @Test
    @DisplayName("Visit 조건이 미충족이면 RenderJob 생성 전에 거부한다")
    void rejectsUnsatisfiedVisitRequirement() {
        when(filmRollRepository.findByIdAndUserIdForUpdate(2L, 6L))
                .thenReturn(Optional.of(filmRoll));
        when(filmRoll.getStatus())
                .thenReturn(FilmRollStatus.READY);
        when(renderJobRepository.existsByFilmRollIdAndStatusIn(
                eq(2L),
                anyList()
        )).thenReturn(false);
        when(photoRepository
                .findAllByFilmRollIdOrderBySequenceAsc(2L))
                .thenReturn(List.of(firstPhoto));
        when(filmRoll.getTotalPhotoCount()).thenReturn(1);
        when(firstPhoto.getSequence()).thenReturn(1);
        when(firstPhoto.getStatus()).thenReturn(PhotoStatus.UPLOADED);
        org.mockito.Mockito.doThrow(new BusinessException(
                        VisitErrorCode.VISIT_REQUIREMENT_NOT_MET
                )
        ).when(visitRequirementService).requireSatisfied(2L);

        RenderJobPreparationService service =
                new RenderJobPreparationService(
                        filmRollRepository,
                        photoRepository,
                        renderJobRepository,
                        awsProperties,
                        visitRequirementService,
                        developmentTimingService
                );

        assertThatThrownBy(() -> service.prepare(6L, 2L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(VisitErrorCode.VISIT_REQUIREMENT_NOT_MET)
                );

        verify(renderJobRepository, never())
                .saveAndFlush(any(RenderJob.class));
    }

    @Test
    @DisplayName("Visit 조건이 충족되면 기존 RenderJob 준비 흐름을 그대로 진행한다")
    void preparesRenderJobWhenVisitRequirementIsSatisfied() {
        LocalDateTime takenAt =
                LocalDateTime.of(2026, 8, 7, 14, 30);

        when(filmRollRepository.findByIdAndUserIdForUpdate(2L, 6L))
                .thenReturn(Optional.of(filmRoll));
        when(filmRoll.getStatus())
                .thenReturn(FilmRollStatus.READY);
        when(renderJobRepository.existsByFilmRollIdAndStatusIn(
                eq(2L),
                anyList()
        )).thenReturn(false);
        when(photoRepository
                .findAllByFilmRollIdOrderBySequenceAsc(2L))
                .thenReturn(List.of(firstPhoto));

        when(filmRoll.getId()).thenReturn(2L);
        when(filmRoll.getTotalPhotoCount()).thenReturn(1);
        when(filmRoll.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(6L);
        when(filmRoll.getRegion()).thenReturn(region);
        when(region.getId()).thenReturn(1L);
        when(filmRoll.getFilterId()).thenReturn("gongju");
        when(filmRoll.getFilterStrength()).thenReturn(0.8);
        when(filmRoll.getFilterVersion()).thenReturn(1);

        when(firstPhoto.getId()).thenReturn(10L);
        when(firstPhoto.getSequence()).thenReturn(1);
        when(firstPhoto.getStatus()).thenReturn(PhotoStatus.UPLOADED);
        when(firstPhoto.getOriginalObjectKey())
                .thenReturn("users/6/rolls/2/original/001.jpg");
        when(firstPhoto.getTakenAt()).thenReturn(takenAt);

        when(awsProperties.getS3()).thenReturn(s3Properties);
        when(s3Properties.getBucket()).thenReturn("chaerok-media-dev");
        when(renderJobRepository.saveAndFlush(any(RenderJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RenderJobPreparationService service =
                new RenderJobPreparationService(
                        filmRollRepository,
                        photoRepository,
                        renderJobRepository,
                        awsProperties,
                        visitRequirementService,
                        developmentTimingService
                );

        PreparedRenderJob prepared = service.prepare(6L, 2L);

        assertThat(prepared.filmRollId()).isEqualTo(2L);
        assertThat(prepared.userId()).isEqualTo(6L);
        assertThat(prepared.message().photos()).hasSize(1);
        assertThat(prepared.message().photos().get(0).sequence())
                .isEqualTo(1);
        verify(visitRequirementService).requireSatisfied(2L);
        verify(developmentTimingService).requireAvailable(filmRoll);
        verify(renderJobRepository).saveAndFlush(any(RenderJob.class));
    }

}
