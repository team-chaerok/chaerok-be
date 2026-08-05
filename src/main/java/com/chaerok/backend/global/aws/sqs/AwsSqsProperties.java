package com.chaerok.backend.global.aws.sqs;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "aws.sqs")
public class AwsSqsProperties {

    @NotBlank(message = "렌더링 SQS 대기열 URL은 필수입니다.")
    private String renderQueueUrl;

    private String renderResultQueueUrl;

    private boolean renderResultConsumerEnabled = false;

    @Min(value = 1, message = "결과 큐 최대 조회 개수는 1 이상이어야 합니다.")
    @Max(value = 10, message = "결과 큐 최대 조회 개수는 10 이하여야 합니다.")
    private int renderResultMaxMessages = 10;

    @Min(value = 0, message = "결과 큐 대기 시간은 0초 이상이어야 합니다.")
    @Max(value = 20, message = "결과 큐 대기 시간은 20초 이하여야 합니다.")
    private int renderResultWaitTimeSeconds = 20;

    @Min(value = 1, message = "결과 큐 가시성 제한은 1초 이상이어야 합니다.")
    @Max(value = 43200, message = "결과 큐 가시성 제한은 12시간 이하여야 합니다.")
    private int renderResultVisibilityTimeoutSeconds = 60;

    @Min(value = 100, message = "결과 큐 폴링 간격은 100ms 이상이어야 합니다.")
    private long renderResultPollDelayMs = 1000L;

    @AssertTrue(
            message = "결과 큐 소비자를 활성화하려면 결과 큐 URL이 필요합니다."
    )
    public boolean isRenderResultConfigurationValid() {
        return !renderResultConsumerEnabled
                || (renderResultQueueUrl != null
                && !renderResultQueueUrl.isBlank());
    }
}
