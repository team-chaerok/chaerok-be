package com.chaerok.render.reel;

import java.util.Map;

public final class ReelTemplateRegistry {

    public static final String GONGJU_V1 = "gongju-v1";

    private final Map<String, ReelTemplate> templates;

    public ReelTemplateRegistry() {
        this.templates = Map.of(
                GONGJU_V1,
                new ReelTemplate(
                        GONGJU_V1,
                        1080,
                        1920,
                        1080,
                        700,
                        new PhotoSlot(120, 70, 840, 560),
                        260,
                        360,
                        0.8,
                        1.2,
                        10.0,
                        0.9,
                        32.0,
                        "reel/templates/gongju/film-cell-v1.png"
                )
        );
    }

    public ReelTemplate require(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId is required.");
        }

        ReelTemplate template = templates.get(templateId);
        if (template == null) {
            throw new IllegalArgumentException(
                    "Unsupported reel template: " + templateId
            );
        }
        return template;
    }
}
