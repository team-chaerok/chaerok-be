package com.chaerok.render.reel;

public final class FilterTemplateSelector {

    private final ReelTemplateRegistry registry;

    public FilterTemplateSelector(ReelTemplateRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("registry is required.");
        }
        this.registry = registry;
    }

    public ReelTemplate select(String filterId) {
        if (filterId == null || filterId.isBlank()) {
            throw new IllegalArgumentException("filterId is required.");
        }

        return switch (filterId) {
            case "gongju",
                 "buyeo",
                 "seosan",
                 "yesan" -> registry.require(
                    ReelTemplateRegistry.GONGJU_V1
            );
            default -> throw new IllegalArgumentException(
                    "Unsupported reel filter template: " + filterId
            );
        };
    }
}