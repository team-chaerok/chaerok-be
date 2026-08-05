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
import com.chaerok.backend.photo.entity.PhotoStatus;
import com.chaerok.backend.photo.repository.PhotoRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.region.repository.RegionRepository;
import com.chaerok.backend.user.entity.User;
import com.chaerok.backend.user.repository.UserRepository;
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
import static org.mockito.Mockito.mock;
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
                filterPresetProvider
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
                .thenReturn(true);    }

    @Test
    @DisplayName("인증 사용자에게 촬영 중 필름 롤을 생성한다")
    void createFilmRoll() {
        FilmRollCreateRequest request =
                new FilmRollCreateRequest(
                        10L,
                        "gongju_baekje_love",
                        0.8
                );

        when(filmRollRepository.existsByUserIdAndStatusIn(
                1L,
                FilmRollStatus.incompleteStatuses()
        )).thenReturn(false);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(regionRepository.findById(10L))
                .thenReturn(Optional.of(region));

        when(filterPresetProvider.getByFilterId(
                "gongju_baekje_love"
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
        assertThat(response.regionId()).isEqualTo(10L);
        assertThat(response.status())
                .isEqualTo(FilmRollStatus.CAPTURING.name());
        assertThat(response.totalPhotoCount()).isZero();

        verify(filterPresetProvider)
                .getByFilterId("gongju_baekje_love");
    }

    @Test
    @DisplayName("활성 필름 롤이 있으면 새 필름 롤을 생성하지 않는다")
    void rejectDuplicateActiveFilmRoll() {
        when(filmRollRepository.existsByUserIdAndStatusIn(
                1L,
                FilmRollStatus.incompleteStatuses()
        )).thenReturn(true);

        FilmRollCreateRequest request =
                new FilmRollCreateRequest(
                        10L,
                        "gongju_baekje_love",
                        0.8
                );

        assertThatThrownBy(() ->
                service.createFilmRoll(
                        1L,
                        request
                )
        ).isInstanceOf(ActiveFilmRollExistsException.class);

        verify(filmRollRepository).existsByUserIdAndStatusIn(
                1L,
                List.of(
                        FilmRollStatus.CAPTURING,
                        FilmRollStatus.READY,
                        FilmRollStatus.QUEUED,
                        FilmRollStatus.PROCESSING,
                        FilmRollStatus.FAILED
                )
        );
        verifyNoInteractions(
                userRepository,
                regionRepository,
                filterPresetProvider
        );
    }

    @Test
    @DisplayName("모든 사진 업로드가 끝나면 READY 상태로 전환한다")
    void markReady() {
        FilmRoll filmRoll = FilmRoll.create(
                user,
                region,
                "gongju_baekje_love",
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

        when(photoRepository.countByFilmRollId(100L))
                .thenReturn(1L);

        when(photoRepository.existsByFilmRollIdAndStatusNot(
                100L,
                PhotoStatus.UPLOADED
        )).thenReturn(false);

        FilmRollResponse response =
                service.markReady(
                        1L,
                        100L
                );

        assertThat(response.status())
                .isEqualTo(FilmRollStatus.READY.name());
    }

    @Test
    @DisplayName("CAPTURING 필름 롤은 업로드를 검증하고 현상 요청 준비 상태로 전환한다")
    void prepareDevelopmentFromCapturing() {
        FilmRoll filmRoll = createFilmRollWithOnePhoto();

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));
        stubCompletedPhotoUpload();

        PreparedFilmRollDevelopment preparation =
                service.prepareDevelopment(1L, 100L);

        assertThat(preparation.renderRequestRequired()).isTrue();
        assertThat(preparation.status())
                .isEqualTo(FilmRollStatus.READY.name());
        assertThat(filmRoll.getStatus())
                .isEqualTo(FilmRollStatus.READY);
    }

    @Test
    @DisplayName("FAILED 필름 롤은 같은 사진으로 재시도할 수 있도록 READY로 복구한다")
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
        stubCompletedPhotoUpload();

        PreparedFilmRollDevelopment preparation =
                service.prepareDevelopment(1L, 100L);

        assertThat(preparation.renderRequestRequired()).isTrue();
        assertThat(preparation.status())
                .isEqualTo(FilmRollStatus.READY.name());
        assertThat(filmRoll.getStatus())
                .isEqualTo(FilmRollStatus.READY);
        assertThat(filmRoll.getErrorCode()).isNull();
        assertThat(filmRoll.getErrorMessage()).isNull();
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
                "gongju_baekje_love",
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

    private void stubCompletedPhotoUpload() {
        when(photoRepository.countByFilmRollId(100L))
                .thenReturn(1L);
        when(photoRepository.existsByFilmRollIdAndStatusNot(
                100L,
                PhotoStatus.UPLOADED
        )).thenReturn(false);
    }

}
