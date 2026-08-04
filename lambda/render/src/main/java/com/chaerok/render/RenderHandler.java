package com.chaerok.render;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.chaerok.render.message.RenderQueueMessage;
import com.chaerok.render.output.RenderOutput;
import com.chaerok.render.pipeline.RenderPipeline;
import com.chaerok.render.validation.RenderMessageValidator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class RenderHandler
        implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private final ObjectMapper objectMapper;
    private final RenderMessageValidator validator;
    private final RenderPipeline renderPipeline;

    public RenderHandler() {
        this.objectMapper = RenderRuntimeFactory.objectMapper();
        this.validator = new RenderMessageValidator();
        this.renderPipeline = RenderRuntimeFactory.renderPipeline(
                objectMapper
        );
    }

    RenderHandler(
            ObjectMapper objectMapper,
            RenderMessageValidator validator,
            RenderPipeline renderPipeline
    ) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.renderPipeline = renderPipeline;
    }

    @Override
    public SQSBatchResponse handleRequest(
            SQSEvent event,
            Context context
    ) {
        List<SQSBatchResponse.BatchItemFailure> failures =
                new ArrayList<>();

        if (event == null || event.getRecords() == null) {
            log(context, "SQS event has no records.");
            return response(failures);
        }

        for (SQSEvent.SQSMessage record : event.getRecords()) {
            String messageId = record == null
                    ? null
                    : record.getMessageId();

            try {
                process(record, context);
            } catch (Exception exception) {
                log(
                        context,
                        "Render message failed: messageId="
                                + messageId
                                + ", errorType="
                                + exception.getClass().getSimpleName()
                                + ", error="
                                + safeMessage(exception)
                );

                if (messageId != null && !messageId.isBlank()) {
                    failures.add(
                            new SQSBatchResponse.BatchItemFailure(
                                    messageId
                            )
                    );
                }
            }
        }

        return response(failures);
    }

    private void process(
            SQSEvent.SQSMessage record,
            Context context
    ) throws Exception {
        if (record == null) {
            throw new IllegalArgumentException(
                    "SQS record is required."
            );
        }

        if (record.getBody() == null
                || record.getBody().isBlank()) {
            throw new IllegalArgumentException(
                    "SQS message body is required."
            );
        }

        RenderQueueMessage message = objectMapper.readValue(
                record.getBody(),
                RenderQueueMessage.class
        );

        validator.validate(message);

        log(
                context,
                "Render started: messageId="
                        + record.getMessageId()
                        + ", renderJobId="
                        + message.renderJobId()
                        + ", filmRollId="
                        + message.filmRollId()
                        + ", photoCount="
                        + message.photos().size()
                        + ", filterId="
                        + message.filterId()
        );

        RenderOutput output = renderPipeline.execute(
                message,
                detail -> log(
                        context,
                        "renderJobId="
                                + message.renderJobId()
                                + ", "
                                + detail
                )
        );

        log(
                context,
                "Render completed: renderJobId="
                        + output.renderJobId()
                        + ", zipObjectKey="
                        + output.zipObjectKey()
                        + ", reelObjectKey="
                        + output.reelObjectKey()
                        + ", manifestObjectKey="
                        + output.manifestObjectKey()
        );
    }

    private SQSBatchResponse response(
            List<SQSBatchResponse.BatchItemFailure> failures
    ) {
        SQSBatchResponse response = new SQSBatchResponse();
        response.setBatchItemFailures(failures);
        return response;
    }

    private void log(Context context, String message) {
        if (context == null || context.getLogger() == null) {
            System.out.println(message);
            return;
        }
        context.getLogger().log(message + "\n");
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getName();
        }
        return message.length() <= 1000
                ? message
                : message.substring(0, 1000);
    }
}
