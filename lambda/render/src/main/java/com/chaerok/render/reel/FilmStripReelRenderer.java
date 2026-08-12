package com.chaerok.render.reel;

import com.chaerok.render.media.MediaGenerationException;
import com.chaerok.render.media.ReelRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public final class FilmStripReelRenderer implements ReelRenderer {

    private final FilterTemplateSelector templateSelector;
    private final FilmStripComposer filmStripComposer;
    private final FfmpegFilmStripRenderer ffmpegRenderer;

    public FilmStripReelRenderer(String ffmpegPath) {
        ReelTemplateRegistry registry = new ReelTemplateRegistry();
        this.templateSelector = new FilterTemplateSelector(registry);
        this.filmStripComposer = new FilmStripComposer();
        this.ffmpegRenderer = new FfmpegFilmStripRenderer(ffmpegPath);
    }

    @Override
    public void render(
            Path filteredDirectory,
            int photoCount,
            Path destination
    ) {
        throw new UnsupportedOperationException(
                "filterId and ordered photo paths are required "
                        + "for film strip reel rendering."
        );
    }

    @Override
    public void render(
            String filterId,
            List<Path> orderedPhotos,
            Path destination
    ) {
        if (orderedPhotos == null || orderedPhotos.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one filtered photo is required."
            );
        }
        if (destination == null) {
            throw new IllegalArgumentException(
                    "Reel destination is required."
            );
        }

        ReelTemplate template = templateSelector.select(filterId);
        BufferedImage overlay = loadOverlay(template.overlayResource());

        Path filmStripPath = destination.resolveSibling(
                destination.getFileName().toString()
                        + ".film-strip.png"
        );

        FilmStrip filmStrip = filmStripComposer.compose(
                List.copyOf(orderedPhotos),
                template,
                overlay,
                filmStripPath
        );

        ffmpegRenderer.render(
                filmStrip,
                template,
                destination
        );
    }

    private BufferedImage loadOverlay(String resourcePath) {
        ClassLoader classLoader = Thread.currentThread()
                .getContextClassLoader();
        if (classLoader == null) {
            classLoader = FilmStripReelRenderer.class.getClassLoader();
        }

        try (InputStream input =
                     classLoader.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new MediaGenerationException(
                        "Reel overlay resource was not found: "
                                + resourcePath
                );
            }

            BufferedImage overlay = ImageIO.read(input);
            if (overlay == null) {
                throw new MediaGenerationException(
                        "Reel overlay resource is not a readable image: "
                                + resourcePath
                );
            }
            return overlay;
        } catch (IOException exception) {
            throw new MediaGenerationException(
                    "Failed to load reel overlay resource: "
                            + resourcePath,
                    exception
            );
        }
    }
}