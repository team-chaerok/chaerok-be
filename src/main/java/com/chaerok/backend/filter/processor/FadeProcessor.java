package com.chaerok.backend.filter.processor;

import com.chaerok.backend.filter.engine.ImageProcessor;

import java.awt.image.BufferedImage;

public class FadeProcessor implements ImageProcessor {

    private final double fade;

    public FadeProcessor(double fade) {
        this.fade = fade;
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

        double value = fade * strength;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);

                int r = fadeChannel((rgb >> 16) & 0xff, value);
                int g = fadeChannel((rgb >> 8) & 0xff, value);
                int b = fadeChannel(rgb & 0xff, value);

                result.setRGB(x, y, toRgb(r, g, b));
            }
        }

        return result;
    }

    private int fadeChannel(int value, double fade) {
        return clamp(value + ((255 - value) * fade / 100));
    }

    private int clamp(double value) {
        return (int) Math.max(0, Math.min(255, value));
    }

    private int toRgb(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }
}