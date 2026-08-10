package com.chaerok.backend.photo.dto;

import com.chaerok.backend.global.aws.PresignedUpload;
import com.chaerok.backend.photo.entity.Photo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PhotoUploadUrlResponse(
        Long photoId,
        Long filmRollId,
        int sequence,
        String objectKey,
        String uploadUrl,
        Instant expiresAt,
        Map<String, List<String>> requiredHeaders
) {

    public static PhotoUploadUrlResponse of(
            Photo photo,
            PresignedUpload presignedUpload
    ) {
        return new PhotoUploadUrlResponse(
                photo.getId(),
                photo.getFilmRoll().getId(),
                photo.getSequence(),
                presignedUpload.objectKey(),
                presignedUpload.uploadUrl(),
                presignedUpload.expiresAt(),
                presignedUpload.requiredHeaders()
        );
    }
}
