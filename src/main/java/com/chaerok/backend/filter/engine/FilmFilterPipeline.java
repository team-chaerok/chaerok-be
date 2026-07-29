package com.chaerok.backend.filter.engine;

import java.awt.image.BufferedImage;
import java.util.List;

public class FilmFilterPipeline {

    private final List<ImageProcessor> processors;

    public FilmFilterPipeline(List<ImageProcessor> processors) {
        this.processors = processors;
    }

    public BufferedImage apply(
            BufferedImage image,
            double strength
    ) {
        BufferedImage current = image;

        for (ImageProcessor processor : processors) {
            current = processor.process(current, strength);
        }

        return current;
    }
}