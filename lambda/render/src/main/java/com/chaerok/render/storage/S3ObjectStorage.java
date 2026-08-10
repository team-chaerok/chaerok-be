package com.chaerok.render.storage;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class S3ObjectStorage implements ObjectStorage {

    private final S3Client s3Client;

    public S3ObjectStorage(S3Client s3Client) {
        if (s3Client == null) {
            throw new IllegalArgumentException("S3Client is required.");
        }
        this.s3Client = s3Client;
    }

    @Override
    public boolean exists(String bucket, String objectKey) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        try {
            s3Client.headObject(request);
            return true;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw new ObjectStorageOperationException(
                    "S3 object existence check failed: " + objectKey,
                    exception
            );
        }
    }

    @Override
    public void download(
            String bucket,
            String objectKey,
            Path destination
    ) {
        try {
            Files.createDirectories(destination.getParent());

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();

            s3Client.getObject(request, destination);
        } catch (S3Exception | IOException exception) {
            throw new ObjectStorageOperationException(
                    "S3 object download failed: " + objectKey,
                    exception
            );
        }
    }

    @Override
    public void upload(
            String bucket,
            String objectKey,
            Path source,
            String contentType
    ) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromFile(source));
        } catch (S3Exception exception) {
            throw new ObjectStorageOperationException(
                    "S3 object upload failed: " + objectKey,
                    exception
            );
        }
    }
}
