package com.chaerok.backend.filter.analysis;

import java.awt.image.BufferedImage;

public final class ImageBrightnessAnalyzer {

    private static final int MAX_SAMPLE_COUNT = 10_000;

    private ImageBrightnessAnalyzer() {
    }

    /**
     * 이미지 밝기를 0.0~1.0 범위로 반환합니다.
     *
     * 0.0: 완전한 검정
     * 1.0: 완전한 흰색
     */
    public static double calculate(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("이미지가 null입니다.");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("이미지 크기가 올바르지 않습니다.");
        }

        long pixelCount = (long) width * height;

        int step = Math.max(
                1,
                (int) Math.sqrt(pixelCount / (double) MAX_SAMPLE_COUNT)
        );

        long sampleCount = 0;
        double luminanceSum = 0.0;

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int rgb = image.getRGB(x, y);

                int red = (rgb >> 16) & 0xff;
                int green = (rgb >> 8) & 0xff;
                int blue = rgb & 0xff;

                /*
                 * 사람이 실제로 느끼는 밝기에 가깝게 계산합니다.
                 * 녹색에 가장 높은 가중치를 부여합니다.
                 */
                double luminance =
                        0.2126 * red
                                + 0.7152 * green
                                + 0.0722 * blue;

                luminanceSum += luminance;
                sampleCount++;
            }
        }

        if (sampleCount == 0) {
            return 0.5;
        }

        return clamp01(
                luminanceSum / sampleCount / 255.0
        );
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}