package com.chaerok.render.reel;

import com.chaerok.render.media.MediaGenerationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class FfmpegFilmStripRenderer {

    private static final int FRAME_RATE = 30;
    private static final Duration TIMEOUT = Duration.ofMinutes(4);

    private final String ffmpegPath;

    public FfmpegFilmStripRenderer(String ffmpegPath) {
        if (ffmpegPath == null || ffmpegPath.isBlank()) {
            throw new IllegalArgumentException(
                    "FFmpeg path is required."
            );
        }
        this.ffmpegPath = ffmpegPath;
    }

    public void render(
            FilmStrip strip,
            ReelTemplate template,
            Path destination
    ) {
        if (destination == null) {
            throw new IllegalArgumentException(
                    "destination is required."
            );
        }
        if (strip == null || Files.notExists(strip.path())) {
            throw new IllegalArgumentException(
                    "Film strip PNG is required."
            );
        }

        ScrollReelPlan plan =
                ScrollReelPlan.calculate(strip, template);

        try {
            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException exception) {
            throw new MediaGenerationException(
                    "Failed to create reel output directory.",
                    exception
            );
        }

        Path logFile = destination.resolveSibling(
                destination.getFileName() + ".ffmpeg.log"
        );
        List<String> command = buildCommand(
                strip,
                plan,
                destination
        );

        Process process;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(logFile.toFile())
                    .start();
        } catch (IOException exception) {
            throw new MediaGenerationException(
                    "Failed to start FFmpeg film strip renderer.",
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
                    "FFmpeg film strip rendering was interrupted.",
                    exception
            );
        }

        String output = readOutput(logFile);
        if (!completed) {
            process.destroyForcibly();
            throw new MediaGenerationException(
                    "FFmpeg film strip rendering timed out. output="
                            + summarize(output)
            );
        }
        if (process.exitValue() != 0) {
            throw new MediaGenerationException(
                    "FFmpeg film strip rendering failed with exit code "
                            + process.exitValue()
                            + ". output="
                            + summarize(output)
            );
        }

        try {
            if (Files.notExists(destination)
                    || Files.size(destination) == 0L) {
                throw new MediaGenerationException(
                        "FFmpeg did not create a valid film strip MP4."
                );
            }
        } catch (IOException exception) {
            throw new MediaGenerationException(
                    "Failed to verify film strip MP4.",
                    exception
            );
        }
    }

    List<String> buildCommand(
            FilmStrip strip,
            ScrollReelPlan plan,
            Path destination
    ) {
        return new ArrayList<>(List.of(
                ffmpegPath,
                "-hide_banner",
                "-loglevel", "error",
                "-y",
                "-loop", "1",
                "-framerate", String.valueOf(FRAME_RATE),
                "-i", strip.path().toString(),
                "-t", seconds(plan.totalDurationSeconds()),
                "-vf", filterExpression(plan),
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-crf", "20",
                "-r", String.valueOf(FRAME_RATE),
                "-pix_fmt", "yuv420p",
                "-movflags", "+faststart",
                destination.toString()
        ));
    }

    private String filterExpression(ScrollReelPlan plan) {
        String yExpression;
        if (plan.scrollDistance() == 0) {
            yExpression = "0";
        } else {
            yExpression = "'if(lt(t,"
                    + seconds(plan.startHoldSeconds())
                    + "),0,if(lt(t,"
                    + seconds(plan.scrollEndSeconds())
                    + "),(ih-"
                    + plan.canvasHeight()
                    + ")*(t-"
                    + seconds(plan.startHoldSeconds())
                    + ")/"
                    + seconds(plan.scrollDurationSeconds())
                    + ",ih-"
                    + plan.canvasHeight()
                    + "))'";
        }

        return "crop="
                + plan.canvasWidth()
                + ":"
                + plan.canvasHeight()
                + ":0:"
                + yExpression
                + ",setsar=1,format=yuv420p";
    }

    private String seconds(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private String readOutput(Path logFile) {
        try {
            return Files.notExists(logFile)
                    ? "<empty>"
                    : Files.readString(logFile);
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
