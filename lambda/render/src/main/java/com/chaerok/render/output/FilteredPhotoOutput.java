package com.chaerok.render.output;

public record FilteredPhotoOutput(
        Long photoId,
        int sequence,
        String objectKey,
        long fileSize
) {
}
