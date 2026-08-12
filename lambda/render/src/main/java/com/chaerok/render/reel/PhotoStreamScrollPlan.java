package com.chaerok.render.reel;

public record PhotoStreamScrollPlan(
        int canvasWidth,
        int canvasHeight,
        int stripWidth,
        int stripX,
        int renderedPitch,
        int scrollStartY,
        int scrollDistance,
        int introPhotoCount,
        double introStepSeconds,
        double introHoldSeconds,
        double scrollDurationSeconds,
        double endHoldSeconds,
        double introDurationSeconds,
        double scrollSegmentDurationSeconds,
        double totalDurationSeconds
) {

    private static final int MAX_VISIBLE_PHOTO_COUNT = 4;
    private static final int STACK_TOP_Y = 72;

    private static final double INTRO_STEP_SECONDS = 0.70;
    private static final double INTRO_HOLD_SECONDS = 0.60;
    private static final double SECONDS_PER_TRANSITION = 1.15;
    private static final double END_HOLD_SECONDS = 1.20;

    public static PhotoStreamScrollPlan calculate(
            PhotoStream stream,
            ReelTemplate template
    ) {
        if (stream == null) {
            throw new IllegalArgumentException(
                    "stream is required."
            );
        }
        if (template == null) {
            throw new IllegalArgumentException(
                    "template is required."
            );
        }

        double scale = template.canvasHeight()
                / (double) template.panelHeight();

        int stripWidth = (int) Math.round(
                stream.width() * scale
        );
        int stripX = (template.canvasWidth() - stripWidth) / 2;
        int renderedPitch = Math.max(
                1,
                (int) Math.round(stream.pitch() * scale)
        );

        int introPhotoCount = Math.min(
                MAX_VISIBLE_PHOTO_COUNT,
                stream.photoCount()
        );

        int visibleTopPhotoOffset = stream.firstPhotoOffset()
                - (introPhotoCount - 1) * stream.pitch();
        int renderedVisibleTopPhotoOffset = (int) Math.round(
                visibleTopPhotoOffset * scale
        );

        int scrollStartY =
                STACK_TOP_Y - renderedVisibleTopPhotoOffset;

        int transitionCount = Math.max(
                0,
                stream.photoCount() - introPhotoCount
        );
        int scrollDistance = (int) Math.round(
                transitionCount
                        * stream.pitch()
                        * scale
        );

        double introDuration =
                introPhotoCount * INTRO_STEP_SECONDS
                        + INTRO_HOLD_SECONDS;
        double scrollDuration =
                transitionCount * SECONDS_PER_TRANSITION;
        double scrollSegmentDuration =
                scrollDuration + END_HOLD_SECONDS;
        double totalDuration =
                introDuration + scrollSegmentDuration;

        return new PhotoStreamScrollPlan(
                template.canvasWidth(),
                template.canvasHeight(),
                stripWidth,
                stripX,
                renderedPitch,
                scrollStartY,
                scrollDistance,
                introPhotoCount,
                INTRO_STEP_SECONDS,
                INTRO_HOLD_SECONDS,
                scrollDuration,
                END_HOLD_SECONDS,
                introDuration,
                scrollSegmentDuration,
                totalDuration
        );
    }

    public int finalScrollY() {
        return scrollStartY + scrollDistance;
    }
}