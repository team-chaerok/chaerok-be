package com.chaerok.backend.global.aws;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {

    @NotBlank(message = "AWS 리전은 필수입니다.")
    private String region = "ap-northeast-2";

    @Valid
    @NotNull(message = "AWS S3 설정은 필수입니다.")
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class S3 {

        @NotBlank(message = "AWS S3 버킷 이름은 필수입니다.")
        private String bucket;

        @NotNull(message = "Presigned PUT URL 만료 시간은 필수입니다.")
        private Duration presignedPutExpiration = Duration.ofMinutes(10);

        @NotNull(message = "Presigned GET URL 만료 시간은 필수입니다.")
        private Duration presignedGetExpiration = Duration.ofMinutes(10);

        @Min(value = 1, message = "최대 업로드 크기는 1바이트 이상이어야 합니다.")
        private long maxUploadBytes = 5L * 1024 * 1024;
    }
}
