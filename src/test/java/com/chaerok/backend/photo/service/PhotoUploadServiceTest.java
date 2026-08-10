package com.chaerok.backend.photo.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.exception.FilmRollConflictException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.aws.PresignedUpload;
import com.chaerok.backend.global.aws.S3ObjectKeyGenerator;
import com.chaerok.backend.global.aws.S3ObjectStorage;
import com.chaerok.backend.global.aws.StoredObjectMetadata;
import com.chaerok.backend.photo.dto.PhotoUploadCompleteResponse;
import com.chaerok.backend.photo.dto.PhotoUploadUrlRequest;
import com.chaerok.backend.photo.dto.PhotoUploadUrlResponse;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.entity.PhotoStatus;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoUploadServiceTest {

    @Mock
    private FilmRollRepository filmRollRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private S3ObjectStorage objectStorage;

    private S3ObjectKeyGenerator objectKeyGenerator;
    private PhotoUploadService service;
    private FilmRoll filmRoll;

    @BeforeEach
    void setUp() {
        objectKeyGenerator = new S3ObjectKeyGenerator();

        service = new PhotoUploadService(
                filmRollRepository,
                photoRepository,
                objectStorage,
                objectKeyGenerator
        );

        User user = mock(User.class);
        Region region = mock(Region.class);

        filmRoll = FilmRoll.create(
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
    }

    @Test
    @DisplayName("Photo를 생성하고 Presigned PUT URL을 발급한다")
    void createUploadUrl() {
        LocalDateTime takenAt =
                LocalDateTime.of(
                        2026,
                        7,
                        30,
                        19,
                        0
                );

        PhotoUploadUrlRequest request =
                new PhotoUploadUrlRequest(
                        1,
                        "image/jpeg",
                        1024L,
                        takenAt
                );

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));

        when(photoRepository.findByFilmRollIdAndSequence(
                100L,
                1
        )).thenReturn(Optional.empty());

        when(photoRepository.countByFilmRollId(100L))
                .thenReturn(0L);

        when(photoRepository.saveAndFlush(
                any(Photo.class)
        )).thenAnswer(invocation -> {
            Photo photo = invocation.getArgument(0);
            ReflectionTestUtils.setField(
                    photo,
                    "id",
                    200L
            );
            return photo;
        });

        when(objectStorage.createPresignedUpload(
                any(),
                any(),
                anyLong()
        )).thenAnswer(invocation ->
                new PresignedUpload(
                        invocation.getArgument(0),
                        "https://example.com/upload",
                        Instant.parse(
                                "2026-07-30T10:10:00Z"
                        ),
                        Map.of(
                                "content-type",
                                List.of("image/jpeg")
                        )
                )
        );

        PhotoUploadUrlResponse response =
                service.createUploadUrl(
                        1L,
                        100L,
                        request
                );

        assertThat(response.photoId()).isEqualTo(200L);
        assertThat(response.filmRollId()).isEqualTo(100L);
        assertThat(response.sequence()).isEqualTo(1);
        assertThat(response.objectKey())
                .startsWith(
                        "users/1/rolls/100/original/001-"
                );
        assertThat(response.uploadUrl())
                .isEqualTo("https://example.com/upload");
    }


    @Test
    @DisplayName("지역 이탈 확정 후에는 새 사진 슬롯을 만들 수 없다")
    void rejectsNewPhotoAfterExitConfirmation() {
        PhotoUploadUrlRequest request =
                new PhotoUploadUrlRequest(
                        1,
                        "image/jpeg",
                        1024L,
                        LocalDateTime.now()
                );

        filmRoll.confirmExit(LocalDateTime.now());

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));
        when(photoRepository.findByFilmRollIdAndSequence(100L, 1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.createUploadUrl(1L, 100L, request)
        )
                .isInstanceOf(FilmRollConflictException.class)
                .hasMessageContaining("지역 이탈 확정 후");

        verify(photoRepository, never()).saveAndFlush(any(Photo.class));
    }

    @Test
    @DisplayName("S3 객체를 검증한 뒤 사진을 UPLOADED로 전환한다")
    void completeUpload() {
        Photo photo = Photo.create(
                filmRoll,
                1,
                "users/1/rolls/100/original/001-test.jpg",
                LocalDateTime.of(
                        2026,
                        7,
                        30,
                        19,
                        0
                )
        );

        ReflectionTestUtils.setField(
                photo,
                "id",
                200L
        );

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));

        when(photoRepository.findByIdAndFilmRollId(
                200L,
                100L
        )).thenReturn(Optional.of(photo));

        when(objectStorage.getMetadata(
                photo.getOriginalObjectKey()
        )).thenReturn(
                new StoredObjectMetadata(
                        photo.getOriginalObjectKey(),
                        1024L,
                        "image/jpeg",
                        "\"etag\"",
                        Instant.parse(
                                "2026-07-30T10:00:00Z"
                        )
                )
        );

        when(objectStorage.getMaxUploadBytes())
                .thenReturn(5L * 1024 * 1024);

        PhotoUploadCompleteResponse response =
                service.completeUpload(
                        1L,
                        100L,
                        200L
                );

        assertThat(response.status())
                .isEqualTo(PhotoStatus.UPLOADED.name());

        assertThat(response.totalPhotoCount())
                .isEqualTo(1);

        assertThat(photo.getUploadCompletedAt())
                .isNotNull();
    }

    @Test
    @DisplayName("완료 요청이 반복돼도 사진 수를 중복 증가시키지 않는다")
    void completeUploadIdempotently() {
        Photo photo = Photo.create(
                filmRoll,
                1,
                "users/1/rolls/100/original/001-test.jpg",
                LocalDateTime.of(
                        2026,
                        7,
                        30,
                        19,
                        0
                )
        );

        ReflectionTestUtils.setField(
                photo,
                "id",
                200L
        );

        photo.markUploaded(
                LocalDateTime.of(
                        2026,
                        7,
                        30,
                        19,
                        1
                )
        );

        filmRoll.increasePhotoCount();

        when(filmRollRepository.findByIdAndUserIdForUpdate(
                100L,
                1L
        )).thenReturn(Optional.of(filmRoll));

        when(photoRepository.findByIdAndFilmRollId(
                200L,
                100L
        )).thenReturn(Optional.of(photo));

        PhotoUploadCompleteResponse response =
                service.completeUpload(
                        1L,
                        100L,
                        200L
                );

        assertThat(response.totalPhotoCount())
                .isEqualTo(1);

        verify(objectStorage, never())
                .getMetadata(any());
    }
}
