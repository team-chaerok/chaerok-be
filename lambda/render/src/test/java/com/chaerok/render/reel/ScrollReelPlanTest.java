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
    @DisplayName("4컷 필름 패널 한 개의 스크롤 거리와 재생 시간을 계산한다")
    void calculatesSinglePanelPlan() {
        FilmStrip strip = new FilmStrip(
                Path.of("strip.png"),
                1080,
                2276,
                4
        );

        ScrollReelPlan plan =
                ScrollReelPlan.calculate(strip, template);

        assertThat(plan.scrollDistance()).isEqualTo(356);
        assertThat(plan.totalDurationSeconds()).isEqualTo(13.6);
        assertThat(plan.startHoldSeconds()).isEqualTo(0.8);
        assertThat(plan.endHoldSeconds()).isEqualTo(1.2);
        assertThat(plan.scrollDurationSeconds()).isEqualTo(11.6);
        assertThat(plan.scrollEndSeconds()).isEqualTo(12.4);
    }

    @Test
    @DisplayName("사진 스물네 장은 4컷 패널 여섯 개 높이로 계산한다")
    void calculatesTwentyFourPhotoPlan() {
        FilmStrip strip = new FilmStrip(
                Path.of("strip.png"),
                1080,
                13656,
                24
        );

        ScrollReelPlan plan =
                ScrollReelPlan.calculate(strip, template);

        assertThat(plan.scrollDistance()).isEqualTo(11736);
        assertThat(plan.totalDurationSeconds()).isEqualTo(31.6);
        assertThat(plan.scrollDurationSeconds()).isEqualTo(29.6);
    }

    @Test
    @DisplayName("스트립 높이가 캔버스와 같으면 고정 화면 계획을 만든다")
    void createsStillPlanWhenStripMatchesCanvas() {
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
