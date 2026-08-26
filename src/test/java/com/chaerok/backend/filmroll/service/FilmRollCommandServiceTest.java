package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollCreateRequest;
import com.chaerok.backend.filmroll.dto.FilmRollResponse;
import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.ActiveFilmRollExistsException;
import com.chaerok.backend.filmroll.exception.FilmRollConflictException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.filter.preset.FilmFilterPreset;
import com.chaerok.backend.filter.preset.FilmFilterPresetProvider;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.repository.PhotoRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.repository.RegionRepository;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.repository.UserRepository;
import com.chaerok.backend.visit.exception.VisitRequirementNotMetException;
import com.chaerok.backend.visit.service.VisitRequirementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollCommandServiceTest {

    @Mock
    private FilmRollRepository filmRollRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private FilmFilterPresetProvider filterPresetProvider;

    @Mock
    private RegionFilterPolicy regionFilterPolicy;

    @Mock
    private VisitRequirementService visitRequirementService;

    @Mock
    private FilmRollDevelopmentTimingService developmentTimingService;

    private FilmRollCommandService service;
    private User user;
    private Region region;

    @BeforeEach
    void setUp() {
        service = new FilmRollCommandService(
                filmRollRepository,
                photoRepository,
                userRepository,
                regionRepository,
                filterPresetProvider,
                regionFilterPolicy,
                visitRequirementService,
                developmentTimingService
        );

        user = mock(User.class);
        region = mock(Region.class);

        org.mockito.Mockito.lenient()
                .when(user.getId())
                .thenReturn(1L);

        org.mockito.Mockito.lenient()
                .when(region.getId())
                .thenReturn(10L);
        org.mockito.Mockito.lenient()
                .when(region.isServiceEnabled())
                .thenReturn(true);

        org.mockito.Mockito.lenient()
                .when(userRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(user));

        org.mockito.Mockito.lenient()
                .when(filmRollRepository
                        .findByUserIdAndClientFilmRollId(
                                org.mockito.ArgumentMatchers.eq(1L),
                                org.mockito.ArgumentMatchers.any(
                                        java.util.UUID.class
                                )
                        ))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("인증 사용자에게 촬영 중 필름 롤을 생성한다")
    void createFilmRoll() {
        FilmRollCreateRequest request =
                new FilmRollCreateRequest(
                        java.util.UUID.randomUUID(),
                        10L,
                        "gongju",
                        0.8
                );

        when(filmRollRepository.existsByUserIdAndStatusAndExitedAtIsNull(
                1L,
                FilmRollStatus.CAPTURING
        )).thenReturn(false);


        when(regionRepository.findById(10L))
                .thenReturn(Optional.of(region));

        when(filterPresetProvider.getByFilterId(
                "gongju"
        )).thenReturn(mock(FilmFilterPreset.class));

        when(filmRollRepository.saveAndFlush(
                any(FilmRoll.class)
        )).thenAnswer(invocation -> {
            FilmRoll filmRoll = invocation.getArgument(0);
            ReflectionTestUtils.setField(
                    filmRoll,
                    "id",
                    100L
            );
            return filmRoll;
        });

        FilmRollResponse response =
                service.createFilmRoll(
                        1L,
                        request
                );

        assertThat(response.filmRollId()).isEqualTo(100L);
        assertThat(response.clientFilmRollId())
                .isEqualTo(request.clientFilmRollId());
        assertThat(response.regionId()).isEqualTo(10L);
        assertThat(response.status())
                .isEqualTo(FilmRollStatus.CAPTURING.name());
        assertThat(response.totalPhotoCount()).isZero();

        verify(filterPresetProvider)
                .getByFilterId("gongju");
        verify(regionFilterPolicy)
                .validate(region, "gongju");
    }

    @Test
    @DisplayName("같은 clientFilmRollId 재요청은 기존 서버 필름 롤을 반환한다")
    void returnExistingFilmRollForSameClientFilmRollId() {
        java.util.UUID clientFilmRollId =
                java.util.UUID.fromString(
                        "1b8dba58-2503-4b8c-bff8-7445fab9a6d7"
                );
        FilmRollCreateRequest request =
                new FilmRollCreateRequest(
                        clientFilmRollId,
                        10L,
                        "gongju",
                        0.8
                );

        FilmRoll existingFilmRoll = FilmRoll.create(
                user,
                region,
                clientFilmRollId,
                "gongju",
                0.8,
                1
        );
        ReflectionTestUtils.setField(
                existingFilmRoll,
                "id",
                100L
        );

        when(filmRollRepository
                .findByUserIdAndClientFilmRollId(
                        1L,
                        clientFilmRollId
                ))
                .thenReturn(Optional.of(existingFilmRoll));

        FilmRollResponse response =
                service.createFilmRoll(
                        1L,
                        request
                );

        assertThat(response.filmRollId()).isEqualTo(100L);
        assertThat(response.clientFilmRollId())
                .isEqualTo(clientFilmRollId);

        verify(userRepository).findByIdForUpdate(1L);
        verify(filmRollRepository, never())
                .existsByUserIdAndStatusAndExitedAtIsNull(
                        1L,
                        FilmRollStatus.CAPTURING
                );
        verify(filmRollRepository, never())
                .saveAndFlush(any(FilmRoll.class));
        verifyNoInteractions(
                regionRepository,
                filterPresetProvider,
                regionFilterPolicy,
                visitRequirementService,
                developmentTimingService
        );
    }
    @Test
    @DisplayName("지역과 필터 조합이 맞지 않으면 필름 롤을 생성하지 않는다")
    void rejectMismatchedRegionFilter() {
        FilmRollCreateRequest request =
                new FilmRollCreateRequest(
                        java.util.UUID.randomUUID(),
                        10L,
                        "buyeo",
                        0.8
                );

        when(filmRollRepository.existsByUserIdAndStatusAndExitedAtIsNull(
                1L,
                FilmRollStatus.CAPTURING
        )).thenReturn(false);
        when(regionRepository.findById(10L))
                .thenReturn(Optional.of(region));
        when(filterPresetProvider.getByFilterId("buyeo"))
                .thenReturn(mock(FilmFilterPreset.class));
        doThrow(new IllegalArgumentException(
                "선택한 지역에서 사용할 수 없는 필터입니다."
        )).when(regionFilterPolicy).validate(region, "buyeo");

        assertThatThrownBy(() ->
                service.createFilmRoll(1L, request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용할 수 없는 필터");

        verify(regionFilterPolicy).validate(region, "buyeo");
        verify(filmRollRepository, never())
                .saveAndFlush(any(FilmRoll.class));
    }

    @Test
    @DisplayName("활성 필름 롤이 있으면 새 필름 롤을 생성하지 않는다")
    void rejectDuplicateActiveFilmRoll() {
        when(filmRollRepository.existsByUserIdAndStatusAndExitedAtIsNull(
                1L,
                FilmRollStatus.CAPTURING
        )).thenReturn(true);

        FilmRollCreateRequest request =
                new FilmRollCreateRequest(
                        java.util.UUID.randomUUID(),
                        10L,
                        "gongju",
                        0.8
                );

        assertThatThrownBy(() ->
                service.createFilmRoll(
                        1L,
                        request
                )
        ).isInstanceOf(ActiveFilmRollExistsException.class);

        verify(filmRollRepository)
                .existsByUserIdAndStatusAndExitedAtIsNull(
                        1L,
                        FilmRollStatus.CAPTURING
                );
        verify(userRepository).findByIdForUpdate(1L);
        verifyNoInteractions(
                regionRepository,
                filterPresetProvider,
                regionFilterPolicy,
                visitRequirementService,
                developmentTimingService
        );
    }

    @Test
    @DisplayName("사진 업로드와 Visit 조건이 충족되면 READY 상태로 전환한다")
    void markReady() {
        FilmRoll filmRoll = FilmRoll.create(
                user,
                region,
                "gongju",
                0.8,
                1
        );

        ReflectionTestUtils.setField(
                filmRoll,
                "id",
                100L
        );

        filmRoll.increasePhotoCount();

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));

        stubCompletedPhotoUpload(filmRoll);

        FilmRollResponse response =
                service.markReady(
                        1L,
                        100L
                );

        assertThat(response.status())
                .isEqualTo(FilmRollStatus.READY.name());
        verify(visitRequirementService).requireSatisfied(100L);
        verify(developmentTimingService).requireAvailable(filmRoll);
    }

    @Test
    @DisplayName("Visit 조건이 부족하면 숨겨진 READY 전환 API도 상태를 바꾸지 않는다")
    void markReadyRejectsUnsatisfiedVisitRequirement() {
        FilmRoll filmRoll = createFilmRollWithOnePhoto();

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));
        doThrow(new VisitRequirementNotMetException())
                .when(visitRequirementService)
                .requireSatisfied(100L);

        assertThatThrownBy(() -> service.markReady(1L, 100L))
                .isInstanceOf(VisitRequirementNotMetException.class);

        assertThat(filmRoll.getStatus())
                .isEqualTo(FilmRollStatus.CAPTURING);
    }

    @Test
    @DisplayName("Visit 조건이 부족하면 CAPTURING 상태를 유지한 채 현상을 거부한다")
    void rejectDevelopmentWhenVisitRequirementIsNotMet() {
        FilmRoll filmRoll = createFilmRollWithOnePhoto();

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));
        doThrow(new VisitRequirementNotMetException())
                .when(visitRequirementService)
                .requireSatisfied(100L);

        assertThatThrownBy(() ->
                service.prepareDevelopment(1L, 100L)
        ).isInstanceOf(VisitRequirementNotMetException.class);

        assertThat(filmRoll.getStatus())
                .isEqualTo(FilmRollStatus.CAPTURING);
    }

    @Test
    @DisplayName("CAPTURING 필름 롤은 업로드를 검증하고 현상 요청 준비 상태로 전환한다")
    void prepareDevelopmentFromCapturing() {
        FilmRoll filmRoll = createFilmRollWithOnePhoto();

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));
        stubCompletedPhotoUpload(filmRoll);

        PreparedFilmRollDevelopment preparation =
                service.prepareDevelopment(1L, 100L);

        assertThat(preparation.renderRequestRequired()).isTrue();
        assertThat(preparation.status())
                .isEqualTo(FilmRollStatus.READY.name());
        assertThat(filmRoll.getStatus())
                .isEqualTo(FilmRollStatus.READY);
        verify(visitRequirementService).requireSatisfied(100L);
        verify(developmentTimingService).requireAvailable(filmRoll);
    }

    @Test
    @DisplayName("READY 필름 롤도 Visit 조건과 이탈 시간을 다시 확인한 뒤 현상을 요청한다")
    void prepareDevelopmentFromReady() {
        FilmRoll filmRoll = createFilmRollWithOnePhoto();
        filmRoll.markReady();

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));
        stubCompletedPhotoUpload(filmRoll);

        PreparedFilmRollDevelopment preparation =
                service.prepareDevelopment(1L, 100L);

        assertThat(preparation.renderRequestRequired()).isTrue();
        assertThat(preparation.status())
                .isEqualTo(FilmRollStatus.READY.name());
        verify(visitRequirementService).requireSatisfied(100L);
        verify(developmentTimingService).requireAvailable(filmRoll);
    }

    @Test
    @DisplayName("FAILED 필름 롤은 같은 사진과 방문 기록으로 재시도할 수 있도록 READY로 복구한다")
    void prepareDevelopmentFromFailed() {
        FilmRoll filmRoll = createFilmRollWithOnePhoto();
        LocalDateTime requestedAt =
                LocalDateTime.of(2026, 8, 5, 18, 0);
        filmRoll.markReady();
        filmRoll.markQueued(requestedAt);
        filmRoll.failFromResult(
                "MEDIA_GENERATION_FAILED",
                "릴스 생성 실패"
        );

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));
        stubCompletedPhotoUpload(filmRoll);

        PreparedFilmRollDevelopment preparation =
                service.prepareDevelopment(1L, 100L);

        assertThat(preparation.renderRequestRequired()).isTrue();
        assertThat(preparation.status())
                .isEqualTo(FilmRollStatus.READY.name());
        assertThat(filmRoll.getStatus())
                .isEqualTo(FilmRollStatus.READY);
        assertThat(filmRoll.getErrorCode()).isNull();
        assertThat(filmRoll.getErrorMessage()).isNull();
        verify(visitRequirementService).requireSatisfied(100L);
        verify(developmentTimingService).requireAvailable(filmRoll);
    }

    @Test
    @DisplayName("현상 시 남은 UPLOADING 슬롯은 버리고 완료 사진 순서를 연속으로 압축한다")
    void finalizeAbandonedUploadingSlotBeforeDevelopment() {
        FilmRoll filmRoll = FilmRoll.create(
                user,
                region,
                "gongju",
                0.8,
                1
        );
        ReflectionTestUtils.setField(filmRoll, "id", 100L);
        filmRoll.increasePhotoCount();
        filmRoll.increasePhotoCount();

        Photo abandoned = Photo.create(
                filmRoll,
                1,
                "users/1/rolls/100/original/001-abandoned.jpg",
                LocalDateTime.of(2026, 8, 11, 18, 0)
        );
        Photo second = uploadedPhoto(filmRoll, 2);
        Photo third = uploadedPhoto(filmRoll, 3);

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));
        when(photoRepository
                .findAllByFilmRollIdOrderBySequenceAscForUpdate(100L))
                .thenReturn(List.of(abandoned, second, third));

        PreparedFilmRollDevelopment preparation =
                service.prepareDevelopment(1L, 100L);

        assertThat(preparation.renderRequestRequired()).isTrue();
        assertThat(filmRoll.getStatus()).isEqualTo(FilmRollStatus.READY);
        assertThat(second.getSequence()).isEqualTo(1);
        assertThat(third.getSequence()).isEqualTo(2);
        verify(photoRepository).deleteAll(List.of(abandoned));
        verify(photoRepository, times(3)).flush();
    }

    @Test
    @DisplayName("현상 가능 시각 전에는 UPLOADING 슬롯을 정리하지 않는다")
    void doesNotFinalizePhotosBeforeDevelopmentIsAvailable() {
        FilmRoll filmRoll = createFilmRollWithOnePhoto();

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));
        doThrow(new com.chaerok.backend.filmroll.exception
                .FilmRollDevelopmentWaitException())
                .when(developmentTimingService)
                .requireAvailable(filmRoll);

        assertThatThrownBy(() ->
                service.prepareDevelopment(1L, 100L)
        ).isInstanceOf(
                com.chaerok.backend.filmroll.exception
                        .FilmRollDevelopmentWaitException.class
        );

        verify(photoRepository, never())
                .findAllByFilmRollIdOrderBySequenceAscForUpdate(100L);
        verify(photoRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("이미 QUEUED 상태이면 새 현상 작업 준비를 하지 않는다")
    void prepareDevelopmentReturnsExistingQueuedState() {
        FilmRoll filmRoll = createFilmRollWithOnePhoto();
        LocalDateTime requestedAt =
                LocalDateTime.of(2026, 8, 5, 18, 0);
        filmRoll.markReady();
        filmRoll.markQueued(requestedAt);

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));

        PreparedFilmRollDevelopment preparation =
                service.prepareDevelopment(1L, 100L);

        assertThat(preparation.renderRequestRequired()).isFalse();
        assertThat(preparation.status())
                .isEqualTo(FilmRollStatus.QUEUED.name());
        assertThat(preparation.requestedAt()).isEqualTo(requestedAt);
        verifyNoInteractions(photoRepository);
    }

    @Test
    @DisplayName("완료된 필름 롤은 다시 현상할 수 없다")
    void rejectDevelopmentForCompletedFilmRoll() {
        FilmRoll filmRoll = createFilmRollWithOnePhoto();
        LocalDateTime completedAt =
                LocalDateTime.of(2026, 8, 5, 18, 0);
        filmRoll.markReady();
        filmRoll.markQueued(completedAt.minusMinutes(1));
        filmRoll.completeFromResult(
                "result.zip",
                "result.mp4",
                completedAt
        );

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));

        assertThatThrownBy(() ->
                service.prepareDevelopment(1L, 100L)
        )
                .isInstanceOf(FilmRollConflictException.class)
                .hasMessageContaining("이미 현상이 완료");

        verifyNoInteractions(photoRepository);
    }

    private FilmRoll createFilmRollWithOnePhoto() {
        FilmRoll filmRoll = FilmRoll.create(
                user,
                region,
                "gongju",
                0.8,
                1
        );

        ReflectionTestUtils.setField(
                filmRoll,
                "id",
                100L
        );
        filmRoll.increasePhotoCount();
        return filmRoll;
    }

    private void stubCompletedPhotoUpload(FilmRoll filmRoll) {
        when(photoRepository
                .findAllByFilmRollIdOrderBySequenceAscForUpdate(100L))
                .thenReturn(List.of(uploadedPhoto(filmRoll, 1)));
    }

    private Photo uploadedPhoto(
            FilmRoll filmRoll,
            int sequence
    ) {
        Photo photo = Photo.create(
                filmRoll,
                sequence,
                "users/1/rolls/100/original/%03d.jpg"
                        .formatted(sequence),
                LocalDateTime.of(2026, 8, 11, 18, sequence)
        );
        photo.markUploaded(
                LocalDateTime.of(2026, 8, 11, 18, sequence + 1)
        );
        return photo;
    }

}
