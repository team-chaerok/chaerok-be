package com.chaerok.backend.odii.dto;

import com.chaerok.backend.odii.external.OdiiStoryItem;

public record OdiiGuideResponse(
        Long placeId,
        String placeTitle,
        boolean heritage,
        boolean audioAvailable,
        AudioGuide audioGuide,
        String guideScript,
        String fallbackOverview,
        OdiiStatus odiiStatus
) {

    public enum OdiiStatus {
        OK, NOT_FOUND, ERROR, NOT_APPLICABLE
    }

    public static OdiiGuideResponse withAudio(
            Long placeId,
            String placeTitle,
            OdiiStoryItem story
    ) {
        return new OdiiGuideResponse(
                placeId,
                placeTitle,
                true,
                true,
                AudioGuide.from(story),
                story.script(),
                null,
                OdiiStatus.OK
        );
    }

    public static OdiiGuideResponse withScript(
            Long placeId,
            String placeTitle,
            OdiiStoryItem story
    ) {
        return new OdiiGuideResponse(
                placeId,
                placeTitle,
                true,
                false,
                null,
                story.script(),
                null,
                OdiiStatus.OK
        );
    }

    public static OdiiGuideResponse withoutOdii(
            Long placeId,
            String placeTitle,
            boolean heritage,
            String fallbackOverview,
            OdiiStatus odiiStatus
    ) {
        return new OdiiGuideResponse(
                placeId,
                placeTitle,
                heritage,
                false,
                null,
                null,
                fallbackOverview,
                odiiStatus
        );
    }

    public record AudioGuide(
            String storyId,
            String title,
            String audioTitle,
            Integer playTime,
            String audioUrl,
            String imageUrl
    ) {

        public static AudioGuide from(OdiiStoryItem story) {
            return new AudioGuide(
                    story.stid(),
                    story.title(),
                    story.audioTitle(),
                    story.playTime(),
                    story.audioUrl(),
                    story.imageUrl()
            );
        }
    }
}
