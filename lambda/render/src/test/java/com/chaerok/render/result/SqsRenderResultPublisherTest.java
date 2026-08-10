package com.chaerok.render.result;

import com.chaerok.render.output.FilteredPhotoOutput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SqsRenderResultPublisherTest {

    @Test
    @DisplayName("결과 메시지 본문과 조회용 속성을 SQS에 전송한다")
    void publishesBodyAndAttributes() throws Exception {
        AtomicReference<SendMessageRequest> captured =
                new AtomicReference<>();
        SqsClient sqsClient = fakeClient(captured);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
                )
                .disable(
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
                );
        String queueUrl =
                "https://sqs.ap-northeast-2.amazonaws.com/123/"
                        + "chaerok-render-result-dev";

        SqsRenderResultPublisher publisher =
                new SqsRenderResultPublisher(
                        sqsClient,
                        new RenderResultQueueConfig(queueUrl),
                        objectMapper
                );

        RenderResultMessage message = new RenderResultMessage(
                1,
                RenderResultMessage.EVENT_COMPLETED,
                "request-message-1",
                java.util.UUID.fromString(
                        "133d6ee3-a120-4df3-8ba3-f60adbdd64d6"
                ),
                2L,
                6L,
                1L,
                "chaerok-media-dev",
                "COMPLETED",
                1,
                false,
                List.of(
                        new FilteredPhotoOutput(
                                1L,
                                1,
                                "filtered/001.jpg",
                                123L
                        )
                ),
                "export/result.zip",
                456L,
                "export/result.mp4",
                789L,
                "manifest.json",
                Instant.parse("2026-08-04T13:00:00Z"),
                null,
                null
        );

        String messageId = publisher.publish(message);

        assertThat(messageId).isEqualTo("result-message-1");
        SendMessageRequest request = captured.get();
        assertThat(request.queueUrl()).isEqualTo(queueUrl);

        JsonNode body = objectMapper.readTree(request.messageBody());
        assertThat(body.get("status").asText())
                .isEqualTo("COMPLETED");
        assertThat(body.get("occurredAt").asText())
                .isEqualTo("2026-08-04T13:00:00Z");
        assertThat(body.get("filteredPhotos").size()).isEqualTo(1);

        assertThat(
                request.messageAttributes()
                        .get("eventType")
                        .stringValue()
        ).isEqualTo(RenderResultMessage.EVENT_COMPLETED);
        assertThat(
                request.messageAttributes()
                        .get("schemaVersion")
                        .stringValue()
        ).isEqualTo("1");
        assertThat(
                request.messageAttributes()
                        .get("retryable")
                        .stringValue()
        ).isEqualTo("false");
    }

    private SqsClient fakeClient(
            AtomicReference<SendMessageRequest> captured
    ) {
        return (SqsClient) Proxy.newProxyInstance(
                SqsClient.class.getClassLoader(),
                new Class<?>[]{SqsClient.class},
                (proxy, method, args) -> {
                    if ("sendMessage".equals(method.getName())
                            && args != null
                            && args.length == 1
                            && args[0] instanceof SendMessageRequest request) {
                        captured.set(request);
                        return SendMessageResponse.builder()
                                .messageId("result-message-1")
                                .build();
                    }
                    if ("serviceName".equals(method.getName())) {
                        return "sqs";
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "FakeSqsClient";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new UnsupportedOperationException(
                            "Unexpected method: " + method.getName()
                    );
                }
        );
    }
}
