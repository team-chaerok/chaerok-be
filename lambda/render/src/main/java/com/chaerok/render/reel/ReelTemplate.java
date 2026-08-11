package com.chaerok.render.reel;

public record ReelTemplate(
        String templateId,
        int canvasWidth,
        int canvasHeight,
        int cellWidth,
        int cellHeight,
        PhotoSlot photoSlot,
        int topPadding,
        int bottomPadding,
        double startHoldSeconds,
        double endHoldSeconds,
        double baseDurationSeconds,
        double perPhotoDurationSeconds,
        double maxDurationSeconds,
        String overlayResource
) {

    public ReelTemplate {
        requireText(templateId, "templateId");
        requireText(overlayResource, "overlayResource");
        requirePositive(canvasWidth, "canvasWidth");
        requirePositive(canvasHeight, "canvasHeight");
        requirePositive(cellWidth, "cellWidth");
        requirePositive(cellHeight, "cellHeight");
        if (photoSlot == null) {
            throw new IllegalArgumentException("photoSlot is required.");
        }
        if (photoSlot.x() + photoSlot.width() > cellWidth
                || photoSlot.y() + photoSlot.height() > cellHeight) {
            throw new IllegalArgumentException(
                    "Photo slot must fit inside the reel cell."
            );
        }
        if (topPadding < 0 || bottomPadding < 0) {
            throw new IllegalArgumentException(
                    "Reel padding must be non-negative."
            );
        }
        if (startHoldSeconds < 0.0
                || endHoldSeconds < 0.0
                || baseDurationSeconds <= 0.0
                || perPhotoDurationSeconds < 0.0
                || maxDurationSeconds <= 0.0
                || maxDurationSeconds < baseDurationSeconds) {
            throw new IllegalArgumentException(
                    "Reel duration configuration is invalid."
            );
        }
    }

    private static void requirePositive(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
    }
}
