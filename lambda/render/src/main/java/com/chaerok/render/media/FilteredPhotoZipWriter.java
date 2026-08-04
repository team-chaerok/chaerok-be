package com.chaerok.render.media;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class FilteredPhotoZipWriter {

    public void write(
            List<Path> orderedPhotos,
            Path destination
    ) {
        if (orderedPhotos == null || orderedPhotos.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one filtered photo is required."
            );
        }

        try {
            Files.createDirectories(destination.getParent());

            try (ZipOutputStream zipOutput = new ZipOutputStream(
                    new BufferedOutputStream(
                            Files.newOutputStream(destination)
                    )
            )) {
                for (Path photo : orderedPhotos) {
                    addPhoto(zipOutput, photo);
                }
            }
        } catch (IOException exception) {
            throw new MediaGenerationException(
                    "Failed to create filtered photo ZIP.",
                    exception
            );
        }
    }

    private void addPhoto(
            ZipOutputStream zipOutput,
            Path photo
    ) throws IOException {
        ZipEntry entry = new ZipEntry(photo.getFileName().toString());
        entry.setTime(0L);
        zipOutput.putNextEntry(entry);

        try (BufferedInputStream input = new BufferedInputStream(
                Files.newInputStream(photo)
        )) {
            input.transferTo(zipOutput);
        }

        zipOutput.closeEntry();
    }
}
