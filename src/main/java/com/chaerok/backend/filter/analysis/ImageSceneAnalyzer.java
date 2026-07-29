package com.chaerok.backend.filter.analysis;

import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

@Component
public class ImageSceneAnalyzer {

    private static final int MAX_SAMPLE_COUNT = 15_000;

    public ImageAnalysis analyze(
            BufferedImage image,
            boolean hasFace,
            SceneType forcedSceneType
    ) {
        if (image == null) {
            throw new IllegalArgumentException("이미지가 null입니다.");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        long totalPixelCount = (long) width * height;

        int step = Math.max(
                1,
                (int) Math.sqrt(
                        totalPixelCount / (double) MAX_SAMPLE_COUNT
                )
        );

        double luminanceSum = 0.0;
        double luminanceSquareSum = 0.0;

        long darkPixelCount = 0;
        long highlightPixelCount = 0;
        long sampledPixelCount = 0;

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int rgb = image.getRGB(x, y);

                int red = (rgb >> 16) & 0xff;
                int green = (rgb >> 8) & 0xff;
                int blue = rgb & 0xff;

                double luminance = (
                        0.2126 * red
                                + 0.7152 * green
                                + 0.0722 * blue
                ) / 255.0;

                luminanceSum += luminance;
                luminanceSquareSum += luminance * luminance;

                if (luminance < 0.20) {
                    darkPixelCount++;
                }

                if (luminance > 0.85) {
                    highlightPixelCount++;
                }

                sampledPixelCount++;
            }
        }

        if (sampledPixelCount == 0) {
            return new ImageAnalysis(
                    0.5,
                    0.0,
                    0.0,
                    0.0,
                    hasFace,
                    resolveSceneType(
                            0.5,
                            0.0,
                            hasFace,
                            forcedSceneType
                    )
            );
        }

        double brightness =
                luminanceSum / sampledPixelCount;

        double darkPixelRatio =
                darkPixelCount / (double) sampledPixelCount;

        double highlightPixelRatio =
                highlightPixelCount / (double) sampledPixelCount;

        double variance =
                luminanceSquareSum / sampledPixelCount
                        - brightness * brightness;

        double contrast =
                Math.sqrt(Math.max(0.0, variance));

        SceneType sceneType = resolveSceneType(
                brightness,
                darkPixelRatio,
                hasFace,
                forcedSceneType
        );

        return new ImageAnalysis(
                brightness,
                darkPixelRatio,
                highlightPixelRatio,
                contrast,
                hasFace,
                sceneType
        );
    }

    private SceneType resolveSceneType(
            double brightness,
            double darkPixelRatio,
            boolean hasFace,
            SceneType forcedSceneType
    ) {
        // Postman 테스트 등에서 장면을 강제로 지정한 경우
        if (forcedSceneType != null) {
            return forcedSceneType;
        }

        /*
         * 단순히 평균 밝기만 보는 것보다
         * 어두운 픽셀 비율을 함께 보는 편이 정확합니다.
         */
        boolean isNight =
                brightness < 0.27
                        || (
                        brightness < 0.38
                                && darkPixelRatio > 0.58
                );

        // 야간 보호 처리를 우선합니다.
        if (isNight) {
            return SceneType.NIGHT;
        }

        if (hasFace) {
            return SceneType.PORTRAIT;
        }

        return SceneType.LANDSCAPE;
    }
}