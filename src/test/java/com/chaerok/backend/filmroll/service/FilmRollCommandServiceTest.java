package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollCreateRequest;
import com.chaerok.backend.filmroll.dto.FilmRollResponse;
import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
                any(),
                anyList()
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
                any(),
                anyList()
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
        ).isInstanceOf(FilmRollConflictException.class);
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
}
