package com.chaerok.render.reel;

import com.chaerok.render.media.MediaGenerationException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FilmStripComposer {

    private static final int MAX_PHOTO_COUNT = 24;

    public FilmStrip compose(
            List<Path> orderedPhotos,
            ReelTemplate template,
            BufferedImage overlay,
            Path destination
    ) {
        validateInputs(
                orderedPhotos,
                template,
                overlay,
                destination
        );

        int naturalHeight = calculateNaturalHeight(
                orderedPhotos.size(),
                template
        );
        int stripHeight = Math.max(
                naturalHeight,
                template.canvasHeight()
        );
        int verticalOffset = (stripHeight - naturalHeight) / 2;

        BufferedImage strip = new BufferedImage(
                template.cellWidth(),
                stripHeight,
                BufferedImage.TYPE_3BYTE_BGR
        );

        Graphics2D graphics = strip.createGraphics();
        try {
            configure(graphics);
            graphics.setColor(Color.BLACK);
            graphics.fillRect(
                    0,
                    0,
                    strip.getWidth(),
                    strip.getHeight()
            );

            int cellY = verticalOffset + template.topPadding();
            for (Path photoPath : orderedPhotos) {
                drawCell(
                        graphics,
                        photoPath,
                        template,
                        overlay,
                        cellY
                );
                cellY += template.cellHeight();
            }
        } finally {
            graphics.dispose();
        }

        writePng(strip, destination);

        return new FilmStrip(
                destination,
                strip.getWidth(),
                strip.getHeight(),
                orderedPhotos.size()
        );
    }

    private void drawCell(
            Graphics2D stripGraphics,
            Path photoPath,
            ReelTemplate template,
            BufferedImage overlay,
            int cellY
    ) {
        BufferedImage photo = readImage(photoPath);
        BufferedImage cell = new BufferedImage(
                template.cellWidth(),
                template.cellHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D cellGraphics = cell.createGraphics();
        try {
            configure(cellGraphics);
            cellGraphics.setColor(Color.BLACK);
            cellGraphics.fillRect(
                    0,
                    0,
                    cell.getWidth(),
                    cell.getHeight()
            );
            drawPhoto(cellGraphics, photo, template.photoSlot());
            cellGraphics.drawImage(overlay, 0, 0, null);
        } finally {
            cellGraphics.dispose();
        }

        stripGraphics.drawImage(cell, 0, cellY, null);
    }

    private void drawPhoto(
            Graphics2D graphics,
            BufferedImage photo,
            PhotoSlot slot
    ) {
        double scale = Math.min(
                (double) slot.width() / photo.getWidth(),
                (double) slot.height() / photo.getHeight()
        );

        int width = Math.max(
                1,
                (int) Math.round(photo.getWidth() * scale)
        );
        int height = Math.max(
                1,
                (int) Math.round(photo.getHeight() * scale)
        );
        int x = slot.x() + (slot.width() - width) / 2;
        int y = slot.y() + (slot.height() - height) / 2;

        graphics.drawImage(
                photo,
                x,
                y,
                width,
                height,
                null
        );
    }

    private BufferedImage readImage(Path source) {
        try {
            BufferedImage image = ImageIO.read(source.toFile());
            if (image == null) {
                throw new MediaGenerationException(
                        "Unsupported or invalid filtered photo: " + source
                );
            }
            return image;
        } catch (IOException exception) {
            throw new MediaGenerationException(
                    "Failed to read filtered photo: " + source,
                    exception
            );
        }
    }

    private void writePng(
            BufferedImage image,
            Path destination
    ) {
        try {
            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            boolean written = ImageIO.write(
                    image,
                    "png",
                    destination.toFile()
            );
            if (!written
                    || Files.notExists(destination)
                    || Files.size(destination) == 0L) {
                throw new MediaGenerationException(
                        "PNG writer did not create a valid film strip."
                );
            }
        } catch (MediaGenerationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new MediaGenerationException(
                    "Failed to write film strip PNG.",
                    exception
            );
        }
    }

    private int calculateNaturalHeight(
            int photoCount,
            ReelTemplate template
    ) {
        long height = (long) template.topPadding()
                + (long) template.cellHeight() * photoCount
                + template.bottomPadding();

        if (height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Film strip height exceeds supported image dimensions."
            );
        }
        return (int) height;
    }

    private void validateInputs(
            List<Path> orderedPhotos,
            ReelTemplate template,
            BufferedImage overlay,
            Path destination
    ) {
        if (orderedPhotos == null || orderedPhotos.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one filtered photo is required."
            );
        }
        if (orderedPhotos.size() > MAX_PHOTO_COUNT) {
            throw new IllegalArgumentException(
                    "Film strip supports at most "
                            + MAX_PHOTO_COUNT
                            + " photos."
            );
        }
        if (orderedPhotos.stream().anyMatch(path -> path == null)) {
            throw new IllegalArgumentException(
                    "Filtered photo path is required."
            );
        }
        if (template == null) {
            throw new IllegalArgumentException("template is required.");
        }
        if (overlay == null) {
            throw new IllegalArgumentException("overlay is required.");
        }
        if (destination == null) {
            throw new IllegalArgumentException("destination is required.");
        }
        if (overlay.getWidth() != template.cellWidth()
                || overlay.getHeight() != template.cellHeight()) {
            throw new IllegalArgumentException(
                    "Overlay dimensions must match reel cell dimensions."
            );
        }
    }

    private void configure(Graphics2D graphics) {
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
    }
}
