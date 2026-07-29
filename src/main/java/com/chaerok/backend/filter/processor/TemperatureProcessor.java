package com.chaerok.backend.filter.processor;

import com.chaerok.backend.filter.engine.ImageProcessor;

import java.awt.image.BufferedImage;

public class TemperatureProcessor implements ImageProcessor {

    private final double temperature;

    public TemperatureProcessor(double temperature) {
        this.temperature = temperature;
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

        double value = temperature * strength;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);

                int r = clamp(((rgb >> 16) & 0xff) + value);
                int g = clamp(((rgb >> 8) & 0xff) + (value * 0.25));
                int b = clamp((rgb & 0xff) - value);

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