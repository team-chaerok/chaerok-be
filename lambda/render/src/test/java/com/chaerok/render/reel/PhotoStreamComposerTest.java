package com.chaerok.render.reel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoStreamComposerTest {

    @TempDir
    Path tempDirectory;

    @Test
    @DisplayName("전체 이동 필름과 메모리 경량 인트로 스냅샷 네 장을 만든다")
    void composesFilmAndIntroSnapshots() throws Exception {
        ReelTemplate template =
                new ReelTemplateRegistry().require(
                        ReelTemplateRegistry.GONGJU_V1
                );

        List<Path> photos = new ArrayList<>();
        photos.add(writeSolidPhoto("001.jpg", Color.RED));
        photos.add(writeSolidPhoto("002.jpg", Color.GREEN));
        photos.add(writeSolidPhoto("003.jpg", Color.BLUE));
        photos.add(writeSolidPhoto("004.jpg", Color.YELLOW));
        photos.add(writeSolidPhoto("005.jpg", Color.CYAN));
        photos.add(writeSolidPhoto("006.jpg", Color.MAGENTA));

        Path output =
                tempDirectory.resolve("moving-film-strip.png");

        PhotoStream stream =
                new PhotoStreamComposer().compose(
                        photos,
                        template,
                        output
                );

        assertThat(stream.photoCount()).isEqualTo(6);
        assertThat(stream.width()).isEqualTo(1080);
        assertThat(stream.pitch()).isEqualTo(534);
        assertThat(stream.introSnapshotPaths()).hasSize(4);
        assertThat(stream.introSnapshotPaths())
                .allSatisfy(path -> assertThat(path).exists());

        BufferedImage snapshot =
                ImageIO.read(
                        stream.introSnapshotPaths()
                                .get(3)
                                .toFile()
                );

        assertThat(snapshot.getWidth()).isEqualTo(1080);
        assertThat(snapshot.getHeight()).isEqualTo(1920);
    }

    private Path writeSolidPhoto(
            String name,
            Color color
    ) throws Exception {
        Path path = tempDirectory.resolve(name);
        BufferedImage image = new BufferedImage(
                1200,
                800,
                BufferedImage.TYPE_INT_RGB
        );

        java.awt.Graphics2D graphics =
                image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(
                    0,
                    0,
                    image.getWidth(),
                    image.getHeight()
            );
        } finally {
            graphics.dispose();
        }

        ImageIO.write(image, "jpg", path.toFile());
        return path;
    }
}