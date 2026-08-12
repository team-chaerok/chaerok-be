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
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public final class RenderPipeline {

    private static final int MAX_IMAGE_DIMENSION = 6000;
    private static final long MAX_IMAGE_PIXELS = 16_000_000L;
    private static final String JPEG_FORMAT = "JPEG";

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
            try {
                zipWriter.write(filteredFiles, workspace.zipFile());
            } catch (RuntimeException exception) {
                throw failure(
                        "ZIP_GENERATION_FAILED",
                        true,
                        "Failed to create filtered photo ZIP.",
                        exception
                );
            }

            safeLogger.accept("Rendering 9:16 MP4 with FFmpeg.");
            try {
                reelRenderer.render(
                        message.filterId(),
                        filteredFiles,
                        workspace.reelFile()
                );
            } catch (RuntimeException exception) {
                throw failure(
                        "REEL_GENERATION_FAILED",
                        true,
                        "Failed to render reel MP4.",
                        exception
                );
            }

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

        boolean manifestExists;
        try {
            manifestExists = objectStorage.exists(
                    message.bucket(),
                    manifestObjectKey
            );
        } catch (RuntimeException exception) {
            throw failure(
                    "MANIFEST_LOOKUP_FAILED",
                    true,
                    "Failed to check existing render manifest.",
                    exception
            );
        }

        if (!manifestExists) {
            return null;
        }

        logger.accept(
                "Completed manifest already exists. Reusing prior output: "
                        + manifestObjectKey
        );

        try {
            objectStorage.download(
                    message.bucket(),
                    manifestObjectKey,
                    workspace.manifestFile()
            );
        } catch (RuntimeException exception) {
            throw failure(
                    "MANIFEST_DOWNLOAD_FAILED",
                    true,
                    "Failed to download existing render manifest.",
                    exception
            );
        }

        try {
            RenderOutput output = objectMapper.readValue(
                    workspace.manifestFile().toFile(),
                    RenderOutput.class
            );

            if (!message.renderJobId().equals(output.renderJobId())
                    || !message.filmRollId().equals(output.filmRollId())
                    || !"COMPLETED".equals(output.status())) {
                throw failure(
                        "MANIFEST_INVALID",
                        false,
                        "Existing manifest does not match render request."
                );
            }

            return output;
        } catch (RenderFailureException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw failure(
                    "MANIFEST_INVALID",
                    false,
                    "Existing render manifest is invalid or unreadable.",
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

        try {
            objectStorage.download(
                    message.bucket(),
                    photo.originalObjectKey(),
                    source
            );
        } catch (RuntimeException exception) {
            throw photoFailure(
                    "PHOTO_DOWNLOAD_FAILED",
                    true,
                    "DOWNLOAD",
                    photo,
                    exception
            );
        }

        inspectImageBeforeDecode(source, photo);
        BufferedImage original = readImage(source, photo);

        BufferedImage filteredImage;
        try {
            filteredImage = filmFilterEngine.apply(
                    original,
                    message.filterId(),
                    message.filterStrength()
            );
        } catch (RuntimeException exception) {
            throw photoFailure(
                    "PHOTO_FILTER_FAILED",
                    false,
                    "FILTER",
                    photo,
                    exception
            );
        }

        try {
            jpegImageWriter.write(filteredImage, filtered);
        } catch (RuntimeException exception) {
            throw photoFailure(
                    "PHOTO_ENCODE_FAILED",
                    true,
                    "ENCODE",
                    photo,
                    exception
            );
        }

        long filteredFileSize;
        try {
            filteredFileSize = Files.size(filtered);
            if (filteredFileSize <= 0L) {
                throw new IOException(
                        "Filtered JPEG is empty: " + filtered
                );
            }
        } catch (IOException exception) {
            throw photoFailure(
                    "PHOTO_ENCODE_FAILED",
                    true,
                    "ENCODE_VERIFY",
                    photo,
                    exception
            );
        }

        String objectKey = RenderObjectKeys.filteredPhoto(
                message,
                photo.sequence()
        );

        try {
            objectStorage.upload(
                    message.bucket(),
                    objectKey,
                    filtered,
                    "image/jpeg"
            );
        } catch (RuntimeException exception) {
            throw photoFailure(
                    "PHOTO_UPLOAD_FAILED",
                    true,
                    "UPLOAD",
                    photo,
                    exception
            );
        }

        return new FilteredPhotoOutput(
                photo.photoId(),
                photo.sequence(),
                objectKey,
                filteredFileSize
        );
    }

    private RenderOutput createOutput(
            RenderQueueMessage message,
            List<FilteredPhotoOutput> filteredOutputs,
            RenderWorkspace workspace
    ) {
        long zipFileSize = fileSize(
                workspace.zipFile(),
                "ZIP_GENERATION_FAILED",
                "Failed to verify filtered photo ZIP."
        );
        long reelFileSize = fileSize(
                workspace.reelFile(),
                "REEL_GENERATION_FAILED",
                "Failed to verify reel MP4."
        );

        return new RenderOutput(
                RenderOutput.CURRENT_SCHEMA_VERSION,
                message.renderJobId(),
                message.filmRollId(),
                "COMPLETED",
                filteredOutputs,
                RenderObjectKeys.zip(message),
                zipFileSize,
                RenderObjectKeys.reel(message),
                reelFileSize,
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
        try {
            objectStorage.upload(
                    message.bucket(),
                    output.zipObjectKey(),
                    workspace.zipFile(),
                    "application/zip"
            );
        } catch (RuntimeException exception) {
            throw failure(
                    "ZIP_UPLOAD_FAILED",
                    true,
                    "Failed to upload filtered photo ZIP.",
                    exception
            );
        }

        logger.accept("Uploading MP4: " + output.reelObjectKey());
        try {
            objectStorage.upload(
                    message.bucket(),
                    output.reelObjectKey(),
                    workspace.reelFile(),
                    "video/mp4"
            );
        } catch (RuntimeException exception) {
            throw failure(
                    "REEL_UPLOAD_FAILED",
                    true,
                    "Failed to upload reel MP4.",
                    exception
            );
        }

        logger.accept(
                "Uploading manifest: " + output.manifestObjectKey()
        );
        try {
            objectStorage.upload(
                    message.bucket(),
                    output.manifestObjectKey(),
                    workspace.manifestFile(),
                    "application/json"
            );
        } catch (RuntimeException exception) {
            throw failure(
                    "MANIFEST_UPLOAD_FAILED",
                    true,
                    "Failed to upload render manifest.",
                    exception
            );
        }
    }

    private void inspectImageBeforeDecode(
            Path source,
            RenderQueueMessage.PhotoItem photo
    ) {
        try (ImageInputStream input =
                     ImageIO.createImageInputStream(source.toFile())) {
            if (input == null) {
                throw photoFailure(
                        "PHOTO_INVALID_IMAGE",
                        false,
                        "READ_HEADER",
                        photo,
                        "image input stream is unavailable"
                );
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw photoFailure(
                        "PHOTO_INVALID_IMAGE",
                        false,
                        "READ_HEADER",
                        photo,
                        "unsupported or invalid image header"
                );
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);

                String formatName = reader.getFormatName();
                if (!JPEG_FORMAT.equalsIgnoreCase(formatName)) {
                    throw photoFailure(
                            "PHOTO_INVALID_IMAGE",
                            false,
                            "READ_HEADER",
                            photo,
                            "expected JPEG but detected " + formatName
                    );
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateImageSize(width, height, photo);
            } finally {
                reader.dispose();
            }
        } catch (RenderFailureException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw photoFailure(
                    "PHOTO_INVALID_IMAGE",
                    false,
                    "READ_HEADER",
                    photo,
                    exception
            );
        }
    }

    private BufferedImage readImage(
            Path source,
            RenderQueueMessage.PhotoItem photo
    ) {
        try {
            BufferedImage image = ImageIO.read(source.toFile());
            if (image == null) {
                throw photoFailure(
                        "PHOTO_INVALID_IMAGE",
                        false,
                        "DECODE",
                        photo,
                        "JPEG decode returned no image"
                );
            }
            return image;
        } catch (RenderFailureException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw photoFailure(
                    "PHOTO_INVALID_IMAGE",
                    false,
                    "DECODE",
                    photo,
                    exception
            );
        }
    }

    private void validateImageSize(
            int width,
            int height,
            RenderQueueMessage.PhotoItem photo
    ) {
        if (width <= 0 || height <= 0) {
            throw photoFailure(
                    "PHOTO_INVALID_IMAGE",
                    false,
                    "VALIDATE_DIMENSIONS",
                    photo,
                    "width=" + width + ", height=" + height
            );
        }

        long pixels = (long) width * height;
        if (width > MAX_IMAGE_DIMENSION
                || height > MAX_IMAGE_DIMENSION
                || pixels > MAX_IMAGE_PIXELS) {
            throw photoFailure(
                    "PHOTO_TOO_LARGE",
                    false,
                    "VALIDATE_DIMENSIONS",
                    photo,
                    "width=" + width
                            + ", height=" + height
                            + ", pixels=" + pixels
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
        } catch (IOException | RuntimeException exception) {
            throw failure(
                    "MANIFEST_GENERATION_FAILED",
                    true,
                    "Failed to write render manifest.",
                    exception
            );
        }
    }

    private long fileSize(
            Path path,
            String errorCode,
            String message
    ) {
        try {
            long size = Files.size(path);
            if (size <= 0L) {
                throw new IOException("Generated file is empty: " + path);
            }
            return size;
        } catch (IOException exception) {
            throw failure(
                    errorCode,
                    true,
                    message,
                    exception
            );
        }
    }

    private RenderFailureException photoFailure(
            String errorCode,
            boolean retryable,
            String stage,
            RenderQueueMessage.PhotoItem photo,
            Throwable cause
    ) {
        String detail = cause == null
                ? null
                : cause.getMessage();
        return photoFailure(
                errorCode,
                retryable,
                stage,
                photo,
                detail,
                cause
        );
    }

    private RenderFailureException photoFailure(
            String errorCode,
            boolean retryable,
            String stage,
            RenderQueueMessage.PhotoItem photo,
            String detail
    ) {
        return photoFailure(
                errorCode,
                retryable,
                stage,
                photo,
                detail,
                null
        );
    }

    private RenderFailureException photoFailure(
            String errorCode,
            boolean retryable,
            String stage,
            RenderQueueMessage.PhotoItem photo,
            String detail,
            Throwable cause
    ) {
        StringBuilder message = new StringBuilder()
                .append("stage=")
                .append(stage)
                .append(", photoId=")
                .append(photo.photoId())
                .append(", sequence=")
                .append(photo.sequence());

        if (detail != null && !detail.isBlank()) {
            message.append(", detail=").append(detail);
        }

        return cause == null
                ? new RenderFailureException(
                        errorCode,
                        retryable,
                        message.toString()
                )
                : new RenderFailureException(
                        errorCode,
                        retryable,
                        message.toString(),
                        cause
                );
    }

    private RenderFailureException failure(
            String errorCode,
            boolean retryable,
            String message,
            Throwable cause
    ) {
        return new RenderFailureException(
                errorCode,
                retryable,
                message,
                cause
        );
    }

    private RenderFailureException failure(
            String errorCode,
            boolean retryable,
            String message
    ) {
        return new RenderFailureException(
                errorCode,
                retryable,
                message
        );
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value;
    }
}
