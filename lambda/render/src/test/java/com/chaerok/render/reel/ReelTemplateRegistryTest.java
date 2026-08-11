package com.chaerok.render.reel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReelTemplateRegistryTest {

    private final ReelTemplateRegistry registry =
            new ReelTemplateRegistry();

    @Test
    @DisplayName("공주 릴스 템플릿은 3대2 사진 슬롯 규격을 제공한다")
    void providesGongjuTemplate() {
        ReelTemplate template = registry.require(
                ReelTemplateRegistry.GONGJU_V1
        );

        assertThat(template.templateId()).isEqualTo("gongju-v1");
        assertThat(template.canvasWidth()).isEqualTo(1080);
        assertThat(template.canvasHeight()).isEqualTo(1920);
        assertThat(template.cellWidth()).isEqualTo(1080);
        assertThat(template.cellHeight()).isEqualTo(700);
        assertThat(template.photoSlot())
                .isEqualTo(new PhotoSlot(120, 70, 840, 560));
        assertThat(template.photoSlot().width() * 2)
                .isEqualTo(template.photoSlot().height() * 3);
        assertThat(template.overlayResource())
                .isEqualTo("reel/templates/gongju/film-cell-v1.png");
    }

    @Test
    @DisplayName("등록되지 않은 릴스 템플릿은 거부한다")
    void rejectsUnknownTemplate() {
        assertThatThrownBy(() -> registry.require("unknown-v1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported reel template");
    }
}
