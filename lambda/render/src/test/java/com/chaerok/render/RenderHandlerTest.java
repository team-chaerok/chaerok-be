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
import com.chaerok.render.result.RenderResultMessage;
import com.chaerok.render.result.RenderResultPublisher;
import com.chaerok.render.retry.RenderRetryConfig;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RenderHandlerTest {

    private static final Instant FIXED_NOW =
            Instant.parse("2026-08-04T13:00:00Z");

    @Test
    @DisplayName("정상 메시지는 결과물을 업로드하고 COMPLETED 결과를 발행한다")
    void processesValidMessageAndPublishesCompleted() throws Exception {
        InMemoryObjectStorage storage = new InMemoryObjectStorage();
        storage.put(
                "users/6/rolls/2/original/photo.jpg",
                createJpeg()
        );
        CapturingResultPublisher publisher =
                new CapturingResultPublisher();

        RenderHandler handler = createHandler(storage, publisher);

        SQSBatchResponse response = handler.handleRequest(
                event("message-1", validBody(), "2"),
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

        assertThat(publisher.messages()).hasSize(2);
        RenderResultMessage started = messageByEvent(
                publisher.messages(),
                RenderResultMessage.EVENT_STARTED
        );
        assertThat(started.status()).isEqualTo("PROCESSING");
        assertThat(started.requestMessageId()).isEqualTo("message-1");
        assertThat(started.attempt()).isEqualTo(2);
        assertThat(started.occurredAt()).isEqualTo(FIXED_NOW);

        RenderResultMessage result = messageByEvent(
                publisher.messages(),
                RenderResultMessage.EVENT_COMPLETED
        );
        assertThat(result.eventType())
                .isEqualTo(RenderResultMessage.EVENT_COMPLETED);
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.requestMessageId()).isEqualTo("message-1");
        assertThat(result.attempt()).isEqualTo(2);
        assertThat(result.retryable()).isFalse();
        assertThat(result.filteredPhotos()).hasSize(1);
        assertThat(result.reelObjectKey()).endsWith(".mp4");
        assertThat(result.occurredAt()).isEqualTo(FIXED_NOW);
        assertThat(result.errorCode()).isNull();
    }

    @Test
    @DisplayName("검증 실패는 FAILED 결과를 발행하고 재시도 없이 종료한다")
    void publishesFailedResultForInvalidMessage() {
        CapturingResultPublisher publisher =
                new CapturingResultPublisher();
        RenderHandler handler = createHandler(
                new InMemoryObjectStorage(),
                publisher
        );

        SQSBatchResponse response = handler.handleRequest(
                event("message-bad", invalidBody(), "3"),
                new TestContext()
        );

        assertThat(response.getBatchItemFailures()).isEmpty();

        assertThat(publisher.messages()).hasSize(1);
        RenderResultMessage result = messageByEvent(
                publisher.messages(),
                RenderResultMessage.EVENT_FAILED
        );
        assertThat(result.eventType())
                .isEqualTo(RenderResultMessage.EVENT_FAILED);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.attempt()).isEqualTo(3);
        assertThat(result.retryable()).isFalse();
        assertThat(result.errorCode())
                .isEqualTo("INVALID_RENDER_MESSAGE");
        assertThat(result.errorMessage())
                .contains("totalPhotoCount");
        assertThat(result.occurredAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    @DisplayName("깨진 JPEG는 재시도하지 않고 사진 식별 정보와 함께 실패 처리한다")
    void publishesNonRetryableFailureForInvalidImage() {
        InMemoryObjectStorage storage = new InMemoryObjectStorage();
        storage.put(
                "users/6/rolls/2/original/photo.jpg",
                "not-a-jpeg".getBytes(StandardCharsets.UTF_8)
        );
        CapturingResultPublisher publisher =
                new CapturingResultPublisher();
        RenderHandler handler = createHandler(storage, publisher);

        SQSBatchResponse response = handler.handleRequest(
                event("message-invalid-image", validBody(), "1"),
                new TestContext()
        );

        assertThat(response.getBatchItemFailures()).isEmpty();
        assertThat(publisher.messages()).hasSize(2);

        RenderResultMessage result = messageByEvent(
                publisher.messages(),
                RenderResultMessage.EVENT_FAILED
        );
        assertThat(result.errorCode()).isEqualTo("PHOTO_INVALID_IMAGE");
        assertThat(result.retryable()).isFalse();
        assertThat(result.errorMessage())
                .contains("stage=READ_HEADER")
                .contains("photoId=1")
                .contains("sequence=1");
    }

    @Test
    @DisplayName("6000px 이하여도 16MP를 넘는 JPEG는 디코딩 전에 거부한다")
    void rejectsImageThatExceedsPixelLimitBeforeDecode() throws Exception {
        InMemoryObjectStorage storage = new InMemoryObjectStorage();
        storage.put(
                "users/6/rolls/2/original/photo.jpg",
                createJpegWithDimensions(5000, 4000)
        );
        CapturingResultPublisher publisher =
                new CapturingResultPublisher();
        RenderHandler handler = createHandler(storage, publisher);

        SQSBatchResponse response = handler.handleRequest(
                event("message-too-large", validBody(), "1"),
                new TestContext()
        );

        assertThat(response.getBatchItemFailures()).isEmpty();
        assertThat(publisher.messages()).hasSize(2);

        RenderResultMessage result = messageByEvent(
                publisher.messages(),
                RenderResultMessage.EVENT_FAILED
        );
        assertThat(result.errorCode()).isEqualTo("PHOTO_TOO_LARGE");
        assertThat(result.errorMessage())
                .contains("width=5000")
                .contains("height=4000")
                .contains("pixels=20000000");
    }

    @Test
    @DisplayName("필터 처리의 결정적 실패는 재시도하지 않는다")
    void publishesNonRetryableFailureForFilterError() throws Exception {
        InMemoryObjectStorage storage = new InMemoryObjectStorage();
        storage.put(
                "users/6/rolls/2/original/photo.jpg",
                createJpeg()
        );
        CapturingResultPublisher publisher =
                new CapturingResultPublisher();
        RenderHandler handler = createHandler(storage, publisher);

        String unsupportedFilterBody = validBody().replace(
                "\"gongju\"",
                "\"unsupported-filter\""
        );

        SQSBatchResponse response = handler.handleRequest(
                event("message-filter-fail", unsupportedFilterBody, "1"),
                new TestContext()
        );

        assertThat(response.getBatchItemFailures()).isEmpty();
        assertThat(publisher.messages()).hasSize(2);

        RenderResultMessage result = messageByEvent(
                publisher.messages(),
                RenderResultMessage.EVENT_FAILED
        );
        assertThat(result.errorCode()).isEqualTo("PHOTO_FILTER_FAILED");
        assertThat(result.retryable()).isFalse();
        assertThat(result.errorMessage())
                .contains("stage=FILTER")
                .contains("photoId=1")
                .contains("sequence=1");
    }

    @Test
    @DisplayName("유효하지 않은 기존 manifest는 재시도하지 않는다")
    void publishesNonRetryableFailureForInvalidManifest() {
        InMemoryObjectStorage storage = new InMemoryObjectStorage();
        storage.put(
                "users/6/rolls/2/render-jobs/"
                        + "133d6ee3-a120-4df3-8ba3-f60adbdd64d6/"
                        + "manifest.json",
                "{invalid-json".getBytes(StandardCharsets.UTF_8)
        );
        CapturingResultPublisher publisher =
                new CapturingResultPublisher();
        RenderHandler handler = createHandler(storage, publisher);

        SQSBatchResponse response = handler.handleRequest(
                event("message-invalid-manifest", validBody(), "1"),
                new TestContext()
        );

        assertThat(response.getBatchItemFailures()).isEmpty();
        assertThat(publisher.messages()).hasSize(2);
        assertThat(messageByEvent(
                publisher.messages(),
                RenderResultMessage.EVENT_FAILED
        ).errorCode()).isEqualTo("MANIFEST_INVALID");
    }

    @Test
    @DisplayName("재시도 중인 실행 실패는 terminal FAILED 결과를 발행하지 않는다")
    void doesNotPublishTerminalFailureBeforeFinalAttempt() {
        CapturingResultPublisher publisher =
                new CapturingResultPublisher();
        RenderHandler handler = createHandler(
                new InMemoryObjectStorage(),
                publisher
        );

        SQSBatchResponse response = handler.handleRequest(
                event("message-missing-input", validBody(), "1"),
                new TestContext()
        );

        assertThat(response.getBatchItemFailures())
                .extracting(
                        SQSBatchResponse.BatchItemFailure
                                ::getItemIdentifier
                )
                .containsExactly("message-missing-input");
        assertThat(publisher.messages()).hasSize(1);
        assertThat(publisher.messages().get(0).eventType())
                .isEqualTo(RenderResultMessage.EVENT_STARTED);
    }

    @Test
    @DisplayName("마지막 실행 실패는 terminal FAILED 결과를 발행하고 요청을 DLQ 대상으로 남긴다")
    void publishesTerminalFailureOnFinalAttempt() {
        CapturingResultPublisher publisher =
                new CapturingResultPublisher();
        RenderHandler handler = createHandler(
                new InMemoryObjectStorage(),
                publisher
        );

        SQSBatchResponse response = handler.handleRequest(
                event("message-final-failure", validBody(), "3"),
                new TestContext()
        );

        assertThat(response.getBatchItemFailures())
                .extracting(
                        SQSBatchResponse.BatchItemFailure
                                ::getItemIdentifier
                )
                .containsExactly("message-final-failure");

        assertThat(publisher.messages()).hasSize(2);
        RenderResultMessage result = messageByEvent(
                publisher.messages(),
                RenderResultMessage.EVENT_FAILED
        );
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.retryable()).isFalse();
        assertThat(result.attempt()).isEqualTo(3);
        assertThat(result.errorCode())
                .isEqualTo("PHOTO_DOWNLOAD_FAILED");
        assertThat(result.errorMessage())
                .contains("stage=DOWNLOAD")
                .contains("photoId=1")
                .contains("sequence=1");
    }

    @Test
    @DisplayName("릴스 생성 실패는 마지막 시도까지 재시도한 뒤 실패 처리한다")
    void publishesReelGenerationFailureOnFinalAttempt() throws Exception {
        InMemoryObjectStorage storage = new InMemoryObjectStorage();
        storage.put(
                "users/6/rolls/2/original/photo.jpg",
                createJpeg()
        );
        CapturingResultPublisher publisher =
                new CapturingResultPublisher();
        ReelRenderer failingReelRenderer = (
                filteredDirectory,
                photoCount,
                destination
        ) -> {
            throw new IllegalStateException("fake ffmpeg failure");
        };
        RenderHandler handler = createHandler(
                storage,
                publisher,
                failingReelRenderer
        );

        SQSBatchResponse response = handler.handleRequest(
                event("message-reel-fail", validBody(), "3"),
                new TestContext()
        );

        assertThat(response.getBatchItemFailures())
                .extracting(
                        SQSBatchResponse.BatchItemFailure
                                ::getItemIdentifier
                )
                .containsExactly("message-reel-fail");
        assertThat(publisher.messages()).hasSize(2);
        assertThat(messageByEvent(
                publisher.messages(),
                RenderResultMessage.EVENT_FAILED
        ).errorCode()).isEqualTo("REEL_GENERATION_FAILED");
    }

    @Test
    @DisplayName("ZIP 업로드 실패는 마지막 시도에 ZIP_UPLOAD_FAILED로 기록한다")
    void publishesZipUploadFailureOnFinalAttempt() throws Exception {
        InMemoryObjectStorage storage = new InMemoryObjectStorage();
        storage.put(
                "users/6/rolls/2/original/photo.jpg",
                createJpeg()
        );
        storage.failUploadsEndingWith(".zip");
        CapturingResultPublisher publisher =
                new CapturingResultPublisher();
        RenderHandler handler = createHandler(storage, publisher);

        SQSBatchResponse response = handler.handleRequest(
                event("message-zip-upload-fail", validBody(), "3"),
                new TestContext()
        );

        assertThat(response.getBatchItemFailures())
                .extracting(
                        SQSBatchResponse.BatchItemFailure
                                ::getItemIdentifier
                )
                .containsExactly("message-zip-upload-fail");
        assertThat(publisher.messages()).hasSize(2);
        assertThat(messageByEvent(
                publisher.messages(),
                RenderResultMessage.EVENT_FAILED
        ).errorCode()).isEqualTo("ZIP_UPLOAD_FAILED");
    }

    @Test
    @DisplayName("PROCESSING 결과 발행 실패 시 렌더링을 시작하지 않고 요청을 재시도한다")
    void retriesBeforeRenderingWhenStartedPublishFails() throws Exception {
        InMemoryObjectStorage storage = new InMemoryObjectStorage();
        storage.put(
                "users/6/rolls/2/original/photo.jpg",
                createJpeg()
        );
        CapturingResultPublisher publisher =
                new CapturingResultPublisher(true);
        RenderHandler handler = createHandler(storage, publisher);

        SQSBatchResponse response = handler.handleRequest(
                event("message-start-publish-fail", validBody(), "1"),
                new TestContext()
        );

        assertThat(response.getBatchItemFailures())
                .extracting(
                        SQSBatchResponse.BatchItemFailure
                                ::getItemIdentifier
                )
                .containsExactly("message-start-publish-fail");
        assertThat(publisher.messages()).hasSize(1);
        assertThat(publisher.messages().get(0).eventType())
                .isEqualTo(RenderResultMessage.EVENT_STARTED);
        assertThat(storage.uploadCount()).isZero();
    }

    @Test
    @DisplayName("COMPLETED 결과 발행 실패 시 요청 메시지를 재시도 대상으로 반환한다")
    void returnsBatchFailureWhenCompletedPublishFails()
            throws Exception {
        InMemoryObjectStorage storage = new InMemoryObjectStorage();
        storage.put(
                "users/6/rolls/2/original/photo.jpg",
                createJpeg()
        );
        FailCompletedResultPublisher publisher =
                new FailCompletedResultPublisher();
        RenderHandler handler = createHandler(storage, publisher);

        SQSBatchResponse response = handler.handleRequest(
                event("message-publish-fail", validBody(), "1"),
                new TestContext()
        );

        assertThat(response.getBatchItemFailures())
                .extracting(
                        SQSBatchResponse.BatchItemFailure
                                ::getItemIdentifier
                )
                .containsExactly("message-publish-fail");
        assertThat(publisher.messages()).hasSize(2);
        assertThat(messageByEvent(
                publisher.messages(),
                RenderResultMessage.EVENT_COMPLETED
        ).status()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("COMPLETED 결과 발행 재시도는 기존 manifest를 재사용한다")
    void reusesManifestWhenCompletedResultPublishIsRetried()
            throws Exception {
        InMemoryObjectStorage storage = new InMemoryObjectStorage();
        storage.put(
                "users/6/rolls/2/original/photo.jpg",
                createJpeg()
        );
        FailOnceResultPublisher publisher =
                new FailOnceResultPublisher();
        RenderHandler handler = createHandler(storage, publisher);

        SQSBatchResponse first = handler.handleRequest(
                event("message-result-retry", validBody(), "1"),
                new TestContext()
        );
        int uploadsAfterFirstAttempt = storage.uploadCount();

        SQSBatchResponse second = handler.handleRequest(
                event("message-result-retry", validBody(), "2"),
                new TestContext()
        );

        assertThat(first.getBatchItemFailures()).hasSize(1);
        assertThat(second.getBatchItemFailures()).isEmpty();
        assertThat(uploadsAfterFirstAttempt).isEqualTo(4);
        assertThat(storage.uploadCount())
                .isEqualTo(uploadsAfterFirstAttempt);
        assertThat(publisher.messages()).hasSize(4);
        assertThat(publisher.messages().stream()
                .filter(message -> RenderResultMessage.EVENT_STARTED.equals(
                        message.eventType()
                ))
                .toList())
                .hasSize(2);
        assertThat(publisher.messages().stream()
                .filter(message -> RenderResultMessage.EVENT_COMPLETED.equals(
                        message.eventType()
                ))
                .toList())
                .hasSize(2);
    }

    @Test
    @DisplayName("식별자가 없는 잘못된 메시지는 결과 큐에 발행하지 않는다")
    void skipsFailedPublishWithoutIdentifiers() {
        CapturingResultPublisher publisher =
                new CapturingResultPublisher();
        RenderHandler handler = createHandler(
                new InMemoryObjectStorage(),
                publisher
        );

        SQSBatchResponse response = handler.handleRequest(
                event(
                        "message-no-identifiers",
                        "{\"schemaVersion\":99}",
                        "1"
                ),
                new TestContext()
        );

        assertThat(response.getBatchItemFailures()).hasSize(1);
        assertThat(publisher.messages()).isEmpty();
    }

    private RenderResultMessage messageByEvent(
            List<RenderResultMessage> messages,
            String eventType
    ) {
        return messages.stream()
                .filter(message -> eventType.equals(message.eventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing result event: " + eventType
                ));
    }

    private RenderHandler createHandler(
            InMemoryObjectStorage storage,
            RenderResultPublisher publisher
    ) {
        return createHandler(
                storage,
                publisher,
                successfulReelRenderer()
        );
    }

    private RenderHandler createHandler(
            InMemoryObjectStorage storage,
            RenderResultPublisher publisher,
            ReelRenderer reelRenderer
    ) {
        ObjectMapper objectMapper = RenderRuntimeFactory.objectMapper();

        FilmFilterEngine filterEngine = new FilmFilterEngine(
                new FilmFilterPresetProvider(),
                new OverlayImageCache(),
                new ImageSceneAnalyzer(),
                new AdaptiveFilterPolicy(),
                new FilterOverlayTuningPolicy()
        );

        Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        RenderPipeline pipeline = new RenderPipeline(
                storage,
                filterEngine,
                new JpegImageWriter(),
                new FilteredPhotoZipWriter(),
                reelRenderer,
                objectMapper,
                clock
        );

        return new RenderHandler(
                objectMapper,
                new RenderMessageValidator(),
                pipeline,
                publisher,
                clock,
                new RenderRetryConfig(3)
        );
    }

    private ReelRenderer successfulReelRenderer() {
        return (filteredDirectory, photoCount, destination) -> {
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
    }

    private SQSEvent event(
            String messageId,
            String body,
            String receiveCount
    ) {
        SQSEvent.SQSMessage record = new SQSEvent.SQSMessage();
        record.setMessageId(messageId);
        record.setBody(body);
        record.setAttributes(
                Map.of("ApproximateReceiveCount", receiveCount)
        );

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(record));
        return event;
    }

    private String validBody() {
        return """
                {
                  "schemaVersion": 2,
                  "renderJobId": "133d6ee3-a120-4df3-8ba3-f60adbdd64d6",
                  "filmRollId": 2,
                  "userId": 6,
                  "regionId": 1,
                  "bucket": "chaerok-media-dev-7f3k2m",
                  "filterId": "gongju",
                  "filterStrength": 0.8,
                  "filterVersion": 1,
                  "totalPhotoCount": 1,
                  "requestedAt": "2026-07-30T22:59:17.022325",
                  "photos": [
                    {
                      "photoId": 1,
                      "sequence": 1,
                      "originalObjectKey": "users/6/rolls/2/original/photo.jpg",
                      "takenAt": "2026-07-30T22:40:00"
                    }
                  ]
                }
                """;
    }

    private String invalidBody() {
        return """
                {
                  "schemaVersion": 2,
                  "renderJobId": "133d6ee3-a120-4df3-8ba3-f60adbdd64d6",
                  "filmRollId": 2,
                  "userId": 6,
                  "regionId": 1,
                  "bucket": "chaerok-media-dev-7f3k2m",
                  "filterId": "gongju",
                  "filterStrength": 0.8,
                  "filterVersion": 1,
                  "totalPhotoCount": 0,
                  "requestedAt": "2026-07-30T22:59:17.022325",
                  "photos": []
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

    private byte[] createJpegWithDimensions(
            int width,
            int height
    ) throws IOException {
        if (width < 1 || width > 65535
                || height < 1 || height > 65535) {
            throw new IllegalArgumentException(
                    "JPEG dimensions must fit unsigned 16-bit values."
            );
        }

        byte[] jpeg = createJpeg();
        for (int index = 0; index < jpeg.length - 9; index++) {
            if ((jpeg[index] & 0xFF) != 0xFF) {
                continue;
            }

            int marker = jpeg[index + 1] & 0xFF;
            if (marker != 0xC0 && marker != 0xC1 && marker != 0xC2) {
                continue;
            }

            jpeg[index + 5] = (byte) (height >>> 8);
            jpeg[index + 6] = (byte) height;
            jpeg[index + 7] = (byte) (width >>> 8);
            jpeg[index + 8] = (byte) width;
            return jpeg;
        }

        throw new IllegalStateException(
                "JPEG start-of-frame marker was not found."
        );
    }

    private static final class CapturingResultPublisher
            implements RenderResultPublisher {

        private final List<RenderResultMessage> messages =
                new ArrayList<>();
        private final boolean fail;

        CapturingResultPublisher() {
            this(false);
        }

        CapturingResultPublisher(boolean fail) {
            this.fail = fail;
        }

        @Override
        public String publish(RenderResultMessage message) {
            messages.add(message);
            if (fail) {
                throw new IllegalStateException(
                        "fake result publish failure"
                );
            }
            return "result-message-" + messages.size();
        }

        List<RenderResultMessage> messages() {
            return List.copyOf(messages);
        }
    }

    private static final class FailCompletedResultPublisher
            implements RenderResultPublisher {

        private final List<RenderResultMessage> messages =
                new ArrayList<>();

        @Override
        public String publish(RenderResultMessage message) {
            messages.add(message);
            if (RenderResultMessage.EVENT_COMPLETED.equals(
                    message.eventType()
            )) {
                throw new IllegalStateException(
                        "fake completed result publish failure"
                );
            }
            return "result-message-started";
        }

        List<RenderResultMessage> messages() {
            return List.copyOf(messages);
        }
    }

    private static final class FailOnceResultPublisher
            implements RenderResultPublisher {

        private final List<RenderResultMessage> messages =
                new ArrayList<>();
        private boolean failedOnce;

        @Override
        public String publish(RenderResultMessage message) {
            messages.add(message);
            if (RenderResultMessage.EVENT_COMPLETED.equals(
                    message.eventType()
            ) && !failedOnce) {
                failedOnce = true;
                throw new IllegalStateException(
                        "fake first completed result publish failure"
                );
            }
            return "result-message-success";
        }

        List<RenderResultMessage> messages() {
            return List.copyOf(messages);
        }
    }

    private static final class InMemoryObjectStorage
            implements ObjectStorage {

        private final Map<String, byte[]> objects = new HashMap<>();
        private final Map<String, byte[]> uploads = new HashMap<>();
        private String failingUploadSuffix;
        private int uploadCount;

        void put(String objectKey, byte[] bytes) {
            objects.put(objectKey, bytes);
        }

        void failUploadsEndingWith(String suffix) {
            this.failingUploadSuffix = suffix;
        }

        int uploadCount() {
            return uploadCount;
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
                bytes = uploads.get(objectKey);
            }
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
            if (failingUploadSuffix != null
                    && objectKey.endsWith(failingUploadSuffix)) {
                throw new IllegalStateException(
                        "Fake upload failure: " + objectKey
                );
            }

            try {
                uploads.put(objectKey, Files.readAllBytes(source));
                uploadCount++;
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
