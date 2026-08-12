package com.chaerok.render.reel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FfmpegPhotoStreamRendererTest {

    private final ReelTemplate template =
            new ReelTemplateRegistry().require(
                    ReelTemplateRegistry.GONGJU_V1
            );

    @Test
    @DisplayName("인트로와 본 스크롤을 별도 FFmpeg 프로세스로 렌더해 메모리 사용을 제한한다")
    void buildsSeparateIntroScrollAndConcatCommands() {
        PhotoStream stream = new PhotoStream(
                Path.of("moving-film-strip.png"),
                List.of(
                        Path.of("intro-1.png"),
                        Path.of("intro-2.png"),
                        Path.of("intro-3.png"),
                        Path.of("intro-4.png")
                ),
                1080,
                76 + 516 * 24 + 18 * 23 + 81,
                24,
                534,
                76 + 23 * 534
        );

        PhotoStreamScrollPlan plan =
                PhotoStreamScrollPlan.calculate(
                        stream,
                        template
                );

        FfmpegPhotoStreamRenderer renderer =
                new FfmpegPhotoStreamRenderer("ffmpeg");

        List<String> intro = renderer.buildIntroCommand(
                Path.of("intro-list.txt"),
                Path.of("intro.mp4")
        );
        List<String> scroll = renderer.buildScrollCommand(
                stream,
                plan,
                Path.of("scroll.mp4")
        );
        List<String> concat = renderer.buildConcatCommand(
                Path.of("final-list.txt"),
                Path.of("reel.mp4")
        );

        assertThat(intro)
                .containsSubsequence("-f", "concat")
                .containsSubsequence("-vf", "fps=30,format=yuv420p");

        String scrollFilter = scroll.get(
                scroll.indexOf("-filter_complex") + 1
        );
        assertThat(scrollFilter)
                .contains("[1:v]scale=911:-1[film]")
                .contains("overlay=x=84")
                .contains("min(max(")
                .doesNotContain("split=");

        assertThat(concat)
                .containsSubsequence("-c", "copy");
    }
}