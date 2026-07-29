package com.chaerok.backend.filter.controller;

import com.chaerok.backend.filter.analysis.SceneType;
import com.chaerok.backend.filter.engine.FilmFilterEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

@Tag(
        name = "Filter Preview",
        description = "필터 미리보기 API"
)
@RestController
@RequestMapping("/api/filters")
@RequiredArgsConstructor
public class FilterPreviewController {

    private static final long MAX_FILE_SIZE =
            20L * 1024 * 1024;

    private static final int MAX_WIDTH = 6000;
    private static final int MAX_HEIGHT = 6000;

    private static final float JPEG_QUALITY = 0.92f;

    private final FilmFilterEngine filmFilterEngine;

    @Operation(
            summary = "이미지 필터 미리보기",
            description = """
                    이미지의 밝기와 장면 종류에 따라
                    필터 강도를 자동으로 조절하여 적용합니다.
                    
                    scene을 입력하지 않으면 장면을 자동 분석합니다.
                    hasFace가 true이고 야간 사진이 아니면
                    인물 사진으로 처리합니다.
                    
                    scene에는 LANDSCAPE, PORTRAIT, NIGHT를 사용할 수 있습니다.
                    """
    )
    @PostMapping(
            value = "/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.IMAGE_JPEG_VALUE
    )
    public ResponseEntity<byte[]> previewFilter(
            @RequestPart("image")
            MultipartFile image,

            @RequestParam("filterId")
            String filterId,

            @RequestParam(
                    value = "strength",
                    defaultValue = "1.0"
            )
            double strength,

            @RequestParam(
                    value = "hasFace",
                    defaultValue = "false"
            )
            boolean hasFace,

            @RequestParam(
                    value = "scene",
                    required = false
            )
            String scene
    ) throws IOException {

        validateFile(image);
        validateFilterId(filterId);
        validateStrength(strength);

        /*
         * scene이 전달되면 해당 장면을 강제로 적용합니다.
         * 전달되지 않으면 null이 되어 자동 분석합니다.
         */
        SceneType forcedSceneType =
                parseSceneType(scene);

        BufferedImage original;

        try (var inputStream = image.getInputStream()) {
            original = ImageIO.read(inputStream);
        }

        if (original == null) {
            throw new IllegalArgumentException(
                    "이미지를 읽을 수 없습니다. 지원하지 않는 이미지 형식일 수 있습니다."
            );
        }

        validateImageSize(original);

        BufferedImage filtered =
                filmFilterEngine.apply(
                        original,
                        filterId.trim(),
                        strength,
                        hasFace,
                        forcedSceneType
                );

        byte[] jpegBytes =
                writeJpeg(filtered, JPEG_QUALITY);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .contentLength(jpegBytes.length)
                .body(jpegBytes);
    }

    /**
     * 장면 입력값을 SceneType으로 변환합니다.
     *
     * null 또는 빈 문자열이면 자동 분석 모드입니다.
     * 대소문자를 구분하지 않습니다.
     */
    private SceneType parseSceneType(String scene) {
        if (scene == null || scene.isBlank()) {
            return null;
        }

        try {
            return SceneType.valueOf(
                    scene.trim()
                            .toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "scene은 LANDSCAPE, PORTRAIT, NIGHT 중 하나여야 합니다."
            );
        }
    }

    private void validateFile(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException(
                    "이미지 파일은 필수입니다."
            );
        }

        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "이미지 파일은 최대 20MB까지 업로드할 수 있습니다."
            );
        }

        String contentType = image.getContentType();

        if (contentType == null) {
            throw new IllegalArgumentException(
                    "이미지 Content-Type을 확인할 수 없습니다."
            );
        }

        boolean supportedType =
                contentType.equalsIgnoreCase(
                        MediaType.IMAGE_JPEG_VALUE
                )
                        || contentType.equalsIgnoreCase(
                        MediaType.IMAGE_PNG_VALUE
                )
                        || contentType.equalsIgnoreCase(
                        "image/webp"
                );

        if (!supportedType) {
            throw new IllegalArgumentException(
                    "JPG, PNG, WebP 이미지만 업로드할 수 있습니다."
            );
        }
    }

    private void validateFilterId(String filterId) {
        if (filterId == null || filterId.isBlank()) {
            throw new IllegalArgumentException(
                    "filterId는 필수입니다."
            );
        }
    }

    private void validateStrength(double strength) {
        if (!Double.isFinite(strength)) {
            throw new IllegalArgumentException(
                    "strength는 유효한 숫자여야 합니다."
            );
        }

        if (strength < 0.0 || strength > 1.0) {
            throw new IllegalArgumentException(
                    "strength는 0.0 이상 1.0 이하이어야 합니다."
            );
        }
    }

    private void validateImageSize(BufferedImage image) {
        if (
                image.getWidth() > MAX_WIDTH
                        || image.getHeight() > MAX_HEIGHT
        ) {
            throw new IllegalArgumentException(
                    "이미지 해상도는 최대 "
                            + MAX_WIDTH
                            + "x"
                            + MAX_HEIGHT
                            + "까지 허용됩니다."
            );
        }

        if (
                image.getWidth() <= 0
                        || image.getHeight() <= 0
        ) {
            throw new IllegalArgumentException(
                    "이미지 해상도가 올바르지 않습니다."
            );
        }
    }

    private byte[] writeJpeg(
            BufferedImage image,
            float quality
    ) throws IOException {

        if (image == null) {
            throw new IllegalArgumentException(
                    "저장할 이미지가 비어 있습니다."
            );
        }

        Iterator<ImageWriter> writers =
                ImageIO.getImageWritersByFormatName("jpg");

        if (!writers.hasNext()) {
            throw new IllegalStateException(
                    "JPG Writer를 찾을 수 없습니다."
            );
        }

        ImageWriter writer = writers.next();

        try (
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream();

                MemoryCacheImageOutputStream imageOutputStream =
                        new MemoryCacheImageOutputStream(
                                outputStream
                        )
        ) {
            writer.setOutput(imageOutputStream);

            ImageWriteParam writeParam =
                    writer.getDefaultWriteParam();

            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(
                        ImageWriteParam.MODE_EXPLICIT
                );

                writeParam.setCompressionQuality(
                        clampJpegQuality(quality)
                );
            }

            writer.write(
                    null,
                    new IIOImage(
                            image,
                            null,
                            null
                    ),
                    writeParam
            );

            imageOutputStream.flush();

            return outputStream.toByteArray();

        } finally {
            writer.dispose();
        }
    }

    private float clampJpegQuality(float quality) {
        return Math.max(
                0.0f,
                Math.min(1.0f, quality)
        );
    }
}