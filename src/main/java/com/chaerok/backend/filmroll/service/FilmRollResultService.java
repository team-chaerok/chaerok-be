package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollResultResponse;
import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollConflictException;
import com.chaerok.backend.filmroll.exception.FilmRollNotFoundException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.aws.PresignedDownload;
import com.chaerok.backend.global.aws.S3ObjectStorage;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.entity.PhotoStatus;
import com.chaerok.backend.photo.repository.PhotoRepository;
import com.chaerok.backend.render.entity.RenderJob;
import com.chaerok.backend.render.entity.RenderJobStatus;
import com.chaerok.backend.render.repository.RenderJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@ConditionalOnProperty(
        prefix = "aws.s3",
        name = "bucket"
)
public class FilmRollResultService {

    private final FilmRollRepository filmRollRepository;
    private final PhotoRepository photoRepository;
    private final RenderJobRepository renderJobRepository;
    private final S3ObjectStorage objectStorage;
    private final Clock clock;

    @Autowired
    public FilmRollResultService(
            FilmRollRepository filmRollRepository,
            PhotoRepository photoRepository,
            RenderJobRepository renderJobRepository,
            S3ObjectStorage objectStorage
    ) {
        this(
                filmRollRepository,
                photoRepository,
                renderJobRepository,
                objectStorage,
                Clock.systemUTC()
        );
    }

    FilmRollResultService(
            FilmRollRepository filmRollRepository,
            PhotoRepository photoRepository,
            RenderJobRepository renderJobRepository,
            S3ObjectStorage objectStorage,
            Clock clock
    ) {
        this.filmRollRepository = require(
                filmRollRepository,
                "filmRollRepository"
        );
        this.photoRepository = require(
                photoRepository,
                "photoRepository"
        );
        this.renderJobRepository = require(
                renderJobRepository,
                "renderJobRepository"
        );
        this.objectStorage = require(
                objectStorage,
                "objectStorage"
        );
        this.clock = require(clock, "clock");
    }

    public FilmRollResultResponse getResult(
            Long userId,
            Long filmRollId
    ) {
        FilmRoll filmRoll = filmRollRepository
                .findByIdAndUserId(filmRollId, userId)
                .orElseThrow(FilmRollNotFoundException::new);

        FilmRollStatus responseStatus = responseStatus(filmRoll);

        if (responseStatus == FilmRollStatus.COMPLETED) {
            return completedResult(filmRoll);
        }

        return nonCompletedResult(filmRoll, responseStatus);
    }

    private FilmRollStatus responseStatus(FilmRoll filmRoll) {
        if (filmRoll.getStatus() != FilmRollStatus.COMPLETED) {
            return filmRoll.getStatus();
        }

        LocalDateTime expiresAt = filmRoll.getExpiresAt();
        if (expiresAt == null) {
            throw conflict("완료된 필름 롤의 만료 시각이 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        return expiresAt.isAfter(now)
                ? FilmRollStatus.COMPLETED
                : FilmRollStatus.EXPIRED;
    }

    private FilmRollResultResponse completedResult(
            FilmRoll filmRoll
    ) {
        requireText(filmRoll.getZipObjectKey(), "ZIP 결과 경로");
        requireText(filmRoll.getReelObjectKey(), "릴스 결과 경로");

        List<Photo> photos = photoRepository
                .findAllByFilmRollIdOrderBySequenceAsc(
                        filmRoll.getId()
                );
        validateCompletedPhotos(filmRoll, photos);

        RenderJob renderJob = renderJobRepository
                .findFirstByFilmRollIdOrderByCreatedAtDesc(
                        filmRoll.getId()
                )
                .orElseThrow(() -> conflict(
                        "완료된 필름 롤의 현상 작업을 찾을 수 없습니다."
                ));
        validateCompletedRenderJob(filmRoll, renderJob);

        List<FilmRollResultResponse.FilteredPhotoResponse>
                filteredPhotos = photos.stream()
                .map(this::filteredPhotoResponse)
                .toList();

        FilmRollResultResponse.DownloadResponse zip =
                downloadResponse(
                        filmRoll.getZipObjectKey(),
                        renderJob.getZipFileSize()
                );
        FilmRollResultResponse.DownloadResponse reel =
                downloadResponse(
                        filmRoll.getReelObjectKey(),
                        renderJob.getReelFileSize()
                );

        return new FilmRollResultResponse(
                filmRoll.getId(),
                FilmRollStatus.COMPLETED.name(),
                filmRoll.getTotalPhotoCount(),
                filmRoll.getProcessedPhotoCount(),
                filteredPhotos,
                zip,
                reel,
                filmRoll.getRequestedAt(),
                filmRoll.getCompletedAt(),
                filmRoll.getExpiresAt(),
                null
        );
    }

    private FilmRollResultResponse nonCompletedResult(
            FilmRoll filmRoll,
            FilmRollStatus responseStatus
    ) {
        FilmRollResultResponse.FailureResponse failure =
                switch (responseStatus) {
                    case FAILED, EXPIRED ->
                            failureResponseOrNull(filmRoll);
                    default -> null;
                };

        return new FilmRollResultResponse(
                filmRoll.getId(),
                responseStatus.name(),
                filmRoll.getTotalPhotoCount(),
                filmRoll.getProcessedPhotoCount(),
                List.of(),
                null,
                null,
                filmRoll.getRequestedAt(),
                filmRoll.getCompletedAt(),
                filmRoll.getExpiresAt(),
                failure
        );
    }

    private FilmRollResultResponse.FilteredPhotoResponse
    filteredPhotoResponse(Photo photo) {
        PresignedDownload download = objectStorage
                .createPresignedDownload(
                        photo.getFilteredObjectKey()
                );

        return new FilmRollResultResponse.FilteredPhotoResponse(
                photo.getId(),
                photo.getSequence(),
                download.downloadUrl(),
                download.expiresAt()
        );
    }

    private FilmRollResultResponse.DownloadResponse downloadResponse(
            String objectKey,
            Long fileSize
    ) {
        if (fileSize == null || fileSize < 0L) {
            throw conflict("현상 결과 파일 크기가 올바르지 않습니다.");
        }

        PresignedDownload download = objectStorage
                .createPresignedDownload(objectKey);

        return new FilmRollResultResponse.DownloadResponse(
                download.downloadUrl(),
                download.expiresAt(),
                fileSize
        );
    }

    private FilmRollResultResponse.FailureResponse
    failureResponseOrNull(FilmRoll filmRoll) {
        String errorCode = filmRoll.getErrorCode();
        String errorMessage = filmRoll.getErrorMessage();

        if (isBlank(errorCode) && isBlank(errorMessage)) {
            return null;
        }

        requireText(errorCode, "현상 실패 코드");
        requireText(errorMessage, "현상 실패 메시지");

        return new FilmRollResultResponse.FailureResponse(
                errorCode,
                errorMessage
        );
    }

    private void validateCompletedPhotos(
            FilmRoll filmRoll,
            List<Photo> photos
    ) {
        if (photos.size() != filmRoll.getTotalPhotoCount()) {
            throw conflict("완료된 사진 수가 필름 롤의 사진 수와 다릅니다.");
        }

        for (Photo photo : photos) {
            if (photo.getStatus() != PhotoStatus.COMPLETED) {
                throw conflict("완료되지 않은 사진이 현상 결과에 포함되어 있습니다.");
            }
            requireText(
                    photo.getFilteredObjectKey(),
                    "필터 사진 결과 경로"
            );
        }
    }

    private void validateCompletedRenderJob(
            FilmRoll filmRoll,
            RenderJob renderJob
    ) {
        if (renderJob.getStatus() != RenderJobStatus.COMPLETED) {
            throw conflict("최신 현상 작업이 완료 상태가 아닙니다.");
        }

        if (!filmRoll.getZipObjectKey().equals(
                renderJob.getZipObjectKey()
        ) || !filmRoll.getReelObjectKey().equals(
                renderJob.getReelObjectKey()
        )) {
            throw conflict("필름 롤과 현상 작업의 결과 경로가 일치하지 않습니다.");
        }
    }

    private static FilmRollConflictException conflict(
            String message
    ) {
        return new FilmRollConflictException(message);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw conflict(fieldName + "이(가) 없습니다.");
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(
                    name + " is required."
            );
        }
        return value;
    }
}
