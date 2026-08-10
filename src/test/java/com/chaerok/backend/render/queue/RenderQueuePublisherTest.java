package com.chaerok.backend.render.queue;

import com.chaerok.backend.global.aws.sqs.AwsSqsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderQueuePublisherTest {

    @Mock
    private SqsClient sqsClient;

    @Test
    @DisplayName("렌더링 메시지를 JSON으로 직렬화해 SQS에 전송한다")
    void publish() {
        AwsSqsProperties properties =
                new AwsSqsProperties();

        properties.setRenderQueueUrl(
                "https://sqs.ap-northeast-2.amazonaws.com/123/chaerok-render-dev"
        );

        ObjectMapper objectMapper =
                new ObjectMapper()
                        .registerModule(new JavaTimeModule());

        RenderQueuePublisher publisher =
                new RenderQueuePublisher(
                        sqsClient,
                        properties,
                        objectMapper
                );

        when(sqsClient.sendMessage(
                any(SendMessageRequest.class)
        )).thenReturn(
                SendMessageResponse.builder()
                        .messageId("message-123")
                        .build()
        );

        UUID renderJobId = UUID.randomUUID();

        RenderQueueMessage message =
                new RenderQueueMessage(
                        RenderQueueMessage.CURRENT_SCHEMA_VERSION,
                        renderJobId,
                        2L,
                        6L,
                        1L,
                        "chaerok-media-dev-7f3k2m",
                        "gongju",
                        0.8,
                        1,
                        1,
                        LocalDateTime.of(
                                2026, 7, 30, 22, 0
                        ),
                        List.of(
                                new RenderQueueMessage.PhotoItem(
                                        1L,
                                        1,
                                        "users/6/rolls/2/original/001.jpg",
                                        LocalDateTime.of(
                                                2026, 7, 30, 21, 0
                                        )
                                )
                        )
                );

        RenderQueuePublishResult result =
                publisher.publish(message);

        assertThat(result.messageId())
                .isEqualTo("message-123");

        ArgumentCaptor<SendMessageRequest> captor =
                ArgumentCaptor.forClass(
                        SendMessageRequest.class
                );

        verify(sqsClient).sendMessage(captor.capture());

        SendMessageRequest request = captor.getValue();

        assertThat(request.queueUrl())
                .isEqualTo(properties.getRenderQueueUrl());

        assertThat(request.messageBody())
                .contains(renderJobId.toString())
                .contains("\"schemaVersion\":2")
                .contains("\"filmRollId\":2")
                .contains("\"originalObjectKey\"")
                .doesNotContain("hasFace", "sceneType");

        assertThat(request.messageAttributes())
                .containsKeys(
                        "eventType",
                        "schemaVersion",
                        "renderJobId"
                );
    }
}
