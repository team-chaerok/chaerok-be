package com.chaerok.backend.render.result;

import com.chaerok.backend.global.aws.sqs.AwsSqsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-result-consumer-enabled",
        havingValue = "true"
)
public class RenderResultQueueConsumer {

    private final SqsClient sqsClient;
    private final AwsSqsProperties properties;
    private final RenderResultMessageParser parser;
    private final RenderResultProcessor processor;

    @Scheduled(
            fixedDelayString = "${aws.sqs.render-result-poll-delay-ms:1000}"
    )
    public void poll() {
        String queueUrl = properties.getRenderResultQueueUrl();
        if (queueUrl == null || queueUrl.isBlank()) {
            log.error("렌더링 결과 소비자가 활성화됐지만 결과 큐 URL이 없습니다.");
            return;
        }

        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(
                        properties.getRenderResultMaxMessages()
                )
                .waitTimeSeconds(
                        properties.getRenderResultWaitTimeSeconds()
                )
                .visibilityTimeout(
                        properties.getRenderResultVisibilityTimeoutSeconds()
                )
                .build();

        List<Message> messages;
        try {
            messages = sqsClient.receiveMessage(request).messages();
        } catch (RuntimeException exception) {
            log.error("렌더링 결과 SQS 조회 실패", exception);
            return;
        }

        for (Message message : messages) {
            processOne(queueUrl, message);
        }
    }

    private void processOne(String queueUrl, Message message) {
        try {
            RenderResultMessage result = parser.parse(message.body());
            RenderResultProcessingOutcome outcome = processor.process(
                    result,
                    message.messageId()
            );

            delete(queueUrl, message);

            log.info(
                    "렌더링 결과 SQS 처리 완료: messageId={}, renderJobId={}, outcome={}",
                    message.messageId(),
                    result.renderJobId(),
                    outcome
            );
        } catch (RuntimeException exception) {
            log.error(
                    "렌더링 결과 SQS 처리 실패, 메시지를 삭제하지 않습니다: messageId={}",
                    message.messageId(),
                    exception
            );
        }
    }

    private void delete(String queueUrl, Message message) {
        DeleteMessageRequest request = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build();

        sqsClient.deleteMessage(request);
    }
}
