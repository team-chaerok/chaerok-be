package com.chaerok.backend.filmroll.dto;

import com.chaerok.backend.photo.entity.Photo;

import java.time.LocalDateTime;
import java.util.List;

public record FilmRollPhotoListResponse(
        Long filmRollId,
        String filmRollStatus,
        int totalPhotoCount,
        List<PhotoResponse> photos
) {

    public FilmRollPhotoListResponse {
        photos = photos == null
                ? List.of()
                : List.copyOf(photos);
    }

    public static FilmRollPhotoListResponse of(
            Long filmRollId,
            String filmRollStatus,
            int totalPhotoCount,
            List<Photo> photos
    ) {
        return new FilmRollPhotoListResponse(
                filmRollId,
                filmRollStatus,
                totalPhotoCount,
                photos.stream()
                        .map(PhotoResponse::from)
                        .toList()
        );
    }

    public record PhotoResponse(
            Long photoId,
            int sequence,
            String status,
            boolean hasFace,
            String sceneType,
            LocalDateTime takenAt,
            LocalDateTime uploadCompletedAt,
            LocalDateTime processedAt,
            FailureResponse failure
    ) {

        private static PhotoResponse from(Photo photo) {
            return new PhotoResponse(
                    photo.getId(),
                    photo.getSequence(),
                    photo.getStatus().name(),
                    photo.isHasFace(),
                    photo.getSceneType() == null
                            ? null
                            : photo.getSceneType().name(),
                    photo.getTakenAt(),
                    photo.getUploadCompletedAt(),
                    photo.getProcessedAt(),
                    FailureResponse.from(photo)
            );
        }
    }

    public record FailureResponse(
            String code,
            String message
    ) {

        private static FailureResponse from(Photo photo) {
            if (photo.getErrorCode() == null
                    && photo.getErrorMessage() == null) {
                return null;
            }

            return new FailureResponse(
                    photo.getErrorCode(),
                    photo.getErrorMessage()
            );
        }
    }
}
