package com.chaerok.render.reel;

import java.nio.file.Path;
import java.util.List;

public record PhotoStream(
        Path path,
        List<Path> introSnapshotPaths,
        int width,
        int height,
        int photoCount,
        int pitch,
        int firstPhotoOffset
) {

    public PhotoStream {
        if (path == null) {
            throw new IllegalArgumentException("path is required.");
        }
        introSnapshotPaths = introSnapshotPaths == null
                ? List.of()
                : List.copyOf(introSnapshotPaths);
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException(
                    "Photo stream dimensions must be positive."
            );
        }
        if (photoCount < 1) {
            throw new IllegalArgumentException(
                    "photoCount must be positive."
            );
        }
        if (pitch < 1) {
            throw new IllegalArgumentException(
                    "pitch must be positive."
            );
        }
        if (firstPhotoOffset < 0) {
            throw new IllegalArgumentException(
                    "firstPhotoOffset must not be negative."
            );
        }
    }

    public PhotoStream(
            Path path,
            int width,
            int height,
            int photoCount,
            int pitch,
            int firstPhotoOffset
    ) {
        this(
                path,
                List.of(),
                width,
                height,
                photoCount,
                pitch,
                firstPhotoOffset
        );
    }
}