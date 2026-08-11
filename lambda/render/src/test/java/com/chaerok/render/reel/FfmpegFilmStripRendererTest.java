package com.chaerok.render.reel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FfmpegFilmStripRendererTest {

    private final ReelTemplate template =
            new ReelTemplateRegistry().require(
                    ReelTemplateRegistry.GONGJU_V1
            );
    private final FfmpegFilmStripRenderer renderer =
            new FfmpegFilmStripRenderer("ffmpeg");

    @Test
    @DisplayName("필름 스트립을 9대16 세로 스크롤 MP4로 만드는 FFmpeg 명령을 구성한다")
    void buildsVerticalScrollCommand() {
        FilmStrip strip = new FilmStrip(
                Path.of("film-strip.png"),
                1080,
                2720,
                3
        );
        ScrollReelPlan plan =
                ScrollReelPlan.calculate(strip, template);

        List<String> command = renderer.buildCommand(
                strip,
                plan,
                Path.of("reel.mp4")
        );

        assertThat(command)
                .containsSubsequence("-loop", "1")
                .containsSubsequence("-framerate", "30")
                .containsSubsequence("-crf", "20")
                .containsSubsequence("-pix_fmt", "yuv420p")
                .containsSubsequence("-movflags", "+faststart");

        String filter = command.get(command.indexOf("-vf") + 1);
        assertThat(filter)
                .contains("crop=1080:1920:0:")
                .contains("lt(t,0.800)")
                .contains("lt(t,11.500)")
                .contains("(ih-1920)")
                .contains("/10.700");
    }

    @Test
    @DisplayName("스크롤할 거리가 없으면 crop y좌표를 0으로 고정한다")
    void keepsCropStillWhenStripMatchesCanvasHeight() {
        FilmStrip strip = new FilmStrip(
                Path.of("film-strip.png"),
                1080,
                1920,
                1
        );
        ScrollReelPlan plan =
                ScrollReelPlan.calculate(strip, template);

        List<String> command = renderer.buildCommand(
                strip,
                plan,
                Path.of("reel.mp4")
        );

        String filter = command.get(command.indexOf("-vf") + 1);
        assertThat(filter).startsWith("crop=1080:1920:0:0");
    }
}
