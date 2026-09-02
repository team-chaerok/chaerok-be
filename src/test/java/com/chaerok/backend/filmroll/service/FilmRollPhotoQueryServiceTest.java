package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollPhotoListResponse;
import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.exception.FilmRollErrorCode;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.repository.PhotoRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollPhotoQueryServiceTest {

    @Mock
    private FilmRollRepository filmRollRepository;

    @Mock
    private PhotoRepository photoRepository;

    private FilmRollPhotoQueryService service;
    private User user;
    private Region region;

    @BeforeEach
    void setUp() {
        service = new FilmRollPhotoQueryService(
                filmRollRepository,
                photoRepository
        );
        user = mock(User.class);
        region = mock(Region.class);
    }

    @Test
    @DisplayName("사진을 sequence 오름차순으로 조회하고 내부 객체 키를 노출하지 않는다")
    void returnsPhotoMetadataInSequenceOrder() {
        FilmRoll filmRoll = filmRollWithPhotoCount(2);
        Photo first = uploadedPhoto(
                filmRoll,
                201L,
                1
        );
        Photo second = uploadedPhoto(
                filmRoll,
                202L,
                2
        );
        second.fail(
                "FILTER_FAILED",
                "필터 처리에 실패했습니다."
        );

        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(filmRoll));
        when(photoRepository
                .findAllByFilmRollIdOrderBySequenceAsc(100L))
                .thenReturn(List.of(first, second));

        FilmRollPhotoListResponse response =
                service.getPhotos(1L, 100L);

        assertThat(response.filmRollId()).isEqualTo(100L);
        assertThat(response.filmRollStatus())
                .isEqualTo("CAPTURING");
        assertThat(response.totalPhotoCount()).isEqualTo(2);
        assertThat(response.photos())
                .extracting(
                        FilmRollPhotoListResponse
                                .PhotoResponse::photoId
                )
                .containsExactly(201L, 202L);

        FilmRollPhotoListResponse.PhotoResponse firstResponse =
                response.photos().get(0);
        assertThat(firstResponse.sequence()).isEqualTo(1);
        assertThat(firstResponse.status()).isEqualTo("UPLOADED");
        assertThat(firstResponse.failure()).isNull();

        FilmRollPhotoListResponse.PhotoResponse secondResponse =
                response.photos().get(1);
        assertThat(secondResponse.status()).isEqualTo("FAILED");
        assertThat(secondResponse.failure())
                .isEqualTo(
                        new FilmRollPhotoListResponse.FailureResponse(
                                "FILTER_FAILED",
                                "필터 처리에 실패했습니다."
                        )
                );
    }

    @Test
    @DisplayName("다른 사용자의 필름 롤 사진은 조회할 수 없다")
    void rejectsNotOwnedFilmRoll() {
        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPhotos(1L, 100L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(FilmRollErrorCode.FILM_ROLL_NOT_FOUND)
                );

        verify(photoRepository, never())
                .findAllByFilmRollIdOrderBySequenceAsc(100L);
    }

    @Test
    @DisplayName("업로드 완료 전 UPLOADING 사진도 목록에서 조회할 수 있다")
    void returnsUploadingPhotoBeforeCompletion() {
        FilmRoll filmRoll = filmRollWithPhotoCount(0);
        Photo uploadingPhoto = Photo.create(
                filmRoll,
                1,
                "users/1/rolls/100/original/1.jpg",
                LocalDateTime.of(2026, 8, 5, 18, 1)
        );
        ReflectionTestUtils.setField(
                uploadingPhoto,
                "id",
                201L
        );

        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(filmRoll));
        when(photoRepository
                .findAllByFilmRollIdOrderBySequenceAsc(100L))
                .thenReturn(List.of(uploadingPhoto));

        FilmRollPhotoListResponse response =
                service.getPhotos(1L, 100L);

        assertThat(response.totalPhotoCount()).isZero();
        assertThat(response.photos()).hasSize(1);
        assertThat(response.photos().get(0).photoId())
                .isEqualTo(201L);
        assertThat(response.photos().get(0).status())
                .isEqualTo("UPLOADING");
        assertThat(response.photos().get(0).sequence())
                .isEqualTo(1);
    }

    private FilmRoll filmRollWithPhotoCount(int photoCount) {
        FilmRoll filmRoll = FilmRoll.create(
                user,
                region,
                "gongju",
                0.8,
                1
        );
        ReflectionTestUtils.setField(filmRoll, "id", 100L);

        for (int index = 0; index < photoCount; index++) {
            filmRoll.increasePhotoCount();
        }
        return filmRoll;
    }

    private Photo uploadedPhoto(
            FilmRoll filmRoll,
            Long photoId,
            int sequence
    ) {
        Photo photo = Photo.create(
                filmRoll,
                sequence,
                "users/1/rolls/100/original/"
                        + sequence
                        + ".jpg",
                LocalDateTime.of(2026, 8, 5, 18, sequence)
        );
        ReflectionTestUtils.setField(photo, "id", photoId);
        photo.markUploaded(
                LocalDateTime.of(2026, 8, 5, 18, 20)
        );
        return photo;
    }
}
