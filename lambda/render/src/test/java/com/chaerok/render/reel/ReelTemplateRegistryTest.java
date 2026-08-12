package com.chaerok.render.reel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReelTemplateRegistryTest {

    private final ReelTemplateRegistry registry =
            new ReelTemplateRegistry();

    @Test
    @DisplayName("공주 릴스 템플릿은 3대2 사진 슬롯 네 개를 제공한다")
    void providesGongjuTemplate() {
        ReelTemplate template = registry.require(
                ReelTemplateRegistry.GONGJU_V1
        );

        assertThat(template.templateId()).isEqualTo("gongju-v1");
        assertThat(template.canvasWidth()).isEqualTo(1080);
        assertThat(template.canvasHeight()).isEqualTo(1920);
        assertThat(template.panelWidth()).isEqualTo(1080);
        assertThat(template.panelHeight()).isEqualTo(2276);
        assertThat(template.photosPerPanel()).isEqualTo(4);
        assertThat(template.photoSlots()).containsExactlyElementsOf(
                List.of(
                        new PhotoSlot(170, 76, 774, 516),
                        new PhotoSlot(170, 606, 774, 516),
                        new PhotoSlot(170, 1133, 774, 516),
                        new PhotoSlot(170, 1659, 774, 516)
                )
        );
        assertThat(template.photoSlots())
                .allSatisfy(slot -> assertThat(slot.width() * 2)
                        .isEqualTo(slot.height() * 3));
        assertThat(template.overlayResource())
                .isEqualTo(
                        "reel/templates/gongju/film-panel-4cut-v1.png"
                );
    }

    @Test
    @DisplayName("등록되지 않은 릴스 템플릿은 거부한다")
    void rejectsUnknownTemplate() {
        assertThatThrownBy(() -> registry.require("unknown-v1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported reel template");
    }
}
