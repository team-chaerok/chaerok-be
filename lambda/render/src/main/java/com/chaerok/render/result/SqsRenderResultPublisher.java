package com.chaerok.render.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.Map;

public final class SqsRenderResultPublisher
        implements RenderResultPublisher {

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;

    public SqsRenderResultPublisher(
            SqsClient sqsClient,
            RenderResultQueueConfig config,
            ObjectMapper objectMapper
    ) {
        if (sqsClient == null) {
            throw new IllegalArgumentException(
                    "sqsClient is required."
            );
        }
        if (config == null) {
            throw new IllegalArgumentException(
                    "config is required."
            );
        }
        if (objectMapper == null) {
            throw new IllegalArgumentException(
                    "objectMapper is required."
            );
        }

        this.sqsClient = sqsClient;
        this.queueUrl = config.queueUrl();
        this.objectMapper = objectMapper;
    }

    @Override
    public String publish(RenderResultMessage message) {
        if (message == null) {
            throw new IllegalArgumentException(
                    "Render result message is required."
            );
        }

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(serialize(message))
                .messageAttributes(attributes(message))
                .build();

        try {
            SendMessageResponse response =
                    sqsClient.sendMessage(request);
            return response.messageId();
        } catch (SqsException exception) {
            throw new RenderResultPublishException(
                    "Failed to publish render result to SQS.",
                    exception
            );
        }
    }

    private String serialize(RenderResultMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new RenderResultPublishException(
                    "Failed to serialize render result message.",
                    exception
            );
        }
    }

    private Map<String, MessageAttributeValue> attributes(
            RenderResultMessage message
    ) {
        return Map.of(
                "eventType",
                stringAttribute(message.eventType()),
                "schemaVersion",
                numberAttribute(message.schemaVersion()),
                "renderJobId",
                stringAttribute(message.renderJobId().toString()),
                "status",
                stringAttribute(message.status()),
                "retryable",
                stringAttribute(
                        Boolean.toString(message.retryable())
                )
        );
    }

    private MessageAttributeValue stringAttribute(String value) {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(value)
                .build();
    }

    private MessageAttributeValue numberAttribute(int value) {
        return MessageAttributeValue.builder()
                .dataType("Number")
                .stringValue(Integer.toString(value))
                .build();
    }
}
