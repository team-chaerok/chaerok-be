package com.chaerok.backend.render.result;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-result-consumer-enabled",
        havingValue = "true"
)
public class RenderResultConsumerConfig {
}
