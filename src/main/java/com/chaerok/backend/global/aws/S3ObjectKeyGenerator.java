package com.chaerok.backend.global.aws;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class S3ObjectKeyGenerator {

    public String createOriginalPhotoKey(
            long userId,
            long filmRollId,
            int sequence
    ) {
        validateIdentifiers(userId, filmRollId);
        validateSequence(sequence);

        return "users/%d/rolls/%d/original/%03d-%s.jpg"
                .formatted(
                        userId,
                        filmRollId,
                        sequence,
                        createRandomSuffix()
                );
    }

    public String createFilteredPhotoKey(
            long userId,
            long filmRollId,
            int sequence
    ) {
        validateIdentifiers(userId, filmRollId);
        validateSequence(sequence);

        return "users/%d/rolls/%d/filtered/%03d-%s.jpg"
                .formatted(
                        userId,
                        filmRollId,
                        sequence,
                        createRandomSuffix()
                );
    }

    public String createZipKey(
            long userId,
            long filmRollId
    ) {
        validateIdentifiers(userId, filmRollId);

        return "users/%d/rolls/%d/export/chaerok_%d.zip"
                .formatted(
                        userId,
                        filmRollId,
                        filmRollId
                );
    }

    public String createReelKey(
            long userId,
            long filmRollId
    ) {
        validateIdentifiers(userId, filmRollId);

        return "users/%d/rolls/%d/export/chaerok_%d.mp4"
                .formatted(
                        userId,
                        filmRollId,
                        filmRollId
                );
    }

    private String createRandomSuffix() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "");
    }

    private void validateIdentifiers(
            long userId,
            long filmRollId
    ) {
        if (userId <= 0) {
            throw new IllegalArgumentException(
                    "사용자 ID는 1 이상이어야 합니다."
            );
        }

        if (filmRollId <= 0) {
            throw new IllegalArgumentException(
                    "필름 롤 ID는 1 이상이어야 합니다."
            );
        }
    }

    private void validateSequence(int sequence) {
        if (sequence <= 0 || sequence > 999) {
            throw new IllegalArgumentException(
                    "사진 순서는 1 이상 999 이하여야 합니다."
            );
        }
    }
}
