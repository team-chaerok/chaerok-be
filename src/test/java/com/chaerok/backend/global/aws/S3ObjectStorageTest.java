package com.chaerok.backend.global.aws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3ObjectStorage objectStorage;

    @BeforeEach
    void setUp() {
        AwsProperties properties = new AwsProperties();
        properties.setRegion("ap-northeast-2");
        properties.getS3().setBucket(
                "chaerok-media-dev-7f3k2m"
        );
        properties.getS3().setPresignedPutExpiration(
                Duration.ofMinutes(10)
        );
        properties.getS3().setPresignedGetExpiration(
                Duration.ofMinutes(10)
        );
        properties.getS3().setMaxUploadBytes(
                5L * 1024 * 1024
        );

        objectStorage = new S3ObjectStorage(
                s3Client,
                s3Presigner,
                properties
        );
    }

    @Test
    @DisplayName("업로드용 Presigned PUT URL을 생성한다")
    void createPresignedUpload() throws Exception {
        PresignedPutObjectRequest presignedRequest =
                mock(PresignedPutObjectRequest.class);

        Instant expiresAt =
                Instant.parse("2026-07-30T11:00:00Z");

        when(presignedRequest.url())
                .thenReturn(
                        new URL(
                                "https://example-bucket.s3.amazonaws.com/test.jpg"
                        )
                );

        when(presignedRequest.expiration())
                .thenReturn(expiresAt);

        when(presignedRequest.signedHeaders())
                .thenReturn(Map.of(
                        "host",
                        List.of("example-bucket.s3.amazonaws.com"),
                        "content-type",
                        List.of("image/jpeg")
                ));

        when(s3Presigner.presignPutObject(
                any(PutObjectPresignRequest.class)
        )).thenReturn(presignedRequest);

        PresignedUpload result =
                objectStorage.createPresignedUpload(
                        "users/1/rolls/2/original/001-test.jpg",
                        "image/jpeg",
                        1024L
                );

        assertThat(result.objectKey())
                .isEqualTo(
                        "users/1/rolls/2/original/001-test.jpg"
                );

        assertThat(result.uploadUrl())
                .contains("example-bucket.s3.amazonaws.com");

        assertThat(result.expiresAt())
                .isEqualTo(expiresAt);

        assertThat(result.requiredHeaders())
                .containsEntry(
                        "content-type",
                        List.of("image/jpeg")
                )
                .doesNotContainKey("host");

        ArgumentCaptor<PutObjectPresignRequest> captor =
                ArgumentCaptor.forClass(
                        PutObjectPresignRequest.class
                );

        verify(s3Presigner)
                .presignPutObject(captor.capture());

        assertThat(captor.getValue().signatureDuration())
                .isEqualTo(Duration.ofMinutes(10));

        assertThat(captor.getValue().putObjectRequest().bucket())
                .isEqualTo("chaerok-media-dev-7f3k2m");

        assertThat(captor.getValue().putObjectRequest().contentType())
                .isEqualTo("image/jpeg");

        assertThat(captor.getValue().putObjectRequest().contentLength())
                .isEqualTo(1024L);
    }

    @Test
    @DisplayName("최대 크기를 초과하면 Presigned URL을 생성하지 않는다")
    void rejectOversizedUpload() {
        assertThatThrownBy(() ->
                objectStorage.createPresignedUpload(
                        "users/1/rolls/2/original/001-test.jpg",
                        "image/jpeg",
                        5L * 1024 * 1024 + 1
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대");

        verify(s3Presigner, never())
                .presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    @DisplayName("S3 객체 메타데이터를 조회한다")
    void getMetadata() {
        Instant lastModified =
                Instant.parse("2026-07-30T10:00:00Z");

        when(s3Client.headObject(
                any(HeadObjectRequest.class)
        )).thenReturn(
                HeadObjectResponse.builder()
                        .contentLength(1024L)
                        .contentType("image/jpeg")
                        .eTag("\"etag\"")
                        .lastModified(lastModified)
                        .build()
        );

        StoredObjectMetadata metadata =
                objectStorage.getMetadata(
                        "users/1/rolls/2/original/001-test.jpg"
                );

        assertThat(metadata.contentLength()).isEqualTo(1024L);
        assertThat(metadata.contentType()).isEqualTo("image/jpeg");
        assertThat(metadata.lastModified()).isEqualTo(lastModified);
    }

    @Test
    @DisplayName("S3 객체가 없으면 exists는 false를 반환한다")
    void returnFalseWhenObjectDoesNotExist() {
        S3Exception notFound = mock(S3Exception.class);

        when(notFound.statusCode())
                .thenReturn(404);

        when(s3Client.headObject(
                any(HeadObjectRequest.class)
        )).thenThrow(notFound);

        assertThat(objectStorage.exists(
                "users/1/rolls/2/original/001-test.jpg"
        )).isFalse();
    }
}
