package com.chaerok.backend.filter.processor;

import com.chaerok.backend.filter.engine.ImageProcessor;

import java.awt.image.BufferedImage;

public class VignetteProcessor implements ImageProcessor {

    private final double vignette;

    public VignetteProcessor(double vignette) {
        this.vignette = vignette;
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

        int width = image.getWidth();
        int height = image.getHeight();

        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double maxDistance = Math.sqrt(centerX * centerX + centerY * centerY);

        double amount = vignette * strength / 100.0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                double dx = x - centerX;
                double dy = y - centerY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                double ratio = distance / maxDistance;

                double darken = 1 - (ratio * ratio * amount);

                int r = clamp(((rgb >> 16) & 0xff) * darken);
                int g = clamp(((rgb >> 8) & 0xff) * darken);
                int b = clamp((rgb & 0xff) * darken);

                result.setRGB(x, y, toRgb(r, g, b));
            }
        }

        return result;
    }

    private int clamp(double value) {
        return (int) Math.max(0, Math.min(255, value));
    }

    private int toRgb(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }
}