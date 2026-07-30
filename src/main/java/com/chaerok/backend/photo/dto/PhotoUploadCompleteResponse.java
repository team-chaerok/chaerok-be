package com.chaerok.backend.photo.dto;

import com.chaerok.backend.filmroll.entity.FilmRoll;
import com.chaerok.backend.photo.entity.Photo;

import java.time.LocalDateTime;

public record PhotoUploadCompleteResponse(
        Long photoId,
        Long filmRollId,
        int sequence,
        String status,
        int totalPhotoCount,
        LocalDateTime uploadCompletedAt
) {

    public static PhotoUploadCompleteResponse of(
            Photo photo,
            FilmRoll filmRoll
    ) {
        return new PhotoUploadCompleteResponse(
                photo.getId(),
                filmRoll.getId(),
                photo.getSequence(),
                photo.getStatus().name(),
                filmRoll.getTotalPhotoCount(),
                photo.getUploadCompletedAt()
        );
    }
}
