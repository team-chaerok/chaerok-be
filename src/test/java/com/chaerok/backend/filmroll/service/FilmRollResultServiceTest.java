package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollResultResponse;
import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollNotFoundException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.aws.PresignedDownload;
import com.chaerok.backend.global.aws.S3ObjectStorage;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.repository.PhotoRepository;
import com.chaerok.backend.region.entity.Region;
import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.repository.RenderJobRepository;
import com.chaerok.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmRollResultServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-05T09:30:00Z");
    private static final LocalDateTime COMPLETED_AT =
            LocalDateTime.of(2026, 8, 5, 9, 0);
    private static final Instant URL_EXPIRES_AT =
            Instant.parse("2026-08-05T09:40:00Z");

    @Mock
    private FilmRollRepository filmRollRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private RenderJobRepository renderJobRepository;

    @Mock
    private S3ObjectStorage objectStorage;

    private FilmRollResultService service;
    private User user;
    private Region region;

    @BeforeEach
    void setUp() {
        service = new FilmRollResultService(
                filmRollRepository,
                photoRepository,
                renderJobRepository,
                objectStorage,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        user = org.mockito.Mockito.mock(User.class);
        region = org.mockito.Mockito.mock(Region.class);
    }

    @Test
    @DisplayName("완료된 현상 결과에 필터 사진 ZIP 릴스 다운로드 URL을 반환한다")
    void returnsCompletedResultWithDownloadUrls() {
        FilmRoll filmRoll = completedFilmRoll(COMPLETED_AT);
        Photo first = completedPhoto(
                filmRoll,
                201L,
                1,
                "filtered/001.jpg"
        );
        Photo second = completedPhoto(
                filmRoll,
                202L,
                2,
                "filtered/002.jpg"
        );
        RenderJob renderJob = completedRenderJob(filmRoll);

        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(filmRoll));
        when(photoRepository
                .findAllByFilmRollIdOrderBySequenceAsc(100L))
                .thenReturn(List.of(first, second));
        when(renderJobRepository
                .findFirstByFilmRollIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(renderJob));
        when(objectStorage.createPresignedDownload(
                "filtered/001.jpg"
        )).thenReturn(download(
                "filtered/001.jpg",
                "https://download.example/001"
        ));
        when(objectStorage.createPresignedDownload(
                "filtered/002.jpg"
        )).thenReturn(download(
                "filtered/002.jpg",
                "https://download.example/002"
        ));
        when(objectStorage.createPresignedDownload("export/result.zip"))
                .thenReturn(download(
                        "export/result.zip",
                        "https://download.example/result.zip"
                ));
        when(objectStorage.createPresignedDownload("export/result.mp4"))
                .thenReturn(download(
                        "export/result.mp4",
                        "https://download.example/result.mp4"
                ));

        FilmRollResultResponse result =
                service.getResult(1L, 100L);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.totalPhotoCount()).isEqualTo(2);
        assertThat(result.processedPhotoCount()).isEqualTo(2);
        assertThat(result.filteredPhotos())
                .extracting(
                        FilmRollResultResponse.FilteredPhotoResponse
                                ::downloadUrl
                )
                .containsExactly(
                        "https://download.example/001",
                        "https://download.example/002"
                );
        assertThat(result.zip().downloadUrl())
                .isEqualTo("https://download.example/result.zip");
        assertThat(result.zip().fileSize()).isEqualTo(1200L);
        assertThat(result.reel().downloadUrl())
                .isEqualTo("https://download.example/result.mp4");
        assertThat(result.reel().fileSize()).isEqualTo(3400L);
        assertThat(result.failure()).isNull();
    }

    @Test
    @DisplayName("현상 진행 중에는 S3 URL을 생성하지 않고 상태만 반환한다")
    void returnsQueuedStatusWithoutDownloads() {
        FilmRoll filmRoll = queuedFilmRoll();
        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(filmRoll));

        FilmRollResultResponse result =
                service.getResult(1L, 100L);

        assertThat(result.status()).isEqualTo("QUEUED");
        assertThat(result.filteredPhotos()).isEmpty();
        assertThat(result.zip()).isNull();
        assertThat(result.reel()).isNull();
        assertThat(result.failure()).isNull();
        verifyNoInteractions(
                photoRepository,
                renderJobRepository,
                objectStorage
        );
    }

    @Test
    @DisplayName("현상 실패 시 실패 정보만 반환하고 S3 URL을 생성하지 않는다")
    void returnsFailureWithoutDownloads() {
        FilmRoll filmRoll = queuedFilmRoll();
        filmRoll.failFromResult(
                "MEDIA_GENERATION_FAILED",
                "릴스 생성에 실패했습니다."
        );
        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(filmRoll));

        FilmRollResultResponse result =
                service.getResult(1L, 100L);

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.failure())
                .isEqualTo(
                        new FilmRollResultResponse.FailureResponse(
                                "MEDIA_GENERATION_FAILED",
                                "릴스 생성에 실패했습니다."
                        )
                );
        assertThat(result.filteredPhotos()).isEmpty();
        assertThat(result.zip()).isNull();
        assertThat(result.reel()).isNull();
        verifyNoInteractions(
                photoRepository,
                renderJobRepository,
                objectStorage
        );
    }

    @Test
    @DisplayName("보관 시간이 지난 완료 결과는 EXPIRED로 조회하고 URL을 숨긴다")
    void returnsExpiredStatusWithoutDownloads() {
        FilmRoll filmRoll = completedFilmRoll(
                LocalDateTime.of(2026, 8, 3, 9, 0)
        );
        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(filmRoll));

        FilmRollResultResponse result =
                service.getResult(1L, 100L);

        assertThat(result.status()).isEqualTo("EXPIRED");
        assertThat(result.filteredPhotos()).isEmpty();
        assertThat(result.zip()).isNull();
        assertThat(result.reel()).isNull();
        verifyNoInteractions(
                photoRepository,
                renderJobRepository,
                objectStorage
        );
    }

    @Test
    @DisplayName("서버 Clock이 UTC여도 한국 시간 기준으로 결과 만료를 판단한다")
    void determinesExpiryUsingKoreanTimeWithUtcClock() {
        Clock utcClock = Clock.fixed(
                Instant.parse("2026-08-07T11:07:00Z"),
                ZoneOffset.UTC
        );
        FilmRollResultService utcClockService =
                new FilmRollResultService(
                        filmRollRepository,
                        photoRepository,
                        renderJobRepository,
                        objectStorage,
                        utcClock
                );
        FilmRoll filmRoll = completedFilmRoll(
                LocalDateTime.of(2026, 8, 5, 20, 6)
        );

        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(filmRoll));

        FilmRollResultResponse result =
                utcClockService.getResult(1L, 100L);

        assertThat(result.status()).isEqualTo("EXPIRED");
        assertThat(result.expiresAt())
                .isEqualTo(
                        LocalDateTime.of(2026, 8, 7, 20, 6)
                );
        assertThat(result.filteredPhotos()).isEmpty();
        assertThat(result.zip()).isNull();
        assertThat(result.reel()).isNull();
        verifyNoInteractions(
                photoRepository,
                renderJobRepository,
                objectStorage
        );
    }

    @Test
    @DisplayName("오류 사유가 있는 EXPIRED 결과에는 만료 사유를 반환한다")
    void returnsExpiredFailureWhenStored() {
        FilmRoll filmRoll = queuedFilmRoll();
        ReflectionTestUtils.setField(
                filmRoll,
                "status",
                FilmRollStatus.EXPIRED
        );
        ReflectionTestUtils.setField(
                filmRoll,
                "errorCode",
                "SOURCE_OBJECT_EXPIRED"
        );
        ReflectionTestUtils.setField(
                filmRoll,
                "errorMessage",
                "S3 입력 사진이 만료되었습니다."
        );
        ReflectionTestUtils.setField(
                filmRoll,
                "expiresAt",
                LocalDateTime.of(2026, 8, 5, 9, 20)
        );

        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(filmRoll));

        FilmRollResultResponse result =
                service.getResult(1L, 100L);

        assertThat(result.status()).isEqualTo("EXPIRED");
        assertThat(result.failure())
                .isEqualTo(
                        new FilmRollResultResponse.FailureResponse(
                                "SOURCE_OBJECT_EXPIRED",
                                "S3 입력 사진이 만료되었습니다."
                        )
                );
        verifyNoInteractions(
                photoRepository,
                renderJobRepository,
                objectStorage
        );
    }

    @Test
    @DisplayName("다른 사용자의 필름 롤 결과는 조회할 수 없다")
    void rejectsNotOwnedFilmRoll() {
        when(filmRollRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getResult(1L, 100L))
                .isInstanceOf(FilmRollNotFoundException.class);

        verify(photoRepository, never())
                .findAllByFilmRollIdOrderBySequenceAsc(100L);
    }

    private FilmRoll queuedFilmRoll() {
        FilmRoll filmRoll = baseFilmRoll();
        filmRoll.increasePhotoCount();
        filmRoll.increasePhotoCount();
        filmRoll.markReady();
        filmRoll.markQueued(
                LocalDateTime.of(2026, 8, 5, 8, 50)
        );
        return filmRoll;
    }

    private FilmRoll completedFilmRoll(
            LocalDateTime completedAt
    ) {
        FilmRoll filmRoll = queuedFilmRoll();
        filmRoll.completeFromResult(
                "export/result.zip",
                "export/result.mp4",
                completedAt
        );
        return filmRoll;
    }

    private FilmRoll baseFilmRoll() {
        FilmRoll filmRoll = FilmRoll.create(
                user,
                region,
                "gongju_baekje_love",
                0.8,
                1
        );
        ReflectionTestUtils.setField(filmRoll, "id", 100L);
        return filmRoll;
    }

    private Photo completedPhoto(
            FilmRoll filmRoll,
            Long photoId,
            int sequence,
            String filteredObjectKey
    ) {
        Photo photo = Photo.create(
                filmRoll,
                sequence,
                "original/" + sequence + ".jpg",
                false,
                null,
                LocalDateTime.of(2026, 8, 5, 8, sequence)
        );
        ReflectionTestUtils.setField(photo, "id", photoId);
        photo.markUploaded(
                LocalDateTime.of(2026, 8, 5, 8, 20)
        );
        photo.completeFromResult(
                filteredObjectKey,
                COMPLETED_AT
        );
        return photo;
    }

    private RenderJob completedRenderJob(FilmRoll filmRoll) {
        RenderJob renderJob = RenderJob.create(filmRoll);
        renderJob.markQueued(
                LocalDateTime.of(2026, 8, 5, 8, 50),
                "request-message-1"
        );
        renderJob.completeFromResult(
                1,
                "request-message-1",
                "result-message-1",
                "chaerok-media-dev-7f3k2m",
                "export/result.zip",
                1200L,
                "export/result.mp4",
                3400L,
                "manifest.json",
                COMPLETED_AT
        );
        return renderJob;
    }

    private PresignedDownload download(
            String objectKey,
            String url
    ) {
        return new PresignedDownload(
                objectKey,
                url,
                URL_EXPIRES_AT
        );
    }
}
