package com.chaerok.render;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RenderHandlerTest {

    private final RenderHandler handler =
            new RenderHandler();

    @Test
    @DisplayName("정상 렌더링 메시지는 실패 항목 없이 처리한다")
    void acceptsValidMessage() {
        SQSEvent.SQSMessage record =
                new SQSEvent.SQSMessage();

        record.setMessageId("message-1");
        record.setBody("""
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
                """);

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(record));

        SQSBatchResponse response =
                handler.handleRequest(
                        event,
                        new TestContext()
                );

        assertThat(response.getBatchItemFailures())
                .isEmpty();
    }

    @Test
    @DisplayName("잘못된 메시지는 해당 messageId만 실패로 반환한다")
    void returnsPartialBatchFailure() {
        SQSEvent.SQSMessage record =
                new SQSEvent.SQSMessage();

        record.setMessageId("message-bad");
        record.setBody("""
                {
                  "schemaVersion": 99
                }
                """);

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(record));

        SQSBatchResponse response =
                handler.handleRequest(
                        event,
                        new TestContext()
                );

        assertThat(response.getBatchItemFailures())
                .extracting(
                        SQSBatchResponse.BatchItemFailure
                                ::getItemIdentifier
                )
                .containsExactly("message-bad");
    }

    private static final class TestContext
            implements Context {

        private final LambdaLogger logger =
                new LambdaLogger() {
                    @Override
                    public void log(String message) {
                        System.out.print(message);
                    }

                    @Override
                    public void log(byte[] message) {
                        System.out.print(
                                new String(
                                        message,
                                        StandardCharsets.UTF_8
                                )
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
            return 1024;
        }

        @Override
        public LambdaLogger getLogger() {
            return logger;
        }
    }
}
