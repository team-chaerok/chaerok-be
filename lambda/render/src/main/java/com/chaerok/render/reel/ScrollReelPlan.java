package com.chaerok.render.reel;

public record ScrollReelPlan(
        int canvasWidth,
        int canvasHeight,
        int stripHeight,
        int scrollDistance,
        double totalDurationSeconds,
        double startHoldSeconds,
        double endHoldSeconds,
        double scrollDurationSeconds
) {

    public static ScrollReelPlan calculate(
            FilmStrip strip,
            ReelTemplate template
    ) {
        if (strip == null) {
            throw new IllegalArgumentException("strip is required.");
        }
        if (template == null) {
            throw new IllegalArgumentException("template is required.");
        }
        if (strip.width() != template.canvasWidth()) {
            throw new IllegalArgumentException(
                    "Film strip width must match reel canvas width."
            );
        }
        if (strip.height() < template.canvasHeight()) {
            throw new IllegalArgumentException(
                    "Film strip height must be at least reel canvas height."
            );
        }

        double totalDuration = Math.min(
                template.baseDurationSeconds()
                        + strip.photoCount()
                        * template.perPhotoDurationSeconds(),
                template.maxDurationSeconds()
        );
        double scrollDuration = totalDuration
                - template.startHoldSeconds()
                - template.endHoldSeconds();
        int scrollDistance = strip.height()
                - template.canvasHeight();

        if (scrollDistance > 0 && scrollDuration <= 0.0) {
            throw new IllegalArgumentException(
                    "Scroll duration must be positive when scrolling is required."
            );
        }

        return new ScrollReelPlan(
                template.canvasWidth(),
                template.canvasHeight(),
                strip.height(),
                scrollDistance,
                totalDuration,
                template.startHoldSeconds(),
                template.endHoldSeconds(),
                Math.max(0.0, scrollDuration)
        );
    }

    public double scrollEndSeconds() {
        return totalDurationSeconds - endHoldSeconds;
    }
}
