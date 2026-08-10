package com.chaerok.render.output;

import com.chaerok.render.message.RenderQueueMessage;

import java.util.UUID;

public final class RenderObjectKeys {

    private RenderObjectKeys() {
    }

    public static String jobPrefix(RenderQueueMessage message) {
        return "users/%d/rolls/%d/render-jobs/%s".formatted(
                message.userId(),
                message.filmRollId(),
                message.renderJobId()
        );
    }

    public static String filteredPhoto(
            RenderQueueMessage message,
            int sequence
    ) {
        return jobPrefix(message)
                + "/filtered/%03d.jpg".formatted(sequence);
    }

    public static String zip(RenderQueueMessage message) {
        return jobPrefix(message)
                + "/export/chaerok_%d_%d_%s.zip".formatted(
                message.regionId(),
                message.filmRollId(),
                shortJobId(message.renderJobId())
        );
    }

    public static String reel(RenderQueueMessage message) {
        return jobPrefix(message)
                + "/export/chaerok_%d_%d_%s.mp4".formatted(
                message.regionId(),
                message.filmRollId(),
                shortJobId(message.renderJobId())
        );
    }

    public static String manifest(RenderQueueMessage message) {
        return jobPrefix(message) + "/manifest.json";
    }

    private static String shortJobId(UUID renderJobId) {
        return renderJobId.toString().substring(0, 8);
    }
}
