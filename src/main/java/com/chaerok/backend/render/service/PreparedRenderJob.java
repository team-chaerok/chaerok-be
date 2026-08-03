package com.chaerok.backend.render.service;

import com.chaerok.backend.render.queue.RenderQueueMessage;

import java.util.UUID;

public record PreparedRenderJob(
        UUID renderJobId,
        Long filmRollId,
        Long userId,
        RenderQueueMessage message
) {
}
