package com.chaerok.render.validation;

import com.chaerok.render.message.RenderQueueMessage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RenderMessageValidator {

    private static final int SUPPORTED_SCHEMA_VERSION = 2;
    private static final int MAX_PHOTO_COUNT = 24;

    public void validate(RenderQueueMessage message) {
        if (message == null) {
            throw new InvalidRenderMessageException(
                    "Render message is required."
            );
        }

        require(
                message.schemaVersion() == SUPPORTED_SCHEMA_VERSION,
                "Unsupported schemaVersion: " + message.schemaVersion()
        );
        require(message.renderJobId() != null, "renderJobId is required.");
        requirePositive(message.filmRollId(), "filmRollId");
        requirePositive(message.userId(), "userId");
        requirePositive(message.regionId(), "regionId");
        requireText(message.bucket(), "bucket");
        requireText(message.filterId(), "filterId");

        require(
                Double.isFinite(message.filterStrength())
                        && message.filterStrength() >= 0.0
                        && message.filterStrength() <= 1.0,
                "filterStrength must be between 0.0 and 1.0."
        );
        require(
                message.filterVersion() >= 1,
                "filterVersion must be at least 1."
        );
        require(
                message.totalPhotoCount() >= 1
                        && message.totalPhotoCount()
                        <= MAX_PHOTO_COUNT,
                "totalPhotoCount must be between 1 and 24."
        );
        require(
                message.requestedAt() != null,
                "requestedAt is required."
        );

        List<RenderQueueMessage.PhotoItem> photos = message.photos();
        require(
                photos != null && !photos.isEmpty(),
                "At least one photo is required."
        );
        require(
                photos.size() == message.totalPhotoCount(),
                "photos size must match totalPhotoCount."
        );

        validatePhotos(photos);
    }

    private void validatePhotos(
            List<RenderQueueMessage.PhotoItem> photos
    ) {
        Set<Long> photoIds = new HashSet<>();
        Set<Integer> sequences = new HashSet<>();

        for (RenderQueueMessage.PhotoItem photo : photos) {
            require(photo != null, "photo item is required.");
            requirePositive(photo.photoId(), "photoId");
            require(
                    photoIds.add(photo.photoId()),
                    "photoId must be unique: " + photo.photoId()
            );
            require(
                    photo.sequence() >= 1
                            && photo.sequence() <= photos.size(),
                    "photo sequence is out of range: "
                            + photo.sequence()
            );
            require(
                    sequences.add(photo.sequence()),
                    "photo sequence must be unique: "
                            + photo.sequence()
            );
            requireText(
                    photo.originalObjectKey(),
                    "originalObjectKey"
            );
            require(
                    photo.takenAt() != null,
                    "takenAt is required for photoId=" + photo.photoId()
            );

        }

        for (int sequence = 1;
             sequence <= photos.size();
             sequence++) {
            require(
                    sequences.contains(sequence),
                    "photo sequences must be contiguous from 1."
            );
        }
    }

    private void requirePositive(Long value, String fieldName) {
        require(
                value != null && value > 0,
                fieldName + " must be a positive number."
        );
    }

    private void requireText(String value, String fieldName) {
        require(
                value != null && !value.isBlank(),
                fieldName + " is required."
        );
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidRenderMessageException(message);
        }
    }
}
