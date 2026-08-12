package com.chaerok.render.reel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilmStripComposerTest {

    private final FilmStripComposer composer =
            new FilmStripComposer();
    private final ReelTemplate template =
            new ReelTemplateRegistry().require(
                    ReelTemplateRegistry.GONGJU_V1
            );

    @TempDir
    Path tempDirectory;

    @Test
    @DisplayName("사진 네 장을 한 개의 4컷 필름 패널로 합성한다")
    void composesFourPhotosIntoOnePanel() throws Exception {
        List<Color> colors = List.of(
                new Color(30, 60, 90),
                new Color(90, 60, 30),
                new Color(40, 100, 50),
                new Color(120, 50, 80)
        );
        List<Path> photos = createImages(colors);
        Path destination = tempDirectory.resolve("film-strip.png");

        FilmStrip result = composer.compose(
                photos,
                template,
                transparentOverlay(),
                destination
        );

        assertThat(result.path()).isEqualTo(destination);
        assertThat(result.width()).isEqualTo(1080);
        assertThat(result.height()).isEqualTo(2276);
        assertThat(result.photoCount()).isEqualTo(4);
        assertThat(Files.size(destination)).isPositive();

        BufferedImage strip = ImageIO.read(destination.toFile());
        assertThat(strip.getWidth()).isEqualTo(1080);
        assertThat(strip.getHeight()).isEqualTo(2276);

        for (int index = 0; index < colors.size(); index++) {
            PhotoSlot slot = template.photoSlots().get(index);
            assertThat(colorAtCenter(strip, slot, 0))
                    .isEqualTo(colors.get(index));
        }
    }

    @Test
    @DisplayName("사진 다섯 장은 4컷 패널 두 개로 이어 붙인다")
    void composesFivePhotosIntoTwoPanels() throws Exception {
        List<Color> colors = List.of(
                new Color(20, 40, 60),
                new Color(40, 60, 80),
                new Color(60, 80, 100),
                new Color(80, 100, 120),
                new Color(100, 120, 140)
        );
        Path destination = tempDirectory.resolve("two-panels.png");

        FilmStrip result = composer.compose(
                createImages(colors),
                template,
                transparentOverlay(),
                destination
        );

        assertThat(result.height()).isEqualTo(4552);

        BufferedImage strip = ImageIO.read(destination.toFile());
        PhotoSlot firstSlot = template.photoSlots().get(0);
        assertThat(colorAtCenter(
                strip,
                firstSlot,
                template.panelHeight()
        )).isEqualTo(colors.get(4));
    }

    @Test
    @DisplayName("마지막 패널의 비어 있는 슬롯은 필름 베이지색으로 유지한다")
    void keepsUnusedSlotsAsFilmPaper() throws Exception {
        Path destination = tempDirectory.resolve("partial-panel.png");

        composer.compose(
                createImages(List.of(
                        new Color(20, 40, 60),
                        new Color(40, 60, 80),
                        new Color(60, 80, 100)
                )),
                template,
                transparentOverlay(),
                destination
        );

        BufferedImage strip = ImageIO.read(destination.toFile());
        PhotoSlot emptySlot = template.photoSlots().get(3);
        Color emptyColor = colorAtCenter(strip, emptySlot, 0);

        assertThat(emptyColor.getRed()).isBetween(210, 225);
        assertThat(emptyColor.getGreen()).isBetween(188, 205);
        assertThat(emptyColor.getBlue()).isBetween(148, 165);
    }

    @Test
    @DisplayName("3대2가 아닌 사진도 자르지 않고 슬롯 안에 contain 방식으로 배치한다")
    void containsPhotoWithoutCropping() throws Exception {
        Color photoColor = new Color(110, 50, 30);
        Path portrait = createImage(
                "portrait.png",
                200,
                300,
                photoColor
        );
        Path destination = tempDirectory.resolve("portrait-strip.png");

        composer.compose(
                List.of(portrait),
                template,
                transparentOverlay(),
                destination
        );

        BufferedImage strip = ImageIO.read(destination.toFile());
        PhotoSlot slot = template.photoSlots().get(0);
        assertThat(colorAtCenter(strip, slot, 0))
                .isEqualTo(photoColor);

        int nearLeftX = slot.x() + 10;
        int middleY = slot.y() + slot.height() / 2;
        Color margin = new Color(strip.getRGB(nearLeftX, middleY));
        assertThat(margin).isNotEqualTo(photoColor);
    }

    @Test
    @DisplayName("필름 패널과 크기가 다른 프레임 이미지는 거부한다")
    void rejectsOverlayWithWrongDimensions() throws Exception {
        Path photo = createImage(
                "photo.png",
                300,
                200,
                Color.WHITE
        );
        BufferedImage wrongOverlay = new BufferedImage(
                100,
                100,
                BufferedImage.TYPE_INT_ARGB
        );

        assertThatThrownBy(() -> composer.compose(
                List.of(photo),
                template,
                wrongOverlay,
                tempDirectory.resolve("invalid.png")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Overlay dimensions");
    }

    private List<Path> createImages(List<Color> colors)
            throws IOException {
        List<Path> paths = new ArrayList<>();
        for (int index = 0; index < colors.size(); index++) {
            paths.add(createImage(
                    String.format("%03d.png", index + 1),
                    300,
                    200,
                    colors.get(index)
            ));
        }
        return paths;
    }

    private Path createImage(
            String fileName,
            int width,
            int height,
            Color color
    ) throws IOException {
        BufferedImage image = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }

        Path path = tempDirectory.resolve(fileName);
        ImageIO.write(image, "png", path.toFile());
        return path;
    }

    private BufferedImage transparentOverlay() {
        return new BufferedImage(
                template.panelWidth(),
                template.panelHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
    }

    private Color colorAtCenter(
            BufferedImage image,
            PhotoSlot slot,
            int panelY
    ) {
        return new Color(image.getRGB(
                slot.x() + slot.width() / 2,
                panelY + slot.y() + slot.height() / 2
        ));
    }
}
