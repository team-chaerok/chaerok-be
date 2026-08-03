package com.chaerok.backend.global.aws.sqs;

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
}
