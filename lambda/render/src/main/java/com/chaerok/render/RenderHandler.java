package com.chaerok.render;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.chaerok.render.media.MediaGenerationException;
import com.chaerok.render.message.RenderQueueMessage;
import com.chaerok.render.output.RenderOutput;
import com.chaerok.render.pipeline.RenderPipeline;
import com.chaerok.render.pipeline.RenderPipelineException;
import com.chaerok.render.result.RenderResultMessage;
import com.chaerok.render.result.RenderResultPublisher;
import com.chaerok.render.retry.RenderRetryConfig;
import com.chaerok.render.storage.ObjectStorageOperationException;
import com.chaerok.render.validation.InvalidRenderMessageException;
import com.chaerok.render.validation.RenderMessageValidator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RenderHandler
        implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;
    private static final String RECEIVE_COUNT_ATTRIBUTE =
            "ApproximateReceiveCount";

    private final ObjectMapper objectMapper;
    private final RenderMessageValidator validator;
    private final RenderPipeline renderPipeline;
    private final RenderResultPublisher resultPublisher;
    private final Clock clock;
    private final RenderRetryConfig retryConfig;

    public RenderHandler() {
        this(RenderRuntimeFactory.objectMapper());
    }

    private RenderHandler(ObjectMapper objectMapper) {
        this(
                objectMapper,
                new RenderMessageValidator(),
                RenderRuntimeFactory.renderPipeline(objectMapper),
                RenderRuntimeFactory.resultPublisher(objectMapper),
                Clock.systemUTC(),
                RenderRuntimeFactory.retryConfig()
        );
    }

    RenderHandler(
            ObjectMapper objectMapper,
            RenderMessageValidator validator,
            RenderPipeline renderPipeline,
            RenderResultPublisher resultPublisher,
            Clock clock,
            RenderRetryConfig retryConfig
    ) {
        this.objectMapper = require(objectMapper, "objectMapper");
        this.validator = require(validator, "validator");
        this.renderPipeline = require(
                renderPipeline,
                "renderPipeline"
        );
        this.resultPublisher = require(
                resultPublisher,
                "resultPublisher"
        );
        this.clock = require(clock, "clock");
        this.retryConfig = require(retryConfig, "retryConfig");
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
        int attempt = receiveCount(record);

        RenderOutput output;

        try {
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
                            + ", attempt="
                            + attempt
            );

            output = renderPipeline.execute(
                    message,
                    detail -> log(
                            context,
                            "renderJobId="
                                    + message.renderJobId()
                                    + ", "
                                    + detail
                    )
            );
        } catch (Exception exception) {
            handleRenderFailure(
                    message,
                    record.getMessageId(),
                    attempt,
                    exception,
                    context
            );
            return;
        }

        RenderResultMessage result =
                RenderResultMessage.completed(
                        message,
                        output,
                        record.getMessageId(),
                        attempt
                );

        String resultMessageId = resultPublisher.publish(result);

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
                        + ", resultMessageId="
                        + resultMessageId
        );
    }

    private void handleRenderFailure(
            RenderQueueMessage message,
            String requestMessageId,
            int attempt,
            Exception renderException,
            Context context
    ) throws Exception {
        boolean retryable = isRetryable(renderException);
        boolean finalAttempt = retryConfig.isFinalAttempt(attempt);

        if (retryable && !finalAttempt) {
            log(
                    context,
                    "Retryable render failure will be retried without "
                            + "publishing a terminal FAILED result: "
                            + "renderJobId="
                            + message.renderJobId()
                            + ", attempt="
                            + attempt
                            + ", maxReceiveCount="
                            + retryConfig.maxReceiveCount()
            );
            throw renderException;
        }

        boolean failurePublished = publishFailureSafely(
                message,
                requestMessageId,
                attempt,
                renderException,
                context
        );

        if (!retryable && failurePublished) {
            log(
                    context,
                    "Non-retryable render failure was published and "
                            + "acknowledged: renderJobId="
                            + message.renderJobId()
            );
            return;
        }

        if (retryable && finalAttempt && failurePublished) {
            log(
                    context,
                    "Terminal retryable render failure was published; "
                            + "the request remains failed so SQS can move "
                            + "it to the request DLQ: renderJobId="
                            + message.renderJobId()
                            + ", attempt="
                            + attempt
            );
        }

        throw renderException;
    }

    private boolean publishFailureSafely(
            RenderQueueMessage message,
            String requestMessageId,
            int attempt,
            Exception renderException,
            Context context
    ) {
        if (!hasResultIdentifiers(message)) {
            log(
                    context,
                    "Skipped FAILED result publish because required "
                            + "identifiers are missing."
            );
            return false;
        }

        RenderResultMessage failed = RenderResultMessage.failed(
                message,
                requestMessageId,
                attempt,
                false,
                Instant.now(clock),
                errorCode(renderException),
                safeMessage(renderException)
        );

        try {
            String resultMessageId =
                    resultPublisher.publish(failed);

            log(
                    context,
                    "Terminal FAILED result published: renderJobId="
                            + message.renderJobId()
                            + ", resultMessageId="
                            + resultMessageId
                            + ", attempt="
                            + failed.attempt()
            );
            return true;
        } catch (Exception publishException) {
            renderException.addSuppressed(publishException);
            log(
                    context,
                    "FAILED result publish failed: renderJobId="
                            + message.renderJobId()
                            + ", errorType="
                            + publishException.getClass().getSimpleName()
                            + ", error="
                            + safeMessage(publishException)
            );
            return false;
        }
    }

    private boolean hasResultIdentifiers(RenderQueueMessage message) {
        return message != null
                && message.renderJobId() != null
                && message.filmRollId() != null
                && message.userId() != null;
    }

    private boolean isRetryable(Exception exception) {
        return !hasCause(
                exception,
                InvalidRenderMessageException.class
        ) && !hasCause(exception, IllegalArgumentException.class);
    }

    private String errorCode(Exception exception) {
        if (hasCause(exception, InvalidRenderMessageException.class)) {
            return "INVALID_RENDER_MESSAGE";
        }
        if (hasCause(exception, IllegalArgumentException.class)) {
            return "INVALID_RENDER_REQUEST";
        }
        if (hasCause(
                exception,
                ObjectStorageOperationException.class
        )) {
            return "OBJECT_STORAGE_FAILED";
        }
        if (hasCause(exception, MediaGenerationException.class)) {
            return "MEDIA_GENERATION_FAILED";
        }
        if (hasCause(exception, RenderPipelineException.class)) {
            return "RENDER_PIPELINE_FAILED";
        }
        return "RENDER_EXECUTION_FAILED";
    }

    private boolean hasCause(
            Throwable throwable,
            Class<? extends Throwable> expectedType
    ) {
        Throwable current = throwable;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private int receiveCount(SQSEvent.SQSMessage record) {
        Map<String, String> attributes = record.getAttributes();
        if (attributes == null) {
            return 1;
        }

        String raw = attributes.get(RECEIVE_COUNT_ATTRIBUTE);
        if (raw == null || raw.isBlank()) {
            return 1;
        }

        try {
            return Math.max(Integer.parseInt(raw), 1);
        } catch (NumberFormatException ignored) {
            return 1;
        }
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
        return message.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value;
    }
}
