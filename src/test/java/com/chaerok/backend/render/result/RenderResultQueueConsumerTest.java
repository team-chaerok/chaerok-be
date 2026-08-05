package com.chaerok.backend.render.result;

import com.chaerok.backend.global.aws.sqs.AwsSqsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderResultQueueConsumerTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private RenderResultMessageParser parser;

    @Mock
    private RenderResultProcessor processor;

    private AwsSqsProperties properties;
    private RenderResultQueueConsumer consumer;

    @BeforeEach
    void setUp() {
        properties = new AwsSqsProperties();
        properties.setRenderQueueUrl("request-queue");
        properties.setRenderResultQueueUrl("result-queue");
        properties.setRenderResultMaxMessages(10);
        properties.setRenderResultWaitTimeSeconds(20);
        properties.setRenderResultVisibilityTimeoutSeconds(60);

        consumer = new RenderResultQueueConsumer(
                sqsClient,
                properties,
                parser,
                processor
        );
    }

    @Test
    @DisplayName("DB 트랜잭션 처리가 끝난 결과 메시지만 SQS에서 삭제한다")
    void deleteAfterProcessingSucceeds() {
        Message sqsMessage = Message.builder()
                .messageId("result-message-1")
                .receiptHandle("receipt-1")
                .body("{}")
                .build();

        RenderResultMessage result = failedResultMessage();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(
                        ReceiveMessageResponse.builder()
                                .messages(sqsMessage)
                                .build()
                );
        when(parser.parse("{}")).thenReturn(result);
        when(processor.process(result, "result-message-1"))
                .thenReturn(RenderResultProcessingOutcome.APPLIED);

        consumer.poll();

        ArgumentCaptor<DeleteMessageRequest> deleteCaptor =
                ArgumentCaptor.forClass(DeleteMessageRequest.class);

        verify(sqsClient).deleteMessage(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().queueUrl())
                .isEqualTo("result-queue");
        assertThat(deleteCaptor.getValue().receiptHandle())
                .isEqualTo("receipt-1");
    }

    @Test
    @DisplayName("DB 처리 실패 시 메시지를 삭제하지 않아 SQS 재시도를 유지한다")
    void keepMessageWhenProcessingFails() {
        Message sqsMessage = Message.builder()
                .messageId("result-message-2")
                .receiptHandle("receipt-2")
                .body("{}")
                .build();

        RenderResultMessage result = failedResultMessage();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(
                        ReceiveMessageResponse.builder()
                                .messages(sqsMessage)
                                .build()
                );
        when(parser.parse("{}")).thenReturn(result);
        when(processor.process(result, "result-message-2"))
                .thenThrow(new RuntimeException("DB failure"));

        consumer.poll();

        verify(sqsClient, never())
                .deleteMessage(any(DeleteMessageRequest.class));
    }


    private RenderResultMessage failedResultMessage() {
        return new RenderResultMessage(
                1,
                RenderResultMessage.EVENT_FAILED,
                "request-1",
                UUID.randomUUID(),
                2L,
                3L,
                1L,
                "bucket",
                "FAILED",
                3,
                false,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                Instant.parse("2026-08-05T03:42:31Z"),
                "MEDIA_GENERATION_FAILED",
                "FFmpeg failed"
        );
    }
}
