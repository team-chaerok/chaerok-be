package com.chaerok.backend.odii.external;

public record OdiiStoryItem(
        String tid,
        String tlid,
        String stid,
        String stlid,
        String title,
        String mapX,
        String mapY,
        String audioTitle,
        String script,
        Integer playTime,
        String audioUrl,
        String langCode,
        String imageUrl
) {

    public boolean hasPlayableAudio() {
        return audioUrl != null && !audioUrl.isBlank();
    }
}