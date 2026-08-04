package com.chaerok.backend.filter.processor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OverlayImageCache {

    private final Map<String, BufferedImage> cache = new ConcurrentHashMap<>();

    public BufferedImage getOverlay(String overlayPath) {
        if (overlayPath == null || overlayPath.isBlank()) {
            throw new IllegalArgumentException("오버레이 경로가 비어 있습니다.");
        }

        return cache.computeIfAbsent(overlayPath, this::loadOverlay);
    }

    private BufferedImage loadOverlay(String overlayPath) {
        ClassLoader classLoader = Thread.currentThread()
                .getContextClassLoader();

        if (classLoader == null) {
            classLoader = OverlayImageCache.class.getClassLoader();
        }

        try (InputStream inputStream =
                     classLoader.getResourceAsStream(overlayPath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException(
                        "오버레이 파일을 찾을 수 없습니다: " + overlayPath
                );
            }

            BufferedImage image = ImageIO.read(inputStream);

            if (image == null) {
                throw new IllegalArgumentException(
                        "오버레이 이미지를 읽을 수 없습니다: " + overlayPath
                );
            }

            return image;

        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "오버레이 이미지 로드 중 오류가 발생했습니다: " + overlayPath,
                    exception
            );
        }
    }
}
