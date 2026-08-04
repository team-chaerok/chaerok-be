package com.chaerok.render.output;

import com.chaerok.render.message.RenderQueueMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RenderObjectKeysTest {

    @Test
    @DisplayName("같은 renderJobId는 항상 같은 결과 경로를 만든다")
    void createsDeterministicKeys() {
        UUID renderJobId = UUID.fromString(
                "133d6ee3-a120-4df3-8ba3-f60adbdd64d6"
        );

        RenderQueueMessage message = new RenderQueueMessage(
                1,
                renderJobId,
                2L,
                6L,
                1L,
                "bucket",
                "gongju_baekje_love",
                0.8,
                1,
                1,
                LocalDateTime.now(),
                List.of(
                        new RenderQueueMessage.PhotoItem(
                                1L,
                                1,
                                "input.jpg",
                                false,
                                null,
                                LocalDateTime.now()
                        )
                )
        );

        assertThat(RenderObjectKeys.filteredPhoto(message, 1))
                .isEqualTo(
                        "users/6/rolls/2/render-jobs/"
                                + renderJobId
                                + "/filtered/001.jpg"
                );
        assertThat(RenderObjectKeys.zip(message))
                .endsWith("chaerok_1_2_133d6ee3.zip");
        assertThat(RenderObjectKeys.reel(message))
                .endsWith("chaerok_1_2_133d6ee3.mp4");
    }
}
