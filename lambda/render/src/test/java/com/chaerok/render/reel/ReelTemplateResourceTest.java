package com.chaerok.render.reel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ReelTemplateResourceTest {

    @Test
    @DisplayName("4컷 필름 PNG는 패널 규격과 투명 사진 슬롯을 가진다")
    void validatesFourCutOverlayResource() throws Exception {
        ReelTemplate template =
                new ReelTemplateRegistry().require(
                        ReelTemplateRegistry.GONGJU_V1
                );

        ClassLoader classLoader = Thread.currentThread()
                .getContextClassLoader();
        assertThat(classLoader).isNotNull();

        try (InputStream input = classLoader.getResourceAsStream(
                template.overlayResource()
        )) {
            assertThat(input).isNotNull();

            BufferedImage overlay = ImageIO.read(input);
            assertThat(overlay).isNotNull();
            assertThat(overlay.getWidth())
                    .isEqualTo(template.panelWidth());
            assertThat(overlay.getHeight())
                    .isEqualTo(template.panelHeight());

            for (PhotoSlot slot : template.photoSlots()) {
                int centerX = slot.x() + slot.width() / 2;
                int centerY = slot.y() + slot.height() / 2;
                int alpha = (overlay.getRGB(centerX, centerY) >>> 24)
                        & 0xFF;
                assertThat(alpha).isZero();
            }
        }
    }
}