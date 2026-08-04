package com.chaerok.render;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.chaerok.backend.filter.analysis.AdaptiveFilterPolicy;
import com.chaerok.backend.filter.analysis.FilterOverlayTuningPolicy;
import com.chaerok.backend.filter.analysis.ImageSceneAnalyzer;
import com.chaerok.backend.filter.engine.FilmFilterEngine;
import com.chaerok.backend.filter.preset.FilmFilterPresetProvider;
import com.chaerok.backend.filter.processor.OverlayImageCache;
import com.chaerok.render.media.FilteredPhotoZipWriter;
import com.chaerok.render.media.JpegImageWriter;
import com.chaerok.render.media.ReelRenderer;
import com.chaerok.render.pipeline.RenderPipeline;
import com.chaerok.render.storage.ObjectStorage;
import com.chaerok.render.validation.RenderMessageValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RenderHandlerTest {

    @Test
    @DisplayName("정상 메시지는 필터 사진, ZIP, MP4, manifest를 업로드한다")
    void processesValidMessage() throws Exception {
        InMemoryObjectStorage storage = new InMemoryObjectStorage();
        storage.put(
                "users/6/rolls/2/original/photo.jpg",
                createJpeg()
        );

        RenderHandler handler = createHandler(storage);

        SQSBatchResponse response = handler.handleRequest(
                event("message-1", validBody()),
                new TestContext()
        );

        assertThat(response.getBatchItemFailures()).isEmpty();
        assertThat(storage.uploadedKeys())
                .anyMatch(key -> key.endsWith("/filtered/001.jpg"))
                .anyMatch(key -> key.endsWith(".zip"))
                .anyMatch(key -> key.endsWith(".mp4"))
                .anyMatch(key -> key.endsWith("/manifest.json"));

        String manifest = new String(
                storage.uploadedBytes(
                        "users/6/rolls/2/render-jobs/"
                                + "133d6ee3-a120-4df3-8ba3-f60adbdd64d6/"
                                + "manifest.json"
                ),
                StandardCharsets.UTF_8
        );

        assertThat(manifest)
                .contains(
                        "\"completedAt\" : "
                                + "\"2026-08-04T13:00:00Z\""
                );
    }

    @Test
    @DisplayName("잘못된 메시지는 해당 messageId만 실패로 반환한다")
    void returnsPartialBatchFailure() {
        RenderHandler handler = createHandler(
                new InMemoryObjectStorage()
        );

        SQSBatchResponse response = handler.handleRequest(
                event(
                        "message-bad",
                        """
                        {
                          "schemaVersion": 99
                        }
                        """
                ),
                new TestContext()
        );

        assertThat(response.getBatchItemFailures())
                .extracting(
                        SQSBatchResponse.BatchItemFailure
                                ::getItemIdentifier
                )
                .containsExactly("message-bad");
    }

    private RenderHandler createHandler(
            InMemoryObjectStorage storage
    ) {
        ObjectMapper objectMapper = RenderRuntimeFactory.objectMapper();

        FilmFilterEngine filterEngine = new FilmFilterEngine(
                new FilmFilterPresetProvider(),
                new OverlayImageCache(),
                new ImageSceneAnalyzer(),
                new AdaptiveFilterPolicy(),
                new FilterOverlayTuningPolicy()
        );

        ReelRenderer fakeReelRenderer = (
                filteredDirectory,
                photoCount,
                destination
        ) -> {
            try {
                Files.createDirectories(destination.getParent());
                Files.writeString(
                        destination,
                        "fake-mp4-" + photoCount,
                        StandardCharsets.UTF_8
                );
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        };

        RenderPipeline pipeline = new RenderPipeline(
                storage,
                filterEngine,
                new JpegImageWriter(),
                new FilteredPhotoZipWriter(),
                fakeReelRenderer,
                objectMapper,
                Clock.fixed(
                        Instant.parse("2026-08-04T13:00:00Z"),
                        ZoneOffset.UTC
                )
        );

        return new RenderHandler(
                objectMapper,
                new RenderMessageValidator(),
                pipeline
        );
    }

    private SQSEvent event(String messageId, String body) {
        SQSEvent.SQSMessage record = new SQSEvent.SQSMessage();
        record.setMessageId(messageId);
        record.setBody(body);

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(record));
        return event;
    }

    private String validBody() {
        return """
                {
                  "schemaVersion": 1,
                  "renderJobId": "133d6ee3-a120-4df3-8ba3-f60adbdd64d6",
                  "filmRollId": 2,
                  "userId": 6,
                  "regionId": 1,
                  "bucket": "chaerok-media-dev-7f3k2m",
                  "filterId": "gongju_baekje_love",
                  "filterStrength": 0.8,
                  "filterVersion": 1,
                  "totalPhotoCount": 1,
                  "requestedAt": "2026-07-30T22:59:17.022325",
                  "photos": [
                    {
                      "photoId": 1,
                      "sequence": 1,
                      "originalObjectKey": "users/6/rolls/2/original/photo.jpg",
                      "hasFace": false,
                      "sceneType": null,
                      "takenAt": "2026-07-30T22:40:00"
                    }
                  ]
                }
                """;
    }

    private byte[] createJpeg() throws IOException {
        BufferedImage image = new BufferedImage(
                320,
                240,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(58, 104, 76));
        graphics.fillRect(0, 0, 320, 240);
        graphics.setColor(new Color(220, 184, 120));
        graphics.fillOval(110, 60, 100, 100);
        graphics.dispose();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", output);
        return output.toByteArray();
    }

    private static final class InMemoryObjectStorage
            implements ObjectStorage {

        private final Map<String, byte[]> objects = new HashMap<>();
        private final Map<String, byte[]> uploads = new HashMap<>();

        void put(String objectKey, byte[] bytes) {
            objects.put(objectKey, bytes);
        }

        List<String> uploadedKeys() {
            return uploads.keySet().stream().sorted().toList();
        }

        byte[] uploadedBytes(String objectKey) {
            byte[] bytes = uploads.get(objectKey);
            if (bytes == null) {
                throw new IllegalStateException(
                        "Missing fake upload: " + objectKey
                );
            }
            return bytes;
        }

        @Override
        public boolean exists(String bucket, String objectKey) {
            return uploads.containsKey(objectKey)
                    || objects.containsKey(objectKey);
        }

        @Override
        public void download(
                String bucket,
                String objectKey,
                Path destination
        ) {
            byte[] bytes = objects.get(objectKey);
            if (bytes == null) {
                throw new IllegalStateException(
                        "Missing fake object: " + objectKey
                );
            }

            try {
                Files.createDirectories(destination.getParent());
                Files.write(destination, bytes);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        @Override
        public void upload(
                String bucket,
                String objectKey,
                Path source,
                String contentType
        ) {
            try {
                uploads.put(objectKey, Files.readAllBytes(source));
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private static final class TestContext implements Context {

        private final LambdaLogger logger = new LambdaLogger() {
            @Override
            public void log(String message) {
                System.out.print(message);
            }

            @Override
            public void log(byte[] message) {
                System.out.print(
                        new String(message, StandardCharsets.UTF_8)
                );
            }
        };

        @Override
        public String getAwsRequestId() {
            return "test-request";
        }

        @Override
        public String getLogGroupName() {
            return "test";
        }

        @Override
        public String getLogStreamName() {
            return "test";
        }

        @Override
        public String getFunctionName() {
            return "chaerok-render-lambda";
        }

        @Override
        public String getFunctionVersion() {
            return "1";
        }

        @Override
        public String getInvokedFunctionArn() {
            return "test";
        }

        @Override
        public CognitoIdentity getIdentity() {
            return null;
        }

        @Override
        public ClientContext getClientContext() {
            return null;
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 300_000;
        }

        @Override
        public int getMemoryLimitInMB() {
            return 3008;
        }

        @Override
        public LambdaLogger getLogger() {
            return logger;
        }
    }
}
