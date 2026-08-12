package com.chaerok.render.reel;

import com.chaerok.render.media.ReelRenderer;

import java.nio.file.Path;
import java.util.List;

public final class FilmStripReelRenderer implements ReelRenderer {

    private final FilterTemplateSelector templateSelector;
    private final PhotoStreamComposer photoStreamComposer;
    private final FfmpegPhotoStreamRenderer ffmpegRenderer;

    public FilmStripReelRenderer(String ffmpegPath) {
        ReelTemplateRegistry registry =
                new ReelTemplateRegistry();
        this.templateSelector =
                new FilterTemplateSelector(registry);
        this.photoStreamComposer =
                new PhotoStreamComposer();
        this.ffmpegRenderer =
                new FfmpegPhotoStreamRenderer(ffmpegPath);
    }

    @Override
    public void render(
            Path filteredDirectory,
            int photoCount,
            Path destination
    ) {
        throw new UnsupportedOperationException(
                "filterId and ordered photo paths are required "
                        + "for moving full film strip rendering."
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

        ReelTemplate template =
                templateSelector.select(filterId);

        Path streamPath = destination.resolveSibling(
                destination.getFileName().toString()
                        + ".moving-film-strip.png"
        );

        PhotoStream stream =
                photoStreamComposer.compose(
                        List.copyOf(orderedPhotos),
                        template,
                        streamPath
                );

        ffmpegRenderer.render(
                stream,
                template,
                destination
        );
    }
}