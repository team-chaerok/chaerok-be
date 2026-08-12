package com.chaerok.render.reel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoStreamScrollPlanTest {

    private final ReelTemplate template =
            new ReelTemplateRegistry().require(
                    ReelTemplateRegistry.GONGJU_V1
            );

    @Test
    @DisplayName("첫 네 장은 0.7초 간격으로 등장하고 마지막 네 장에서 멈춘다")
    void calculatesSlowIntroAndFinalFourHold() {
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

        assertThat(plan.stripWidth()).isEqualTo(911);
        assertThat(plan.stripX()).isEqualTo(84);
        assertThat(plan.introPhotoCount()).isEqualTo(4);
        assertThat(plan.introStepSeconds()).isEqualTo(0.70);
        assertThat(plan.introHoldSeconds()).isEqualTo(0.60);
        assertThat(plan.introDurationSeconds()).isEqualTo(3.40);
        assertThat(plan.scrollDistance()).isEqualTo(9009);
        assertThat(plan.endHoldSeconds()).isEqualTo(1.20);
        assertThat(plan.finalScrollY())
                .isEqualTo(
                        plan.scrollStartY()
                                + plan.scrollDistance()
                );
    }
}