package com.chaerok.backend.filter.processor;

import com.chaerok.backend.filter.engine.ImageProcessor;

import java.awt.image.BufferedImage;

public class ContrastProcessor implements ImageProcessor {

    private final double contrast;

    public ContrastProcessor(double contrast) {
        this.contrast = contrast;
    }

    @Override
    public BufferedImage process(
            BufferedImage image,
            double strength
    ) {
        BufferedImage result = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        double mixedContrast = 1 + ((contrast - 1) * strength);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);

                int r = apply((rgb >> 16) & 0xff, mixedContrast);
                int g = apply((rgb >> 8) & 0xff, mixedContrast);
                int b = apply(rgb & 0xff, mixedContrast);

                result.setRGB(x, y, toRgb(r, g, b));
            }
        }

        return result;
    }

    private int apply(int value, double contrast) {
        return clamp(((value - 128) * contrast) + 128);
    }

    private int clamp(double value) {
        return (int) Math.max(0, Math.min(255, value));
    }

    private int toRgb(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }
}