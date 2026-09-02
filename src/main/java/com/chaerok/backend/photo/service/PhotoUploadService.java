package com.chaerok.backend.photo.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollErrorCode;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.aws.PresignedUpload;
import com.chaerok.backend.global.aws.S3ObjectKeyGenerator;
import com.chaerok.backend.global.aws.S3ObjectNotFoundException;
import com.chaerok.backend.global.aws.S3ObjectStorage;
import com.chaerok.backend.global.aws.StoredObjectMetadata;
import com.chaerok.backend.global.exception.BusinessException;
import com.chaerok.backend.photo.dto.PhotoUploadCompleteResponse;
import com.chaerok.backend.photo.dto.PhotoUploadUrlRequest;
import com.chaerok.backend.photo.dto.PhotoUploadUrlResponse;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.entity.PhotoStatus;
import com.chaerok.backend.photo.exception.PhotoErrorCode;
import com.chaerok.backend.photo.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(
        prefix = "aws.s3",
        name = "bucket"
)
public class PhotoUploadService {

    private static final String JPEG_CONTENT_TYPE = "image/jpeg";
    private static final String JPG_CONTENT_TYPE = "image/jpg";

    private final FilmRollRepository filmRollRepository;
    private final PhotoRepository photoRepository;
    private final S3ObjectStorage objectStorage;
    private final S3ObjectKeyGenerator objectKeyGenerator;

    @Transactional
    public PhotoUploadUrlResponse createUploadUrl(
            Long userId,
            Long filmRollId,
            PhotoUploadUrlRequest request
    ) {
        FilmRoll filmRoll =
                findOwnedFilmRollForUpdate(
                        userId,
                        filmRollId
                );

        requireCapturing(filmRoll);

        Optional<Photo> existingPhoto = photoRepository
                .findByFilmRollIdAndSequence(
                        filmRollId,
                        request.sequence()
                );

        if (filmRoll.isExitConfirmed() && existingPhoto.isEmpty()) {
            throw new BusinessException(
                    PhotoErrorCode.PHOTO_ADD_AFTER_EXIT_NOT_ALLOWED
            );
        }

        Photo photo = existingPhoto
                .map(this::validateReusableUpload)
                .orElseGet(() ->
                        createPhoto(
                                userId,
                                filmRoll,
                                request
                        )
                );

        PresignedUpload presignedUpload =
                objectStorage.createPresignedUpload(
                        photo.getOriginalObjectKey(),
                        normalizeContentType(
                                request.contentType()
                        ),
                        request.contentLength()
                );

        return PhotoUploadUrlResponse.of(
                photo,
                presignedUpload
        );
    }

    @Transactional
    public PhotoUploadCompleteResponse completeUpload(
            Long userId,
            Long filmRollId,
            Long photoId
    ) {
        FilmRoll filmRoll =
                findOwnedFilmRollForUpdate(
                        userId,
                        filmRollId
                );

        requireCapturing(filmRoll);

        Photo photo = photoRepository
                .findByIdAndFilmRollId(
                        photoId,
                        filmRollId
                )
                .orElseThrow(() ->
                        new BusinessException(
                                FilmRollErrorCode.PHOTO_NOT_FOUND
                        )
                );

        if (photo.getStatus() == PhotoStatus.UPLOADED) {
            return PhotoUploadCompleteResponse.of(
                    photo,
                    filmRoll
            );
        }

        if (photo.getStatus() != PhotoStatus.UPLOADING) {
            throw new BusinessException(
                    PhotoErrorCode.PHOTO_NOT_UPLOADING
            );
        }

        StoredObjectMetadata metadata =
                loadUploadedObjectMetadata(photo);

        validateUploadedObject(metadata);

        photo.markUploaded(LocalDateTime.now());
        filmRoll.increasePhotoCount();

        return PhotoUploadCompleteResponse.of(
                photo,
                filmRoll
        );
    }

    private Photo createPhoto(
            Long userId,
            FilmRoll filmRoll,
            PhotoUploadUrlRequest request
    ) {
        long photoSlotCount =
                photoRepository.countByFilmRollId(
                        filmRoll.getId()
                );

        if (photoSlotCount >= FilmRoll.MAX_PHOTO_COUNT) {
            throw new BusinessException(
                    PhotoErrorCode.PHOTO_LIMIT_EXCEEDED
            );
        }

        String objectKey =
                objectKeyGenerator.createOriginalPhotoKey(
                        userId,
                        filmRoll.getId(),
                        request.sequence()
                );

        Photo photo = Photo.create(
                filmRoll,
                request.sequence(),
                objectKey,
                request.takenAt()
        );

        return photoRepository.saveAndFlush(photo);
    }

    private Photo validateReusableUpload(Photo photo) {
        if (photo.getStatus() != PhotoStatus.UPLOADING) {
            throw new BusinessException(
                    PhotoErrorCode.PHOTO_SEQUENCE_ALREADY_IN_USE
            );
        }

        return photo;
    }

    private StoredObjectMetadata loadUploadedObjectMetadata(
            Photo photo
    ) {
        try {
            return objectStorage.getMetadata(
                    photo.getOriginalObjectKey()
            );
        } catch (S3ObjectNotFoundException exception) {
            throw new BusinessException(
                    PhotoErrorCode.UPLOADED_PHOTO_NOT_FOUND,
                    "S3에서 업로드된 사진 메타데이터를 찾지 못했습니다.",
                    exception
            );
        }
    }

    private void validateUploadedObject(
            StoredObjectMetadata metadata
    ) {
        if (metadata.contentLength() <= 0) {
            throw new BusinessException(
                    PhotoErrorCode.EMPTY_PHOTO_FILE
            );
        }

        if (metadata.contentLength()
                > objectStorage.getMaxUploadBytes()) {
            deleteInvalidObject(metadata.objectKey());

            throw new BusinessException(
                    PhotoErrorCode.PHOTO_FILE_TOO_LARGE
            );
        }

        String contentType = normalizeContentType(
                metadata.contentType()
        );

        if (!JPEG_CONTENT_TYPE.equals(contentType)
                && !JPG_CONTENT_TYPE.equals(contentType)) {
            deleteInvalidObject(metadata.objectKey());

            throw new BusinessException(
                    PhotoErrorCode.INVALID_PHOTO_CONTENT_TYPE
            );
        }
    }

    private void deleteInvalidObject(String objectKey) {
        objectStorage.delete(objectKey);
    }

    private FilmRoll findOwnedFilmRollForUpdate(
            Long userId,
            Long filmRollId
    ) {
        return filmRollRepository
                .findByIdAndUserIdForUpdate(
                        filmRollId,
                        userId
                )
                .orElseThrow(() ->
                        new BusinessException(
                                FilmRollErrorCode.FILM_ROLL_NOT_FOUND
                        )
                );
    }

    private void requireCapturing(FilmRoll filmRoll) {
        if (filmRoll.getStatus() != FilmRollStatus.CAPTURING) {
            throw new BusinessException(
                    PhotoErrorCode.PHOTO_UPLOAD_NOT_ALLOWED
            );
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }

        return contentType.trim()
                .toLowerCase(Locale.ROOT);
    }
}
