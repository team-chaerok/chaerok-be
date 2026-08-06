package com.chaerok.backend.filter.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class ImageSceneAnalyzerTest {

    private final ImageSceneAnalyzer analyzer =
            new ImageSceneAnalyzer();

    @Test
    @DisplayName("밝은 사진은 풍경 장면으로 자동 분류한다")
    void classifiesBrightImageAsLandscape() {
        ImageAnalysis analysis = analyzer.analyze(
                solidImage(new Color(220, 220, 220))
        );

        assertThat(analysis.sceneType())
                .isEqualTo(SceneType.LANDSCAPE);
    }

    @Test
    @DisplayName("어두운 사진은 야간 장면으로 자동 분류한다")
    void classifiesDarkImageAsNight() {
        ImageAnalysis analysis = analyzer.analyze(
                solidImage(new Color(20, 20, 20))
        );

        assertThat(analysis.sceneType())
                .isEqualTo(SceneType.NIGHT);
    }

    private BufferedImage solidImage(Color color) {
        BufferedImage image = new BufferedImage(
                10,
                10,
                BufferedImage.TYPE_INT_RGB
        );

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
        return image;
    }
}
