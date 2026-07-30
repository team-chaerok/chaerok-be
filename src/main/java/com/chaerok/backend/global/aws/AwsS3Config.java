package com.chaerok.backend.global.aws;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsProperties.class)
@ConditionalOnProperty(
        prefix = "aws.s3",
        name = "bucket"
)
public class AwsS3Config {

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        /*
         * 로컬:
         * ~/.aws/credentials와 AWS_PROFILE 환경변수를 사용합니다.
         *
         * AWS 배포 환경:
         * EC2, ECS, Lambda 등에 연결된 IAM Role의 임시 자격증명을
         * 자동으로 사용합니다.
         */
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public Region awsRegion(AwsProperties properties) {
        return Region.of(properties.getRegion());
    }

    @Bean
    public S3Client s3Client(
            Region awsRegion,
            AwsCredentialsProvider credentialsProvider
    ) {
        ClientOverrideConfiguration overrideConfiguration =
                ClientOverrideConfiguration.builder()
                        .apiCallAttemptTimeout(Duration.ofSeconds(10))
                        .apiCallTimeout(Duration.ofSeconds(20))
                        .build();

        return S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(credentialsProvider)
                .overrideConfiguration(overrideConfiguration)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(
            Region awsRegion,
            AwsCredentialsProvider credentialsProvider
    ) {
        return S3Presigner.builder()
                .region(awsRegion)
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    public S3ObjectStorage s3ObjectStorage(
            S3Client s3Client,
            S3Presigner s3Presigner,
            AwsProperties properties
    ) {
        return new S3ObjectStorage(
                s3Client,
                s3Presigner,
                properties
        );
    }
}
