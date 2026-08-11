package com.chaerok.render.reel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ScrollReelPlanTest {

    private final ReelTemplate template =
            new ReelTemplateRegistry().require(
                    ReelTemplateRegistry.GONGJU_V1
            );

    @Test
    @DisplayName("사진 세 장 필름 스트립의 스크롤 거리와 재생 시간을 계산한다")
    void calculatesThreePhotoPlan() {
        FilmStrip strip = new FilmStrip(
                Path.of("strip.png"),
                1080,
                2720,
                3
        );

        ScrollReelPlan plan =
                ScrollReelPlan.calculate(strip, template);

        assertThat(plan.scrollDistance()).isEqualTo(800);
        assertThat(plan.totalDurationSeconds()).isEqualTo(12.7);
        assertThat(plan.startHoldSeconds()).isEqualTo(0.8);
        assertThat(plan.endHoldSeconds()).isEqualTo(1.2);
        assertThat(plan.scrollDurationSeconds()).isEqualTo(10.7);
        assertThat(plan.scrollEndSeconds()).isEqualTo(11.5);
    }

    @Test
    @DisplayName("사진 스물네 장도 최대 재생 시간 이내에서 계산한다")
    void calculatesTwentyFourPhotoPlan() {
        FilmStrip strip = new FilmStrip(
                Path.of("strip.png"),
                1080,
                17420,
                24
        );

        ScrollReelPlan plan =
                ScrollReelPlan.calculate(strip, template);

        assertThat(plan.scrollDistance()).isEqualTo(15500);
        assertThat(plan.totalDurationSeconds()).isEqualTo(31.6);
        assertThat(plan.scrollDurationSeconds()).isEqualTo(29.6);
    }

    @Test
    @DisplayName("사진 한 장으로 스크롤할 거리가 없으면 고정 화면 계획을 만든다")
    void createsStillPlanForSinglePhoto() {
        FilmStrip strip = new FilmStrip(
                Path.of("strip.png"),
                1080,
                1920,
                1
        );

        ScrollReelPlan plan =
                ScrollReelPlan.calculate(strip, template);

        assertThat(plan.scrollDistance()).isZero();
        assertThat(plan.totalDurationSeconds()).isEqualTo(10.9);
    }
}
