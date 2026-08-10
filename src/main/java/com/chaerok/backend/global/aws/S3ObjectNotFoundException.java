package com.chaerok.backend.global.aws;

public class S3ObjectNotFoundException extends ObjectStorageException {

    public S3ObjectNotFoundException(
            String objectKey,
            Throwable cause
    ) {
        super(
                "S3 객체를 찾을 수 없습니다: " + objectKey,
                cause
        );
    }
}
