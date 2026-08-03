package com.chaerok.backend.global.aws.sqs;

import com.chaerok.backend.global.aws.AwsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsSqsProperties.class)
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-queue-url"
)
public class AwsSqsConfig {

    @Bean
    public SqsClient sqsClient(AwsProperties awsProperties) {
        ClientOverrideConfiguration overrideConfiguration =
                ClientOverrideConfiguration.builder()
                        .apiCallAttemptTimeout(Duration.ofSeconds(10))
                        .apiCallTimeout(Duration.ofSeconds(20))
                        .build();

        return SqsClient.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(
                        DefaultCredentialsProvider.create()
                )
                .overrideConfiguration(overrideConfiguration)
                .build();
    }
}
