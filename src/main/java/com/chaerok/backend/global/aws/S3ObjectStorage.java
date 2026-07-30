package com.chaerok.backend.global.aws;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class S3ObjectStorage {

    private static final Duration MAX_PRESIGNED_DURATION =
            Duration.ofDays(7);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsProperties properties;

    public S3ObjectStorage(
            S3Client s3Client,
            S3Presigner s3Presigner,
            AwsProperties properties
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    public PresignedUpload createPresignedUpload(
            String objectKey,
            String contentType,
            long contentLength
    ) {
        validateObjectKey(objectKey);
        validateContentType(contentType);
        validateUploadSize(contentLength);

        Duration expiration =
                properties.getS3().getPresignedPutExpiration();

        validatePresignedDuration(
                expiration,
                "Presigned PUT URL 만료 시간"
        );

        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(getBucket())
                        .key(objectKey)
                        .contentType(contentType)
                        .contentLength(contentLength)
                        .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(expiration)
                        .putObjectRequest(putObjectRequest)
                        .build();

        try {
            PresignedPutObjectRequest presignedRequest =
                    s3Presigner.presignPutObject(presignRequest);

            return new PresignedUpload(
                    objectKey,
                    presignedRequest.url().toString(),
                    presignedRequest.expiration(),
                    getClientRequiredHeaders(
                            presignedRequest.signedHeaders()
                    )
            );
        } catch (RuntimeException exception) {
            throw new ObjectStorageException(
                    "S3 업로드 URL 생성에 실패했습니다.",
                    exception
            );
        }
    }

    public PresignedDownload createPresignedDownload(
            String objectKey
    ) {
        validateObjectKey(objectKey);

        Duration expiration =
                properties.getS3().getPresignedGetExpiration();

        validatePresignedDuration(
                expiration,
                "Presigned GET URL 만료 시간"
        );

        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder()
                        .bucket(getBucket())
                        .key(objectKey)
                        .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(expiration)
                        .getObjectRequest(getObjectRequest)
                        .build();

        try {
            PresignedGetObjectRequest presignedRequest =
                    s3Presigner.presignGetObject(presignRequest);

            return new PresignedDownload(
                    objectKey,
                    presignedRequest.url().toString(),
                    presignedRequest.expiration()
            );
        } catch (RuntimeException exception) {
            throw new ObjectStorageException(
                    "S3 다운로드 URL 생성에 실패했습니다.",
                    exception
            );
        }
    }

    public StoredObjectMetadata getMetadata(String objectKey) {
        validateObjectKey(objectKey);

        HeadObjectRequest request =
                HeadObjectRequest.builder()
                        .bucket(getBucket())
                        .key(objectKey)
                        .build();

        try {
            HeadObjectResponse response =
                    s3Client.headObject(request);

            return new StoredObjectMetadata(
                    objectKey,
                    response.contentLength(),
                    response.contentType(),
                    response.eTag(),
                    response.lastModified()
            );
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new S3ObjectNotFoundException(
                        objectKey,
                        exception
                );
            }

            log.warn(
                    "S3 객체 메타데이터 조회 실패: key={}, statusCode={}",
                    objectKey,
                    exception.statusCode()
            );

            throw new ObjectStorageException(
                    "S3 객체 메타데이터 조회에 실패했습니다.",
                    exception
            );
        }
    }

    public boolean exists(String objectKey) {
        validateObjectKey(objectKey);

        HeadObjectRequest request =
                HeadObjectRequest.builder()
                        .bucket(getBucket())
                        .key(objectKey)
                        .build();

        try {
            s3Client.headObject(request);
            return true;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }

            throw new ObjectStorageException(
                    "S3 객체 존재 여부 확인에 실패했습니다.",
                    exception
            );
        }
    }

    public void delete(String objectKey) {
        validateObjectKey(objectKey);

        DeleteObjectRequest request =
                DeleteObjectRequest.builder()
                        .bucket(getBucket())
                        .key(objectKey)
                        .build();

        try {
            s3Client.deleteObject(request);
        } catch (S3Exception exception) {
            log.warn(
                    "S3 객체 삭제 실패: key={}, statusCode={}",
                    objectKey,
                    exception.statusCode()
            );

            throw new ObjectStorageException(
                    "S3 객체 삭제에 실패했습니다.",
                    exception
            );
        }
    }

    public long getMaxUploadBytes() {
        return properties.getS3().getMaxUploadBytes();
    }

    private String getBucket() {
        return properties.getS3().getBucket();
    }

    private Map<String, List<String>> getClientRequiredHeaders(
            Map<String, List<String>> signedHeaders
    ) {
        Map<String, List<String>> requiredHeaders =
                new LinkedHashMap<>();

        signedHeaders.forEach((name, values) -> {
            /*
             * Host 헤더는 HTTP 클라이언트가 URL을 기준으로 자동 지정하므로
             * 앱이 직접 설정할 헤더 목록에서는 제외합니다.
             */
            if (!"host".equalsIgnoreCase(name)) {
                requiredHeaders.put(
                        name,
                        List.copyOf(values)
                );
            }
        });

        return Map.copyOf(requiredHeaders);
    }

    private void validateObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException(
                    "S3 객체 키는 필수입니다."
            );
        }

        if (objectKey.startsWith("/")
                || objectKey.contains("\\")
                || objectKey.contains("..")) {
            throw new IllegalArgumentException(
                    "S3 객체 키 형식이 올바르지 않습니다."
            );
        }
    }

    private void validateContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException(
                    "Content-Type은 필수입니다."
            );
        }
    }

    private void validateUploadSize(long contentLength) {
        if (contentLength <= 0) {
            throw new IllegalArgumentException(
                    "업로드 파일 크기는 1바이트 이상이어야 합니다."
            );
        }

        long maxUploadBytes =
                properties.getS3().getMaxUploadBytes();

        if (contentLength > maxUploadBytes) {
            throw new IllegalArgumentException(
                    "업로드 파일은 최대 "
                            + maxUploadBytes
                            + "바이트까지 허용됩니다."
            );
        }
    }

    private void validatePresignedDuration(
            Duration duration,
            String fieldName
    ) {
        if (duration == null
                || duration.isZero()
                || duration.isNegative()) {
            throw new IllegalStateException(
                    fieldName + "은 0초보다 길어야 합니다."
            );
        }

        if (duration.compareTo(MAX_PRESIGNED_DURATION) > 0) {
            throw new IllegalStateException(
                    fieldName + "은 7일을 초과할 수 없습니다."
            );
        }
    }
}
