package com.chaerok.backend.global.aws;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3ObjectKeyGeneratorTest {

    private final S3ObjectKeyGenerator keyGenerator =
            new S3ObjectKeyGenerator();

    @Test
    @DisplayName("원본 사진 객체 키를 사용자와 필름 롤 기준으로 생성한다")
    void createOriginalPhotoKey() {
        String objectKey =
                keyGenerator.createOriginalPhotoKey(
                        1L,
                        2L,
                        3
                );

        assertThat(objectKey)
                .matches(
                        "users/1/rolls/2/original/"
                                + "003-[a-f0-9]{32}\\.jpg"
                );
    }

    @Test
    @DisplayName("같은 사진 순서라도 서로 다른 객체 키를 생성한다")
    void createUniqueOriginalPhotoKey() {
        String first =
                keyGenerator.createOriginalPhotoKey(
                        1L,
                        2L,
                        3
                );

        String second =
                keyGenerator.createOriginalPhotoKey(
                        1L,
                        2L,
                        3
                );

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("ZIP과 릴스 결과 객체 키를 생성한다")
    void createExportKeys() {
        assertThat(keyGenerator.createZipKey(1L, 2L))
                .isEqualTo(
                        "users/1/rolls/2/export/chaerok_2.zip"
                );

        assertThat(keyGenerator.createReelKey(1L, 2L))
                .isEqualTo(
                        "users/1/rolls/2/export/chaerok_2.mp4"
                );
    }

    @Test
    @DisplayName("잘못된 식별자나 사진 순서는 거부한다")
    void rejectInvalidArguments() {
        assertThatThrownBy(() ->
                keyGenerator.createOriginalPhotoKey(
                        0L,
                        1L,
                        1
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
                keyGenerator.createOriginalPhotoKey(
                        1L,
                        0L,
                        1
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
                keyGenerator.createOriginalPhotoKey(
                        1L,
                        1L,
                        0
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
