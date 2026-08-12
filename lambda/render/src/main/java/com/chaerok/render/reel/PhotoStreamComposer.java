package com.chaerok.render.reel;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PhotoStreamComposer {

    static final int MAX_PHOTO_COUNT = 24;
    static final int PHOTO_X = 170;
    static final int PHOTO_WIDTH = 774;
    static final int PHOTO_HEIGHT = 516;
    static final int SEPARATOR_HEIGHT = 18;
    static final int TOP_MARGIN = 76;
    static final int BOTTOM_MARGIN = 81;
    static final int STACK_TOP_Y = 72;

    private static final int INTRO_VISIBLE_COUNT = 4;

    private static final Color FILM_COLOR =
            new Color(9, 9, 9, 255);
    private static final Color RAIL_TEXT_COLOR =
            new Color(214, 187, 132, 235);
    private static final Color RAIL_MUTED_COLOR =
            new Color(178, 151, 105, 190);
    private static final Color SEPARATOR_COLOR =
            new Color(7, 7, 7, 255);

    private static final Font FRAME_NUMBER_FONT =
            new Font(Font.SANS_SERIF, Font.BOLD, 26);
    private static final Font RAIL_LABEL_FONT =
            new Font(Font.SANS_SERIF, Font.BOLD, 18);

    public PhotoStream compose(
            List<Path> orderedPhotos,
            ReelTemplate template,
            Path destination
    ) {
        validate(orderedPhotos, template, destination);

        int pitch = PHOTO_HEIGHT + SEPARATOR_HEIGHT;
        int streamHeight = TOP_MARGIN
                + PHOTO_HEIGHT * orderedPhotos.size()
                + SEPARATOR_HEIGHT * (orderedPhotos.size() - 1)
                + BOTTOM_MARGIN;

        BufferedImage stream = new BufferedImage(
                template.panelWidth(),
                streamHeight,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = stream.createGraphics();
        try {
            fillFilmBackground(graphics, stream);

            List<Path> reverseOrdered =
                    new ArrayList<>(orderedPhotos);
            Collections.reverse(reverseOrdered);

            for (int index = 0;
                 index < reverseOrdered.size();
                 index++) {
                int cellY = TOP_MARGIN + index * pitch;
                int photoNumber = orderedPhotos.size() - index;

                BufferedImage photo =
                        readImage(reverseOrdered.get(index));

                drawContained(
                        graphics,
                        photo,
                        PHOTO_X,
                        cellY,
                        PHOTO_WIDTH,
                        PHOTO_HEIGHT
                );

                drawMovingRails(
                        graphics,
                        cellY,
                        photoNumber,
                        template.panelWidth()
                );

                if (index < reverseOrdered.size() - 1) {
                    drawSeparator(
                            graphics,
                            cellY + PHOTO_HEIGHT,
                            template.panelWidth()
                    );
                }
            }
        } finally {
            graphics.dispose();
        }

        writePng(stream, destination);

        List<Path> introSnapshots = writeIntroSnapshots(
                orderedPhotos,
                template,
                destination
        );

        int firstPhotoOffset = TOP_MARGIN
                + (orderedPhotos.size() - 1) * pitch;

        return new PhotoStream(
                destination,
                introSnapshots,
                stream.getWidth(),
                stream.getHeight(),
                orderedPhotos.size(),
                pitch,
                firstPhotoOffset
        );
    }

    private List<Path> writeIntroSnapshots(
            List<Path> orderedPhotos,
            ReelTemplate template,
            Path destination
    ) {
        int introCount = Math.min(
                INTRO_VISIBLE_COUNT,
                orderedPhotos.size()
        );
        int pitch = PHOTO_HEIGHT + SEPARATOR_HEIGHT;
        List<Path> snapshots = new ArrayList<>();

        for (int visibleCount = 1;
             visibleCount <= introCount;
             visibleCount++) {
            BufferedImage panel = new BufferedImage(
                    template.panelWidth(),
                    template.panelHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D panelGraphics = panel.createGraphics();
            try {
                fillFilmBackground(panelGraphics, panel);

                for (int photoNumber = 1;
                     photoNumber <= introCount;
                     photoNumber++) {
                    int slotIndex = introCount - photoNumber;
                    int cellY = TOP_MARGIN + slotIndex * pitch;

                    panelGraphics.setColor(Color.BLACK);
                    panelGraphics.fillRect(
                            PHOTO_X,
                            cellY,
                            PHOTO_WIDTH,
                            PHOTO_HEIGHT
                    );

                    drawMovingRails(
                            panelGraphics,
                            cellY,
                            photoNumber,
                            template.panelWidth()
                    );

                    if (photoNumber <= visibleCount) {
                        BufferedImage photo =
                                readImage(
                                        orderedPhotos.get(
                                                photoNumber - 1
                                        )
                                );
                        drawContained(
                                panelGraphics,
                                photo,
                                PHOTO_X,
                                cellY,
                                PHOTO_WIDTH,
                                PHOTO_HEIGHT
                        );
                    }

                    if (slotIndex < introCount - 1) {
                        drawSeparator(
                                panelGraphics,
                                cellY + PHOTO_HEIGHT,
                                template.panelWidth()
                        );
                    }
                }
            } finally {
                panelGraphics.dispose();
            }

            BufferedImage canvas = renderPanelToCanvas(
                    panel,
                    template
            );

            Path snapshot = destination.resolveSibling(
                    destination.getFileName().toString()
                            + ".intro-"
                            + visibleCount
                            + ".png"
            );
            writePng(canvas, snapshot);
            snapshots.add(snapshot);
        }

        return List.copyOf(snapshots);
    }

    private BufferedImage renderPanelToCanvas(
            BufferedImage panel,
            ReelTemplate template
    ) {
        BufferedImage canvas = new BufferedImage(
                template.canvasWidth(),
                template.canvasHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        double scale = template.canvasHeight()
                / (double) template.panelHeight();
        int renderedWidth = (int) Math.round(
                template.panelWidth() * scale
        );
        int renderedHeight = template.canvasHeight();
        int x = (template.canvasWidth() - renderedWidth) / 2;
        int renderedTopMargin = (int) Math.round(
                TOP_MARGIN * scale
        );
        int y = STACK_TOP_Y - renderedTopMargin;

        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(
                    0,
                    0,
                    canvas.getWidth(),
                    canvas.getHeight()
            );
            applyQualityHints(graphics);
            graphics.drawImage(
                    panel,
                    x,
                    y,
                    renderedWidth,
                    renderedHeight,
                    null
            );
        } finally {
            graphics.dispose();
        }

        return canvas;
    }

    private void fillFilmBackground(
            Graphics2D graphics,
            BufferedImage image
    ) {
        graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(FILM_COLOR);
        graphics.fillRect(
                0,
                0,
                image.getWidth(),
                image.getHeight()
        );
        graphics.setComposite(AlphaComposite.SrcOver);
        applyQualityHints(graphics);
    }

    private void drawSeparator(
            Graphics2D graphics,
            int separatorY,
            int panelWidth
    ) {
        graphics.setColor(SEPARATOR_COLOR);
        graphics.fillRect(
                0,
                separatorY,
                panelWidth,
                SEPARATOR_HEIGHT
        );
    }

    private void drawMovingRails(
            Graphics2D graphics,
            int cellY,
            int photoNumber,
            int panelWidth
    ) {
        graphics.setColor(RAIL_TEXT_COLOR);
        graphics.setFont(FRAME_NUMBER_FONT);
        graphics.drawString(
                String.format(Locale.ROOT, "%02d", photoNumber),
                52,
                cellY + 64
        );

        graphics.setFont(RAIL_LABEL_FONT);
        AffineTransform originalTransform =
                graphics.getTransform();
        try {
            graphics.rotate(
                    -Math.PI / 2.0,
                    105,
                    cellY + PHOTO_HEIGHT / 2.0
            );
            graphics.drawString(
                    "CHAEROK 400",
                    40,
                    cellY + PHOTO_HEIGHT / 2 + 6
            );
        } finally {
            graphics.setTransform(originalTransform);
        }

        int rightCenterX = panelWidth - 68;
        int triangleCenterY = cellY + PHOTO_HEIGHT / 2;

        Polygon triangle = new Polygon(
                new int[]{
                        rightCenterX - 9,
                        rightCenterX + 9,
                        rightCenterX
                },
                new int[]{
                        triangleCenterY - 8,
                        triangleCenterY - 8,
                        triangleCenterY + 8
                },
                3
        );
        graphics.fillPolygon(triangle);

        graphics.setColor(RAIL_MUTED_COLOR);
        graphics.fillRect(
                PHOTO_X - 13,
                cellY,
                2,
                PHOTO_HEIGHT
        );
        graphics.fillRect(
                PHOTO_X + PHOTO_WIDTH + 11,
                cellY,
                2,
                PHOTO_HEIGHT
        );

        int markY = cellY + 122;
        for (int index = 0; index < 3; index++) {
            graphics.fillRect(
                    panelWidth - 38,
                    markY + index * 22,
                    12,
                    3
            );
        }
    }

    private void validate(
            List<Path> orderedPhotos,
            ReelTemplate template,
            Path destination
    ) {
        if (orderedPhotos == null || orderedPhotos.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one photo is required."
            );
        }
        if (orderedPhotos.size() > MAX_PHOTO_COUNT) {
            throw new IllegalArgumentException(
                    "Photo stream supports at most 24 photos."
            );
        }
        if (template == null) {
            throw new IllegalArgumentException(
                    "template is required."
            );
        }
        if (destination == null) {
            throw new IllegalArgumentException(
                    "destination is required."
            );
        }
        if (template.panelWidth() < PHOTO_X + PHOTO_WIDTH) {
            throw new IllegalArgumentException(
                    "Template panel is too narrow for the film strip."
            );
        }
    }

    private BufferedImage readImage(Path path) {
        if (path == null || Files.notExists(path)) {
            throw new IllegalArgumentException(
                    "Filtered photo does not exist: " + path
            );
        }

        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                throw new IllegalArgumentException(
                        "Filtered photo is not a readable image: "
                                + path
                );
            }
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read filtered photo: " + path,
                    exception
            );
        }
    }

    private void drawContained(
            Graphics2D graphics,
            BufferedImage source,
            int x,
            int y,
            int width,
            int height
    ) {
        double scale = Math.min(
                width / (double) source.getWidth(),
                height / (double) source.getHeight()
        );

        int drawWidth = Math.max(
                1,
                (int) Math.round(source.getWidth() * scale)
        );
        int drawHeight = Math.max(
                1,
                (int) Math.round(source.getHeight() * scale)
        );
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;

        graphics.drawImage(
                source,
                drawX,
                drawY,
                drawWidth,
                drawHeight,
                null
        );
    }

    private void applyQualityHints(Graphics2D graphics) {
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
        graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
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
            if (!ImageIO.write(image, "png", destination.toFile())) {
                throw new IllegalStateException(
                        "PNG writer is unavailable."
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to write reel PNG.",
                    exception
            );
        }
    }
}