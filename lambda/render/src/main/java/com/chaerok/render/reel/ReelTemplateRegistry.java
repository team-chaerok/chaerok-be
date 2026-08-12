package com.chaerok.render.reel;

import java.util.List;
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
                        2276,
                        List.of(
                                new PhotoSlot(170, 76, 774, 516),
                                new PhotoSlot(170, 606, 774, 516),
                                new PhotoSlot(170, 1133, 774, 516),
                                new PhotoSlot(170, 1659, 774, 516)
                        ),
                        0.8,
                        1.2,
                        10.0,
                        0.9,
                        32.0,
                        "reel/templates/gongju/film-panel-4cut-v1.png"
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
