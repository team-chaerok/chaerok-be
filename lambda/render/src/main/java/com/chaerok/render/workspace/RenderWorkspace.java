package com.chaerok.render.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

public final class RenderWorkspace implements AutoCloseable {

    private final Path root;
    private final Path inputDirectory;
    private final Path filteredDirectory;
    private final Path exportDirectory;

    private RenderWorkspace(Path root) {
        this.root = root;
        this.inputDirectory = root.resolve("input");
        this.filteredDirectory = root.resolve("filtered");
        this.exportDirectory = root.resolve("export");
    }

    public static RenderWorkspace create(UUID renderJobId) {
        if (renderJobId == null) {
            throw new IllegalArgumentException("renderJobId is required.");
        }

        Path root = Path.of(
                System.getProperty("java.io.tmpdir"),
                "chaerok",
                renderJobId.toString()
        );

        RenderWorkspace workspace = new RenderWorkspace(root);
        workspace.reset();
        return workspace;
    }

    private void reset() {
        deleteRecursively(root);

        try {
            Files.createDirectories(inputDirectory);
            Files.createDirectories(filteredDirectory);
            Files.createDirectories(exportDirectory);
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Failed to create render workspace: " + root,
                    exception
            );
        }
    }

    public Path inputPhoto(int sequence) {
        return inputDirectory.resolve("%03d.jpg".formatted(sequence));
    }

    public Path filteredPhoto(int sequence) {
        return filteredDirectory.resolve("%03d.jpg".formatted(sequence));
    }

    public Path filteredDirectory() {
        return filteredDirectory;
    }

    public Path zipFile() {
        return exportDirectory.resolve("filtered-photos.zip");
    }

    public Path reelFile() {
        return exportDirectory.resolve("reel.mp4");
    }

    public Path manifestFile() {
        return exportDirectory.resolve("manifest.json");
    }

    @Override
    public void close() {
        deleteRecursively(root);
    }

    private static void deleteRecursively(Path path) {
        if (path == null || Files.notExists(path)) {
            return;
        }

        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(current -> {
                        try {
                            Files.deleteIfExists(current);
                        } catch (IOException exception) {
                            throw new WorkspaceDeleteRuntimeException(
                                    current,
                                    exception
                            );
                        }
                    });
        } catch (WorkspaceDeleteRuntimeException exception) {
            throw new WorkspaceException(
                    "Failed to clean render workspace: "
                            + exception.path,
                    exception.getCause()
            );
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Failed to read render workspace: " + path,
                    exception
            );
        }
    }

    private static final class WorkspaceDeleteRuntimeException
            extends RuntimeException {

        private final Path path;

        private WorkspaceDeleteRuntimeException(
                Path path,
                Throwable cause
        ) {
            super(cause);
            this.path = path;
        }
    }
}
