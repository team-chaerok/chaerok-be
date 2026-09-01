package com.chaerok.backend.filmroll.service;

import com.chaerok.backend.filmroll.dto.FilmRollResultResponse;
import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollErrorCode;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.aws.PresignedDownload;
import com.chaerok.backend.global.aws.S3ObjectStorage;
import com.chaerok.backend.global.exception.BusinessException;
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
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
@ConditionalOnProperty(
        prefix = "aws.s3",
        name = "bucket"
)
public class FilmRollResultService {

    private static final ZoneId APPLICATION_ZONE_ID =
            ZoneId.of("Asia/Seoul");

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
                .orElseThrow(() ->
                        new BusinessException(
                                FilmRollErrorCode.FILM_ROLL_NOT_FOUND
                        )
                );

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
            throw new BusinessException(
                    FilmRollErrorCode.RESULT_EXPIRATION_MISSING
            );
        }

        LocalDateTime now = LocalDateTime.ofInstant(
                clock.instant(),
                APPLICATION_ZONE_ID
        );
        return expiresAt.isAfter(now)
                ? FilmRollStatus.COMPLETED
                : FilmRollStatus.EXPIRED;
    }

    private FilmRollResultResponse completedResult(
            FilmRoll filmRoll
    ) {
        requireText(
                filmRoll.getZipObjectKey(),
                FilmRollErrorCode.ZIP_RESULT_PATH_MISSING
        );
        requireText(
                filmRoll.getReelObjectKey(),
                FilmRollErrorCode.REEL_RESULT_PATH_MISSING
        );

        List<Photo> photos = photoRepository
                .findAllByFilmRollIdOrderBySequenceAsc(
                        filmRoll.getId()
                );
        validateCompletedPhotos(filmRoll, photos);

        RenderJob renderJob = renderJobRepository
                .findFirstByFilmRollIdOrderByCreatedAtDesc(
                        filmRoll.getId()
                )
                .orElseThrow(() ->
                        new BusinessException(
                                FilmRollErrorCode.COMPLETED_RENDER_JOB_NOT_FOUND
                        )
                );
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
            throw new BusinessException(
                    FilmRollErrorCode.INVALID_RESULT_FILE_SIZE
            );
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

        requireText(
                errorCode,
                FilmRollErrorCode.RENDER_FAILURE_CODE_MISSING
        );
        requireText(
                errorMessage,
                FilmRollErrorCode.RENDER_FAILURE_MESSAGE_MISSING
        );

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
            throw new BusinessException(
                    FilmRollErrorCode.COMPLETED_PHOTO_COUNT_MISMATCH
            );
        }

        for (Photo photo : photos) {
            if (photo.getStatus() != PhotoStatus.COMPLETED) {
                throw new BusinessException(
                        FilmRollErrorCode.INCOMPLETE_PHOTO_IN_RESULT
                );
            }
            requireText(
                    photo.getFilteredObjectKey(),
                    FilmRollErrorCode.FILTERED_PHOTO_RESULT_PATH_MISSING
            );
        }
    }

    private void validateCompletedRenderJob(
            FilmRoll filmRoll,
            RenderJob renderJob
    ) {
        if (renderJob.getStatus() != RenderJobStatus.COMPLETED) {
            throw new BusinessException(
                    FilmRollErrorCode.RENDER_JOB_NOT_COMPLETED
            );
        }

        if (!filmRoll.getZipObjectKey().equals(
                renderJob.getZipObjectKey()
        ) || !filmRoll.getReelObjectKey().equals(
                renderJob.getReelObjectKey()
        )) {
            throw new BusinessException(
                    FilmRollErrorCode.RESULT_PATH_MISMATCH
            );
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void requireText(
            String value,
            FilmRollErrorCode errorCode
    ) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(errorCode);
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
