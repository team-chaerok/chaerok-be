package com.chaerok.render.validation;

import com.chaerok.render.message.RenderQueueMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RenderMessageValidatorTest {

    private final RenderMessageValidator validator =
            new RenderMessageValidator();

    @Test
    @DisplayName("사진 순서가 1부터 연속되지 않으면 거부한다")
    void rejectsNonContiguousSequence() {
        RenderQueueMessage message = messageWithPhotos(
                List.of(
                        photo(1L, 1),
                        photo(2L, 3)
                )
        );

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(InvalidRenderMessageException.class)
                .hasMessageContaining("sequence");
    }

    @Test
    @DisplayName("사진 순서가 중복되면 거부한다")
    void rejectsDuplicateSequence() {
        RenderQueueMessage message = messageWithPhotos(
                List.of(
                        photo(1L, 1),
                        photo(2L, 1)
                )
        );

        assertThatThrownBy(() -> validator.validate(message))
                .isInstanceOf(InvalidRenderMessageException.class)
                .hasMessageContaining("sequence");
    }

    private RenderQueueMessage messageWithPhotos(
            List<RenderQueueMessage.PhotoItem> photos
    ) {
        return new RenderQueueMessage(
                1,
                UUID.randomUUID(),
                1L,
                1L,
                1L,
                "bucket",
                "gongju",
                0.8,
                1,
                photos.size(),
                LocalDateTime.now(),
                photos
        );
    }

    private RenderQueueMessage.PhotoItem photo(
            Long photoId,
            int sequence
    ) {
        return new RenderQueueMessage.PhotoItem(
                photoId,
                sequence,
                "users/1/rolls/1/original/" + sequence + ".jpg",
                false,
                null,
                LocalDateTime.now()
        );
    }
}
