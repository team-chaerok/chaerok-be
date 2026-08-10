package com.chaerok.backend.filter.engine;

import java.awt.image.BufferedImage;

public interface ImageProcessor {

    BufferedImage process(
            BufferedImage image,
            double strength
    );
}