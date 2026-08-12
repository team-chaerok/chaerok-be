package com.chaerok.render.reel;

import com.chaerok.render.media.MediaGenerationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class FfmpegPhotoStreamRenderer {

    private static final int FRAME_RATE = 30;
    private static final Duration PROCESS_TIMEOUT =
            Duration.ofMinutes(4);

    private final String ffmpegPath;

    public FfmpegPhotoStreamRenderer(String ffmpegPath) {
        if (ffmpegPath == null || ffmpegPath.isBlank()) {
            throw new IllegalArgumentException(
                    "FFmpeg path is required."
            );
        }
        this.ffmpegPath = ffmpegPath;
    }

    public void render(
            PhotoStream stream,
            ReelTemplate template,
            Path destination
    ) {
        validate(stream, destination);

        PhotoStreamScrollPlan plan =
                PhotoStreamScrollPlan.calculate(
                        stream,
                        template
                );

        Path parent = destination.getParent();
        if (parent == null) {
            parent = Path.of(".");
        }

        Path introList = parent.resolve(
                destination.getFileName()
                        + ".intro.concat.txt"
        );
        Path introVideo = parent.resolve(
                destination.getFileName()
                        + ".intro.mp4"
        );
        Path scrollVideo = parent.resolve(
                destination.getFileName()
                        + ".scroll.mp4"
        );
        Path finalList = parent.resolve(
                destination.getFileName()
                        + ".final.concat.txt"
        );

        try {
            Files.createDirectories(parent);

            writeIntroConcatList(
                    stream,
                    plan,
                    introList
            );

            run(
                    buildIntroCommand(
                            introList,
                            introVideo
                    ),
                    introVideo.resolveSibling(
                            introVideo.getFileName()
                                    + ".ffmpeg.log"
                    ),
                    "intro"
            );

            run(
                    buildScrollCommand(
                            stream,
                            plan,
                            scrollVideo
                    ),
                    scrollVideo.resolveSibling(
                            scrollVideo.getFileName()
                                    + ".ffmpeg.log"
                    ),
                    "scroll"
            );

            writeFinalConcatList(
                    introVideo,
                    scrollVideo,
                    finalList
            );

            run(
                    buildConcatCommand(
                            finalList,
                            destination
                    ),
                    destination.resolveSibling(
                            destination.getFileName()
                                    + ".concat.ffmpeg.log"
                    ),
                    "concat"
            );

            if (Files.notExists(destination)
                    || Files.size(destination) == 0L) {
                throw new MediaGenerationException(
                        "FFmpeg did not create a valid reel MP4."
                );
            }
        } catch (IOException exception) {
            throw new MediaGenerationException(
                    "Failed to render memory-efficient reel.",
                    exception
            );
        } finally {
            deleteQuietly(introList);
            deleteQuietly(finalList);
            deleteQuietly(introVideo);
            deleteQuietly(scrollVideo);
        }
    }

    List<String> buildIntroCommand(
            Path introList,
            Path destination
    ) {
        return List.of(
                ffmpegPath,
                "-hide_banner",
                "-loglevel", "error",
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", introList.toString(),
                "-vf", "fps=" + FRAME_RATE + ",format=yuv420p",
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-crf", "20",
                "-r", String.valueOf(FRAME_RATE),
                "-pix_fmt", "yuv420p",
                "-movflags", "+faststart",
                destination.toString()
        );
    }

    List<String> buildScrollCommand(
            PhotoStream stream,
            PhotoStreamScrollPlan plan,
            Path destination
    ) {
        String duration = seconds(
                plan.scrollSegmentDurationSeconds()
        );

        List<String> command = new ArrayList<>();
        command.addAll(List.of(
                ffmpegPath,
                "-hide_banner",
                "-loglevel", "error",
                "-y",
                "-f", "lavfi",
                "-i", "color=c=black:s="
                        + plan.canvasWidth()
                        + "x"
                        + plan.canvasHeight()
                        + ":r="
                        + FRAME_RATE
                        + ":d="
                        + duration,
                "-loop", "1",
                "-framerate", String.valueOf(FRAME_RATE),
                "-i", stream.path().toString(),
                "-filter_complex", scrollFilterGraph(plan),
                "-map", "[v]",
                "-t", duration,
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-crf", "20",
                "-r", String.valueOf(FRAME_RATE),
                "-pix_fmt", "yuv420p",
                "-movflags", "+faststart",
                destination.toString()
        ));
        return command;
    }

    List<String> buildConcatCommand(
            Path concatList,
            Path destination
    ) {
        return List.of(
                ffmpegPath,
                "-hide_banner",
                "-loglevel", "error",
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", concatList.toString(),
                "-c", "copy",
                "-movflags", "+faststart",
                destination.toString()
        );
    }

    private String scrollFilterGraph(
            PhotoStreamScrollPlan plan
    ) {
        return "[1:v]scale="
                + plan.stripWidth()
                + ":-1[film];"
                + "[0:v][film]overlay=x="
                + plan.stripX()
                + ":y='"
                + movingY(plan)
                + "':eval=frame,"
                + "format=yuv420p[v]";
    }

    private String movingY(
            PhotoStreamScrollPlan plan
    ) {
        if (plan.scrollDistance() == 0
                || plan.scrollDurationSeconds() == 0.0) {
            return String.valueOf(plan.scrollStartY());
        }

        String progress = "(t/"
                + seconds(plan.scrollDurationSeconds())
                + ")";

        return "if(lt(t,"
                + seconds(plan.scrollDurationSeconds())
                + "),"
                + plan.scrollStartY()
                + "+"
                + plan.scrollDistance()
                + "*min(max("
                + progress
                + ",0),1)"
                + ","
                + plan.finalScrollY()
                + ")";
    }

    private void writeIntroConcatList(
            PhotoStream stream,
            PhotoStreamScrollPlan plan,
            Path listPath
    ) throws IOException {
        if (stream.introSnapshotPaths().size()
                < plan.introPhotoCount()) {
            throw new MediaGenerationException(
                    "Not enough intro snapshots were generated."
            );
        }

        StringBuilder builder = new StringBuilder();

        for (int index = 0;
             index < plan.introPhotoCount();
             index++) {
            Path snapshot =
                    stream.introSnapshotPaths().get(index);

            builder.append("file '")
                    .append(escapeConcatPath(snapshot))
                    .append("'")
                    .append(System.lineSeparator());

            double duration = plan.introStepSeconds();
            if (index == plan.introPhotoCount() - 1) {
                duration += plan.introHoldSeconds();
            }

            builder.append("duration ")
                    .append(seconds(duration))
                    .append(System.lineSeparator());
        }

        Path last = stream.introSnapshotPaths().get(
                plan.introPhotoCount() - 1
        );
        builder.append("file '")
                .append(escapeConcatPath(last))
                .append("'")
                .append(System.lineSeparator());

        Files.writeString(
                listPath,
                builder.toString(),
                StandardCharsets.UTF_8
        );
    }

    private void writeFinalConcatList(
            Path introVideo,
            Path scrollVideo,
            Path listPath
    ) throws IOException {
        String content =
                "file '" + escapeConcatPath(introVideo) + "'"
                        + System.lineSeparator()
                        + "file '" + escapeConcatPath(scrollVideo) + "'"
                        + System.lineSeparator();

        Files.writeString(
                listPath,
                content,
                StandardCharsets.UTF_8
        );
    }

    private String escapeConcatPath(Path path) {
        return path.toAbsolutePath()
                .toString()
                .replace("\\", "/")
                .replace("'", "'\\''");
    }

    private void run(
            List<String> command,
            Path logFile,
            String stage
    ) {
        Process process;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(logFile.toFile())
                    .start();
        } catch (IOException exception) {
            throw new MediaGenerationException(
                    "Failed to start FFmpeg " + stage + " stage.",
                    exception
            );
        }

        boolean completed;
        try {
            completed = process.waitFor(
                    PROCESS_TIMEOUT.toSeconds(),
                    TimeUnit.SECONDS
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new MediaGenerationException(
                    "FFmpeg " + stage + " stage was interrupted.",
                    exception
            );
        }

        String output = readOutput(logFile);

        if (!completed) {
            process.destroyForcibly();
            throw new MediaGenerationException(
                    "FFmpeg " + stage + " stage timed out. output="
                            + summarize(output)
            );
        }
        if (process.exitValue() != 0) {
            throw new MediaGenerationException(
                    "FFmpeg "
                            + stage
                            + " stage failed with exit code "
                            + process.exitValue()
                            + ". output="
                            + summarize(output)
            );
        }
    }

    private void validate(
            PhotoStream stream,
            Path destination
    ) {
        if (stream == null || Files.notExists(stream.path())) {
            throw new IllegalArgumentException(
                    "Moving film strip PNG is required."
            );
        }
        if (stream.introSnapshotPaths().isEmpty()) {
            throw new IllegalArgumentException(
                    "Intro snapshots are required."
            );
        }
        for (Path snapshot : stream.introSnapshotPaths()) {
            if (Files.notExists(snapshot)) {
                throw new IllegalArgumentException(
                        "Intro snapshot does not exist: "
                                + snapshot
                );
            }
        }
        if (destination == null) {
            throw new IllegalArgumentException(
                    "destination is required."
            );
        }
    }

    private String seconds(double value) {
        return String.format(
                Locale.ROOT,
                "%.3f",
                value
        );
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
        String normalized = output
                .replaceAll("\\s+", " ")
                .trim();

        return normalized.length() <= 1200
                ? normalized
                : normalized.substring(0, 1200);
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}