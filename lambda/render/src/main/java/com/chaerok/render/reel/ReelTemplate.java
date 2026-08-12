package com.chaerok.render.reel;

import java.util.List;

public record ReelTemplate(
        String templateId,
        int canvasWidth,
        int canvasHeight,
        int panelWidth,
        int panelHeight,
        List<PhotoSlot> photoSlots,
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
        requirePositive(panelWidth, "panelWidth");
        requirePositive(panelHeight, "panelHeight");
        if (photoSlots == null || photoSlots.isEmpty()) {
            throw new IllegalArgumentException("photoSlots are required.");
        }
        photoSlots = List.copyOf(photoSlots);
        for (PhotoSlot slot : photoSlots) {
            if (slot == null) {
                throw new IllegalArgumentException(
                        "Photo slot is required."
                );
            }
            if (slot.x() + slot.width() > panelWidth
                    || slot.y() + slot.height() > panelHeight) {
                throw new IllegalArgumentException(
                        "Photo slot must fit inside the reel panel."
                );
            }
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

    public int photosPerPanel() {
        return photoSlots.size();
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
