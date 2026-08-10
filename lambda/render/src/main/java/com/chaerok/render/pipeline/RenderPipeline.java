package com.chaerok.render.pipeline;

import com.chaerok.backend.filter.engine.FilmFilterEngine;
import com.chaerok.render.media.FilteredPhotoZipWriter;
import com.chaerok.render.media.JpegImageWriter;
import com.chaerok.render.media.ReelRenderer;
import com.chaerok.render.message.RenderQueueMessage;
import com.chaerok.render.output.FilteredPhotoOutput;
import com.chaerok.render.output.RenderObjectKeys;
import com.chaerok.render.output.RenderOutput;
import com.chaerok.render.storage.ObjectStorage;
import com.chaerok.render.workspace.RenderWorkspace;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public final class RenderPipeline {

    private static final int MAX_IMAGE_WIDTH = 6000;
    private static final int MAX_IMAGE_HEIGHT = 6000;

    private final ObjectStorage objectStorage;
    private final FilmFilterEngine filmFilterEngine;
    private final JpegImageWriter jpegImageWriter;
    private final FilteredPhotoZipWriter zipWriter;
    private final ReelRenderer reelRenderer;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RenderPipeline(
            ObjectStorage objectStorage,
            FilmFilterEngine filmFilterEngine,
            JpegImageWriter jpegImageWriter,
            FilteredPhotoZipWriter zipWriter,
            ReelRenderer reelRenderer,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.objectStorage = require(objectStorage, "objectStorage");
        this.filmFilterEngine = require(
                filmFilterEngine,
                "filmFilterEngine"
        );
        this.jpegImageWriter = require(
                jpegImageWriter,
                "jpegImageWriter"
        );
        this.zipWriter = require(zipWriter, "zipWriter");
        this.reelRenderer = require(reelRenderer, "reelRenderer");
        this.objectMapper = require(objectMapper, "objectMapper");
        this.clock = require(clock, "clock");
    }

    public RenderOutput execute(
            RenderQueueMessage message,
            Consumer<String> logger
    ) {
        Consumer<String> safeLogger = logger == null
                ? ignored -> {
                }
                : logger;

        try (RenderWorkspace workspace =
                     RenderWorkspace.create(message.renderJobId())) {
            RenderOutput completedOutput = loadCompletedOutput(
                    message,
                    workspace,
                    safeLogger
            );

            if (completedOutput != null) {
                return completedOutput;
            }

            List<RenderQueueMessage.PhotoItem> orderedPhotos =
                    message.photos().stream()
                            .sorted(
                                    Comparator.comparingInt(
                                            RenderQueueMessage.PhotoItem
                                                    ::sequence
                                    )
                            )
                            .toList();

            List<Path> filteredFiles = new ArrayList<>();
            List<FilteredPhotoOutput> filteredOutputs =
                    new ArrayList<>();

            for (RenderQueueMessage.PhotoItem photo : orderedPhotos) {
                FilteredPhotoOutput output = processPhoto(
                        message,
                        photo,
                        workspace,
                        safeLogger
                );

                filteredFiles.add(
                        workspace.filteredPhoto(photo.sequence())
                );
                filteredOutputs.add(output);
            }

            safeLogger.accept("Creating filtered photo ZIP.");
            zipWriter.write(filteredFiles, workspace.zipFile());

            safeLogger.accept("Rendering 9:16 MP4 with FFmpeg.");
            reelRenderer.render(
                    workspace.filteredDirectory(),
                    filteredFiles.size(),
                    workspace.reelFile()
            );

            RenderOutput output = createOutput(
                    message,
                    filteredOutputs,
                    workspace
            );

            writeManifest(output, workspace.manifestFile());
            uploadExports(message, output, workspace, safeLogger);

            return output;
        }
    }


    private RenderOutput loadCompletedOutput(
            RenderQueueMessage message,
            RenderWorkspace workspace,
            Consumer<String> logger
    ) {
        String manifestObjectKey = RenderObjectKeys.manifest(message);

        if (!objectStorage.exists(message.bucket(), manifestObjectKey)) {
            return null;
        }

        logger.accept(
                "Completed manifest already exists. Reusing prior output: "
                        + manifestObjectKey
        );

        objectStorage.download(
                message.bucket(),
                manifestObjectKey,
                workspace.manifestFile()
        );

        try {
            RenderOutput output = objectMapper.readValue(
                    workspace.manifestFile().toFile(),
                    RenderOutput.class
            );

            if (!message.renderJobId().equals(output.renderJobId())
                    || !message.filmRollId().equals(output.filmRollId())
                    || !"COMPLETED".equals(output.status())) {
                throw new RenderPipelineException(
                        "Existing manifest does not match render request."
                );
            }

            return output;
        } catch (IOException exception) {
            throw new RenderPipelineException(
                    "Failed to read existing render manifest.",
                    exception
            );
        }
    }

    private FilteredPhotoOutput processPhoto(
            RenderQueueMessage message,
            RenderQueueMessage.PhotoItem photo,
            RenderWorkspace workspace,
            Consumer<String> logger
    ) {
        Path source = workspace.inputPhoto(photo.sequence());
        Path filtered = workspace.filteredPhoto(photo.sequence());

        logger.accept(
                "Downloading photo sequence="
                        + photo.sequence()
                        + ", key="
                        + photo.originalObjectKey()
        );

        objectStorage.download(
                message.bucket(),
                photo.originalObjectKey(),
                source
        );

        BufferedImage original = readImage(source);
        validateImageSize(original, photo.sequence());

        BufferedImage filteredImage = filmFilterEngine.apply(
                original,
                message.filterId(),
                message.filterStrength()
        );

        jpegImageWriter.write(filteredImage, filtered);

        String objectKey = RenderObjectKeys.filteredPhoto(
                message,
                photo.sequence()
        );

        objectStorage.upload(
                message.bucket(),
                objectKey,
                filtered,
                "image/jpeg"
        );

        return new FilteredPhotoOutput(
                photo.photoId(),
                photo.sequence(),
                objectKey,
                fileSize(filtered)
        );
    }

    private RenderOutput createOutput(
            RenderQueueMessage message,
            List<FilteredPhotoOutput> filteredOutputs,
            RenderWorkspace workspace
    ) {
        return new RenderOutput(
                RenderOutput.CURRENT_SCHEMA_VERSION,
                message.renderJobId(),
                message.filmRollId(),
                "COMPLETED",
                filteredOutputs,
                RenderObjectKeys.zip(message),
                fileSize(workspace.zipFile()),
                RenderObjectKeys.reel(message),
                fileSize(workspace.reelFile()),
                RenderObjectKeys.manifest(message),
                Instant.now(clock)
        );
    }

    private void uploadExports(
            RenderQueueMessage message,
            RenderOutput output,
            RenderWorkspace workspace,
            Consumer<String> logger
    ) {
        logger.accept("Uploading ZIP: " + output.zipObjectKey());
        objectStorage.upload(
                message.bucket(),
                output.zipObjectKey(),
                workspace.zipFile(),
                "application/zip"
        );

        logger.accept("Uploading MP4: " + output.reelObjectKey());
        objectStorage.upload(
                message.bucket(),
                output.reelObjectKey(),
                workspace.reelFile(),
                "video/mp4"
        );

        logger.accept(
                "Uploading manifest: " + output.manifestObjectKey()
        );
        objectStorage.upload(
                message.bucket(),
                output.manifestObjectKey(),
                workspace.manifestFile(),
                "application/json"
        );
    }

    private BufferedImage readImage(Path source) {
        try {
            BufferedImage image = ImageIO.read(source.toFile());
            if (image == null) {
                throw new RenderPipelineException(
                        "Unsupported or invalid image: " + source
                );
            }
            return image;
        } catch (IOException exception) {
            throw new RenderPipelineException(
                    "Failed to read image: " + source,
                    exception
            );
        }
    }

    private void validateImageSize(
            BufferedImage image,
            int sequence
    ) {
        if (image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new RenderPipelineException(
                    "Invalid image dimensions for sequence=" + sequence
            );
        }

        if (image.getWidth() > MAX_IMAGE_WIDTH
                || image.getHeight() > MAX_IMAGE_HEIGHT) {
            throw new RenderPipelineException(
                    "Image exceeds maximum dimensions for sequence="
                            + sequence
            );
        }
    }

    private void writeManifest(
            RenderOutput output,
            Path destination
    ) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(destination.toFile(), output);
        } catch (IOException exception) {
            throw new RenderPipelineException(
                    "Failed to write render manifest.",
                    exception
            );
        }
    }

    private long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            throw new RenderPipelineException(
                    "Failed to read file size: " + path,
                    exception
            );
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value;
    }
}
