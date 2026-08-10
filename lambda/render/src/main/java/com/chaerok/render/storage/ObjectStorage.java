package com.chaerok.render.storage;

import java.nio.file.Path;

public interface ObjectStorage {

    boolean exists(String bucket, String objectKey);

    void download(String bucket, String objectKey, Path destination);

    void upload(
            String bucket,
            String objectKey,
            Path source,
            String contentType
    );
}
