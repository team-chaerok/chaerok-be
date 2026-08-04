package com.chaerok.render;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.chaerok.render.message.RenderQueueMessage;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.ArrayList;
import java.util.List;

public class RenderHandler
        implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final ObjectMapper objectMapper;

    public RenderHandler() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
                );
    }

    @Override
    public SQSBatchResponse handleRequest(
            SQSEvent event,
            Context context
    ) {
        List<SQSBatchResponse.BatchItemFailure> failures =
                new ArrayList<>();

        if (event == null || event.getRecords() == null) {
            context.getLogger().log(
                    "SQS event has no records.\n"
            );
            return response(failures);
        }

        for (SQSEvent.SQSMessage record : event.getRecords()) {
            try {
                process(record, context);
            } catch (Exception exception) {
                context.getLogger().log(
                        "Render message failed: messageId="
                                + record.getMessageId()
                                + ", error="
                                + exception.getMessage()
                                + "\n"
                );

                failures.add(
                        new SQSBatchResponse.BatchItemFailure(
                                record.getMessageId()
                        )
                );
            }
        }

        return response(failures);
    }

    private void process(
            SQSEvent.SQSMessage record,
            Context context
    ) throws Exception {
        RenderQueueMessage message =
                objectMapper.readValue(
                        record.getBody(),
                        RenderQueueMessage.class
                );

        if (message.schemaVersion()
                != SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported schemaVersion: "
                            + message.schemaVersion()
            );
        }

        if (message.renderJobId() == null) {
            throw new IllegalArgumentException(
                    "renderJobId is required."
            );
        }

        if (message.filmRollId() == null) {
            throw new IllegalArgumentException(
                    "filmRollId is required."
            );
        }

        if (message.bucket() == null
                || message.bucket().isBlank()) {
            throw new IllegalArgumentException(
                    "bucket is required."
            );
        }

        if (message.photos().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one photo is required."
            );
        }

        context.getLogger().log(
                "Render message accepted: "
                        + "messageId=" + record.getMessageId()
                        + ", renderJobId="
                        + message.renderJobId()
                        + ", filmRollId="
                        + message.filmRollId()
                        + ", photoCount="
                        + message.photos().size()
                        + ", filterId="
                        + message.filterId()
                        + "\n"
        );
    }

    private SQSBatchResponse response(
            List<SQSBatchResponse.BatchItemFailure> failures
    ) {
        SQSBatchResponse response =
                new SQSBatchResponse();

        response.setBatchItemFailures(failures);
        return response;
    }
}
