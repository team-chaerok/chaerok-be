package com.chaerok.render.media;

import java.nio.file.Path;
import java.util.List;

public interface ReelRenderer {

    void render(
            Path filteredDirectory,
            int photoCount,
            Path destination
    );

    default void render(
            String filterId,
            List<Path> orderedPhotos,
            Path destination
    ) {
        if (orderedPhotos == null || orderedPhotos.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one filtered photo is required."
            );
        }

        Path filteredDirectory = orderedPhotos.get(0).getParent();
        if (filteredDirectory == null) {
            throw new IllegalArgumentException(
                    "Filtered photo directory is required."
            );
        }

        render(
                filteredDirectory,
                orderedPhotos.size(),
                destination
        );
    }
}