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
    @DisplayName("사진 세 장을 공주 템플릿 규격의 긴 필름 스트립으로 합성한다")
    void composesThreePhotoFilmStrip() throws Exception {
        Path first = createImage("001.png", 300, 200, new Color(30, 60, 90));
        Path second = createImage("002.png", 300, 200, new Color(90, 60, 30));
        Path third = createImage("003.png", 300, 200, new Color(40, 100, 50));
        BufferedImage overlay = transparentOverlay();
        Path destination = tempDirectory.resolve("film-strip.png");

        FilmStrip result = composer.compose(
                List.of(first, second, third),
                template,
                overlay,
                destination
        );

        assertThat(result.path()).isEqualTo(destination);
        assertThat(result.width()).isEqualTo(1080);
        assertThat(result.height()).isEqualTo(2720);
        assertThat(result.photoCount()).isEqualTo(3);
        assertThat(Files.size(destination)).isPositive();

        BufferedImage strip = ImageIO.read(destination.toFile());
        assertThat(strip.getWidth()).isEqualTo(1080);
        assertThat(strip.getHeight()).isEqualTo(2720);

        int firstPhotoCenterY = template.topPadding()
                + template.photoSlot().y()
                + template.photoSlot().height() / 2;
        assertThat(new Color(strip.getRGB(540, firstPhotoCenterY)))
                .isEqualTo(new Color(30, 60, 90));
    }

    @Test
    @DisplayName("사진 한 장이어도 최종 9대16 캔버스보다 짧은 스트립을 만들지 않는다")
    void keepsSinglePhotoStripAtLeastCanvasHeight() throws Exception {
        Path photo = createImage(
                "single.png",
                300,
                200,
                new Color(20, 80, 120)
        );
        Path destination = tempDirectory.resolve("single-strip.png");

        FilmStrip result = composer.compose(
                List.of(photo),
                template,
                transparentOverlay(),
                destination
        );

        assertThat(result.height()).isEqualTo(1920);
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
        int naturalHeight = template.topPadding()
                + template.cellHeight()
                + template.bottomPadding();
        int cellOffset = (template.canvasHeight() - naturalHeight) / 2
                + template.topPadding();
        int slotMiddleY = cellOffset
                + template.photoSlot().y()
                + template.photoSlot().height() / 2;

        assertThat(new Color(strip.getRGB(540, slotMiddleY)))
                .isEqualTo(photoColor);
        assertThat(new Color(strip.getRGB(130, slotMiddleY)))
                .isEqualTo(Color.BLACK);
    }

    @Test
    @DisplayName("필름 셀과 크기가 다른 프레임 이미지는 거부한다")
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
                template.cellWidth(),
                template.cellHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
    }
}
