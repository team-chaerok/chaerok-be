package com.chaerok.render.reel;

public record PhotoSlot(
        int x,
        int y,
        int width,
        int height
) {

    public PhotoSlot {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException(
                    "Photo slot coordinates must be non-negative."
            );
        }
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException(
                    "Photo slot dimensions must be positive."
            );
        }
    }
}
