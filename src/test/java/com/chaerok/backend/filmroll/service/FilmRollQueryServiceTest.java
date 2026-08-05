package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollResponse;
import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollNotFoundException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollQueryServiceTest {

    @Mock
    private FilmRollRepository filmRollRepository;

    private FilmRollQueryService service;
    private User user;
    private Region region;

    @BeforeEach
    void setUp() {
        service = new FilmRollQueryService(filmRollRepository);
        user = mock(User.class);
        region = mock(Region.class);

        org.mockito.Mockito.lenient()
                .when(region.getId())
                .thenReturn(10L);
    }

    @Test
    @DisplayName("FAILED 상태도 현재 미완료 필름 롤로 조회한다")
    void findCurrentIncludesFailedFilmRoll() {
        FilmRoll filmRoll = failedFilmRoll();

        when(filmRollRepository
                .findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                        1L,
                        FilmRollStatus.incompleteStatuses()
                ))
                .thenReturn(Optional.of(filmRoll));

        Optional<FilmRollResponse> response =
                service.findCurrentFilmRoll(1L);

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().filmRollId()).isEqualTo(100L);
        assertThat(response.orElseThrow().status())
                .isEqualTo(FilmRollStatus.FAILED.name());
        assertThat(response.orElseThrow().failure())
                .isEqualTo(new FilmRollResponse.FailureResponse(
                        "MEDIA_GENERATION_FAILED",
                        "릴스 생성에 실패했습니다."
                ));

        verify(filmRollRepository)
                .findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                        1L,
                        FilmRollStatus.incompleteStatuses()
                );
    }

    @Test
    @DisplayName("미완료 필름 롤이 없으면 빈 결과를 반환한다")
    void findCurrentReturnsEmpty() {
        when(filmRollRepository
                .findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                        1L,
                        FilmRollStatus.incompleteStatuses()
                ))
                .thenReturn(Optional.empty());

        assertThat(service.findCurrentFilmRoll(1L)).isEmpty();
    }

    @Test
    @DisplayName("사용자가 소유한 필름 롤 상세를 조회한다")
    void getOwnedFilmRoll() {
        FilmRoll filmRoll = FilmRoll.create(
                user,
                region,
                "gongju_baekje_love",
                0.8,
                1
        );
        ReflectionTestUtils.setField(filmRoll, "id", 100L);

        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(filmRoll));

        FilmRollResponse response = service.getFilmRoll(1L, 100L);

        assertThat(response.filmRollId()).isEqualTo(100L);
        assertThat(response.regionId()).isEqualTo(10L);
        assertThat(response.status())
                .isEqualTo(FilmRollStatus.CAPTURING.name());
        assertThat(response.maxPhotoCount())
                .isEqualTo(FilmRoll.MAX_PHOTO_COUNT);
    }

    @Test
    @DisplayName("다른 사용자의 필름 롤은 조회할 수 없다")
    void getFilmRollRejectsNotOwnedFilmRoll() {
        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFilmRoll(1L, 100L))
                .isInstanceOf(FilmRollNotFoundException.class);
    }

    private FilmRoll failedFilmRoll() {
        FilmRoll filmRoll = FilmRoll.create(
                user,
                region,
                "gongju_baekje_love",
                0.8,
                1
        );
        ReflectionTestUtils.setField(filmRoll, "id", 100L);
        ReflectionTestUtils.setField(
                filmRoll,
                "createdAt",
                LocalDateTime.of(2026, 8, 5, 16, 0)
        );
        ReflectionTestUtils.setField(
                filmRoll,
                "updatedAt",
                LocalDateTime.of(2026, 8, 5, 16, 10)
        );

        filmRoll.increasePhotoCount();
        filmRoll.markReady();
        filmRoll.failFromResult(
                "MEDIA_GENERATION_FAILED",
                "릴스 생성에 실패했습니다."
        );
        return filmRoll;
    }
}
