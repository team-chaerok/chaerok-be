package com.chaerok.render.media;

import java.nio.file.Path;

public interface ReelRenderer {

    void render(
            Path filteredDirectory,
            int photoCount,
            Path destination
    );
}
