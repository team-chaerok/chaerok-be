package com.chaerok.backend.filter.processor;

import com.chaerok.backend.filter.engine.ImageProcessor;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class OverlayProcessor implements ImageProcessor {

    private final OverlayImageCache overlayImageCache;
    private final String overlayPath;
    private final double opacity;
    private final BlendMode blendMode;

    public OverlayProcessor(
            OverlayImageCache overlayImageCache,
            String overlayPath,
            double opacity,
            String blendMode
    ) {
        this.overlayImageCache = overlayImageCache;
        this.overlayPath = overlayPath;
        this.opacity = clamp01(opacity);
        this.blendMode = BlendMode.from(blendMode);
    }

    @Override
    public BufferedImage process(
            BufferedImage image,
            double strength
    ) {
        if (image == null) {
            throw new IllegalArgumentException(
                    "오버레이를 적용할 이미지가 비어 있습니다."
            );
        }

        if (overlayPath == null || overlayPath.isBlank()) {
            return image;
        }

        /*
         * 밝기·풍경·인물·야간에 따른 자동 배율은
         * FilmFilterEngine에서 이미 strength에 반영됩니다.
         *
         * 따라서 이 클래스에서는 전달받은 strength만 사용합니다.
         */
        double safeStrength = Math.max(0.0, strength);

        double finalOpacity = clamp01(
                opacity * safeStrength
        );

        System.out.printf(
                """
                [Overlay Processor]
                path=%s
                blendMode=%s
                baseOpacity=%.3f
                adaptiveStrength=%.3f
                finalOpacity=%.3f
                """,
                overlayPath,
                blendMode,
                opacity,
                safeStrength,
                finalOpacity
        );

        if (finalOpacity <= 0.0) {
            return image;
        }

        BufferedImage overlay =
                overlayImageCache.getOverlay(overlayPath);

        if (overlay == null) {
            throw new IllegalStateException(
                    "오버레이 이미지를 불러오지 못했습니다: "
                            + overlayPath
            );
        }

        BufferedImage resizedOverlay = resize(
                overlay,
                image.getWidth(),
                image.getHeight()
        );

        return blend(
                image,
                resizedOverlay,
                finalOpacity,
                blendMode
        );
    }

    /**
     * 오버레이 이미지를 원본 이미지 크기에 맞게 변경합니다.
     *
     * PNG 투명도를 보존하기 위해 TYPE_INT_ARGB를 사용합니다.
     */
    private BufferedImage resize(
            BufferedImage source,
            int width,
            int height
    ) {
        BufferedImage resized = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = resized.createGraphics();

        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            graphics.drawImage(
                    source,
                    0,
                    0,
                    width,
                    height,
                    null
            );
        } finally {
            graphics.dispose();
        }

        return resized;
    }

    /**
     * 원본 이미지와 오버레이 이미지를 픽셀 단위로 합성합니다.
     */
    private BufferedImage blend(
            BufferedImage base,
            BufferedImage overlay,
            double opacity,
            BlendMode blendMode
    ) {
        int width = base.getWidth();
        int height = base.getHeight();

        BufferedImage result = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int baseArgb = base.getRGB(x, y);
                int overlayArgb = overlay.getRGB(x, y);

                int baseR = (baseArgb >> 16) & 0xff;
                int baseG = (baseArgb >> 8) & 0xff;
                int baseB = baseArgb & 0xff;

                int overlayAlpha =
                        (overlayArgb >>> 24) & 0xff;

                int overlayR =
                        (overlayArgb >> 16) & 0xff;

                int overlayG =
                        (overlayArgb >> 8) & 0xff;

                int overlayB =
                        overlayArgb & 0xff;

                /*
                 * PNG 자체의 투명도와
                 * 프리셋의 오버레이 투명도를 함께 반영합니다.
                 */
                double pixelOpacity =
                        opacity * (overlayAlpha / 255.0);

                int resultR = blendChannel(
                        baseR,
                        overlayR,
                        pixelOpacity,
                        blendMode
                );

                int resultG = blendChannel(
                        baseG,
                        overlayG,
                        pixelOpacity,
                        blendMode
                );

                int resultB = blendChannel(
                        baseB,
                        overlayB,
                        pixelOpacity,
                        blendMode
                );

                result.setRGB(
                        x,
                        y,
                        toRgb(resultR, resultG, resultB)
                );
            }
        }

        return result;
    }

    private int blendChannel(
            int base,
            int overlay,
            double opacity,
            BlendMode blendMode
    ) {
        int blended = switch (blendMode) {
            case NORMAL -> overlay;
            case SCREEN -> screen(base, overlay);
            case OVERLAY -> overlay(base, overlay);
            case SOFT_LIGHT -> softLight(base, overlay);
            case MULTIPLY -> multiply(base, overlay);
        };

        double mixed =
                base * (1.0 - opacity)
                        + blended * opacity;

        return clamp(
                (int) Math.round(mixed)
        );
    }

    private int screen(int base, int overlay) {
        return 255 - (
                (255 - base)
                        * (255 - overlay)
                        / 255
        );
    }

    private int multiply(int base, int overlay) {
        return base * overlay / 255;
    }

    private int overlay(int base, int overlay) {
        if (base < 128) {
            return 2 * base * overlay / 255;
        }

        return 255 - (
                2
                        * (255 - base)
                        * (255 - overlay)
                        / 255
        );
    }

    private int softLight(int base, int overlay) {
        double baseNormalized =
                base / 255.0;

        double overlayNormalized =
                overlay / 255.0;

        double result;

        if (overlayNormalized < 0.5) {
            result =
                    baseNormalized
                            - (
                            1.0
                                    - 2.0 * overlayNormalized
                    )
                            * baseNormalized
                            * (1.0 - baseNormalized);
        } else {
            double curve;

            if (baseNormalized < 0.25) {
                curve =
                        (
                                (
                                        16.0 * baseNormalized
                                                - 12.0
                                )
                                        * baseNormalized
                                        + 4.0
                        )
                                * baseNormalized;
            } else {
                curve = Math.sqrt(baseNormalized);
            }

            result =
                    baseNormalized
                            + (
                            2.0 * overlayNormalized
                                    - 1.0
                    )
                            * (curve - baseNormalized);
        }

        return clamp(
                (int) Math.round(result * 255.0)
        );
    }

    private int toRgb(int red, int green, int blue) {
        return (red << 16)
                | (green << 8)
                | blue;
    }

    private int clamp(int value) {
        return Math.max(
                0,
                Math.min(255, value)
        );
    }

    private double clamp01(double value) {
        return Math.max(
                0.0,
                Math.min(1.0, value)
        );
    }
}