package com.chaerok.backend.photo.service;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.filmroll.entity.FilmRollStatus;
import com.chaerok.backend.filmroll.exception.FilmRollConflictException;
import com.chaerok.backend.filmroll.exception.FilmRollNotFoundException;
import com.chaerok.backend.filmroll.exception.PhotoNotFoundException;
import com.chaerok.backend.filmroll.exception.PhotoUploadValidationException;
import com.chaerok.backend.filmroll.repository.FilmRollRepository;
import com.chaerok.backend.global.aws.PresignedUpload;
import com.chaerok.backend.global.aws.S3ObjectKeyGenerator;
import com.chaerok.backend.global.aws.S3ObjectNotFoundException;
import com.chaerok.backend.global.aws.S3ObjectStorage;
import com.chaerok.backend.global.aws.StoredObjectMetadata;
import com.chaerok.backend.photo.dto.PhotoUploadCompleteResponse;
import com.chaerok.backend.photo.dto.PhotoUploadUrlRequest;
import com.chaerok.backend.photo.dto.PhotoUploadUrlResponse;
import com.chaerok.backend.photo.entity.Photo;
import com.chaerok.backend.photo.entity.PhotoStatus;
import com.chaerok.backend.photo.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

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

        Photo photo = photoRepository
                .findByFilmRollIdAndSequence(
                        filmRollId,
                        request.sequence()
                )
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
                .orElseThrow(PhotoNotFoundException::new);

        if (photo.getStatus() == PhotoStatus.UPLOADED) {
            return PhotoUploadCompleteResponse.of(
                    photo,
                    filmRoll
            );
        }

        if (photo.getStatus() != PhotoStatus.UPLOADING) {
            throw new FilmRollConflictException(
                    "업로드 대기 중인 사진만 업로드 완료 처리할 수 있습니다."
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
            throw new FilmRollConflictException(
                    "필름 롤에는 최대 24장까지만 업로드할 수 있습니다."
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
            throw new FilmRollConflictException(
                    "이미 사용 중인 사진 순서입니다."
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
            throw new PhotoUploadValidationException(
                    "S3에서 업로드된 사진을 찾을 수 없습니다."
            );
        }
    }

    private void validateUploadedObject(
            StoredObjectMetadata metadata
    ) {
        if (metadata.contentLength() <= 0) {
            throw new PhotoUploadValidationException(
                    "업로드된 사진 파일이 비어 있습니다."
            );
        }

        if (metadata.contentLength()
                > objectStorage.getMaxUploadBytes()) {
            deleteInvalidObject(metadata.objectKey());

            throw new PhotoUploadValidationException(
                    "업로드된 사진이 허용된 최대 크기를 초과했습니다."
            );
        }

        String contentType = normalizeContentType(
                metadata.contentType()
        );

        if (!JPEG_CONTENT_TYPE.equals(contentType)
                && !JPG_CONTENT_TYPE.equals(contentType)) {
            deleteInvalidObject(metadata.objectKey());

            throw new PhotoUploadValidationException(
                    "업로드된 파일은 JPEG 이미지가 아닙니다."
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
                .orElseThrow(FilmRollNotFoundException::new);
    }

    private void requireCapturing(FilmRoll filmRoll) {
        if (filmRoll.getStatus() != FilmRollStatus.CAPTURING) {
            throw new FilmRollConflictException(
                    "촬영 중인 필름 롤에서만 사진을 업로드할 수 있습니다."
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
