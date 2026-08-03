package com.chaerok.backend.render.queue;

import com.chaerok.backend.global.aws.sqs.AwsSqsProperties;
import com.chaerok.backend.render.exception.RenderQueueException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-queue-url"
)
public class RenderQueuePublisher {

    private final SqsClient sqsClient;
    private final AwsSqsProperties properties;
    private final ObjectMapper objectMapper;

    public RenderQueuePublishResult publish(
            RenderQueueMessage message
    ) {
        SendMessageRequest request =
                SendMessageRequest.builder()
                        .queueUrl(properties.getRenderQueueUrl())
                        .messageBody(serialize(message))
                        .messageAttributes(
                                createMessageAttributes(message)
                        )
                        .build();

        try {
            SendMessageResponse response =
                    sqsClient.sendMessage(request);

            log.info(
                    "렌더링 SQS 전송 성공: renderJobId={}, filmRollId={}, messageId={}",
                    message.renderJobId(),
                    message.filmRollId(),
                    response.messageId()
            );

            return new RenderQueuePublishResult(
                    response.messageId()
            );
        } catch (SqsException exception) {
            throw new RenderQueueException(
                    "렌더링 요청을 SQS에 전송하지 못했습니다.",
                    exception
            );
        }
    }

    private String serialize(RenderQueueMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new RenderQueueException(
                    "렌더링 메시지 직렬화에 실패했습니다.",
                    exception
            );
        }
    }

    private Map<String, MessageAttributeValue>
    createMessageAttributes(RenderQueueMessage message) {
        return Map.of(
                "eventType",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("CHAEROK_RENDER_REQUESTED")
                        .build(),
                "schemaVersion",
                MessageAttributeValue.builder()
                        .dataType("Number")
                        .stringValue(
                                String.valueOf(
                                        message.schemaVersion()
                                )
                        )
                        .build(),
                "renderJobId",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(
                                message.renderJobId().toString()
                        )
                        .build()
        );
    }
}
