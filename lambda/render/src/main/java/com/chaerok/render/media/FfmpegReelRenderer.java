package com.chaerok.render.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class FfmpegReelRenderer implements ReelRenderer {

    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1920;
    private static final Duration TIMEOUT = Duration.ofMinutes(4);

    private final String ffmpegPath;

    public FfmpegReelRenderer(String ffmpegPath) {
        if (ffmpegPath == null || ffmpegPath.isBlank()) {
            throw new IllegalArgumentException(
                    "FFmpeg path is required."
            );
        }
        this.ffmpegPath = ffmpegPath;
    }

    @Override
    public void render(
            Path filteredDirectory,
            int photoCount,
            Path destination
    ) {
        if (photoCount < 1) {
            throw new IllegalArgumentException(
                    "At least one photo is required for reel rendering."
            );
        }

        try {
            Files.createDirectories(destination.getParent());
        } catch (IOException exception) {
            throw new MediaGenerationException(
                    "Failed to create reel output directory.",
                    exception
            );
        }

        Path logFile = destination.resolveSibling(
                destination.getFileName() + ".ffmpeg.log"
        );

        List<String> command = new ArrayList<>(List.of(
                ffmpegPath,
                "-hide_banner",
                "-loglevel", "error",
                "-y",
                "-framerate", "1/2",
                "-start_number", "1",
                "-i", filteredDirectory.resolve("%03d.jpg").toString(),
                "-t", String.valueOf(photoCount * 2),
                "-vf", filterExpression(),
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-crf", "23",
                "-r", "30",
                "-pix_fmt", "yuv420p",
                "-movflags", "+faststart",
                destination.toString()
        ));

        Process process;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(logFile.toFile())
                    .start();
        } catch (IOException exception) {
            throw new MediaGenerationException(
                    "Failed to start FFmpeg.",
                    exception
            );
        }

        boolean completed;
        try {
            completed = process.waitFor(
                    TIMEOUT.toSeconds(),
                    TimeUnit.SECONDS
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new MediaGenerationException(
                    "FFmpeg execution was interrupted.",
                    exception
            );
        }

        String output = readOutput(logFile);

        if (!completed) {
            process.destroyForcibly();
            throw new MediaGenerationException(
                    "FFmpeg timed out after "
                            + TIMEOUT.toSeconds()
                            + " seconds. output="
                            + summarize(output)
            );
        }

        if (process.exitValue() != 0) {
            throw new MediaGenerationException(
                    "FFmpeg failed with exit code "
                            + process.exitValue()
                            + ". output="
                            + summarize(output)
            );
        }

        try {
            if (Files.notExists(destination)
                    || Files.size(destination) == 0L) {
                throw new MediaGenerationException(
                        "FFmpeg did not create a valid MP4 file."
                );
            }
        } catch (IOException exception) {
            throw new MediaGenerationException(
                    "Failed to verify rendered MP4.",
                    exception
            );
        }
    }

    private String filterExpression() {
        return "scale=" + WIDTH + ":" + HEIGHT
                + ":force_original_aspect_ratio=decrease,"
                + "pad=" + WIDTH + ":" + HEIGHT
                + ":(ow-iw)/2:(oh-ih)/2:color=black,"
                + "setsar=1,"
                + "format=yuv420p";
    }

    private String readOutput(Path logFile) {
        try {
            if (Files.notExists(logFile)) {
                return "<empty>";
            }
            return Files.readString(logFile);
        } catch (IOException exception) {
            return "<failed to read FFmpeg output>";
        }
    }

    private String summarize(String output) {
        if (output == null || output.isBlank()) {
            return "<empty>";
        }

        String normalized = output.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1000
                ? normalized
                : normalized.substring(0, 1000);
    }
}
