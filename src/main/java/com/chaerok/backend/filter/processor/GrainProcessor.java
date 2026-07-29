package com.chaerok.backend.filter.processor;

import com.chaerok.backend.filter.engine.ImageProcessor;

import java.awt.image.BufferedImage;
import java.util.Random;

public class GrainProcessor implements ImageProcessor {

    private final double grain;
    private final Random random = new Random();

    public GrainProcessor(double grain) {
        this.grain = grain;
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

        double amount = grain * strength;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);

                int noise = (int) ((random.nextDouble() - 0.5) * amount);

                int r = clamp(((rgb >> 16) & 0xff) + noise);
                int g = clamp(((rgb >> 8) & 0xff) + noise);
                int b = clamp((rgb & 0xff) + noise);

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