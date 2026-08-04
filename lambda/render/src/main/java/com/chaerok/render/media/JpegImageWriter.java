package com.chaerok.render.media;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public final class JpegImageWriter {

    private static final float DEFAULT_QUALITY = 0.92f;

    public void write(BufferedImage image, Path destination) {
        if (image == null) {
            throw new IllegalArgumentException("Image is required.");
        }

        try {
            Files.createDirectories(destination.getParent());
        } catch (IOException exception) {
            throw new MediaGenerationException(
                    "Failed to create JPEG directory.",
                    exception
            );
        }

        Iterator<ImageWriter> writers =
                ImageIO.getImageWritersByFormatName("jpg");

        if (!writers.hasNext()) {
            throw new MediaGenerationException(
                    "JPEG writer is not available."
            );
        }

        ImageWriter writer = writers.next();

        try (ImageOutputStream output =
                     ImageIO.createImageOutputStream(
                             Files.newOutputStream(destination)
                     )) {
            writer.setOutput(output);

            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(DEFAULT_QUALITY);
            }

            writer.write(
                    null,
                    new IIOImage(image, null, null),
                    params
            );
            output.flush();
        } catch (IOException exception) {
            throw new MediaGenerationException(
                    "Failed to write JPEG: " + destination,
                    exception
            );
        } finally {
            writer.dispose();
        }
    }
}
