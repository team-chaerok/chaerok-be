package com.chaerok.render.result;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public record RenderResultQueueConfig(String queueUrl) {

    public static final String ENV_QUEUE_URL =
            "RENDER_RESULT_QUEUE_URL";

    public RenderResultQueueConfig {
        queueUrl = validate(queueUrl);
    }

    public static RenderResultQueueConfig fromEnvironment(
            Map<String, String> environment
    ) {
        if (environment == null) {
            throw new IllegalStateException(
                    "Lambda environment is required."
            );
        }

        return new RenderResultQueueConfig(
                environment.get(ENV_QUEUE_URL)
        );
    }

    private static String validate(String queueUrl) {
        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalStateException(
                    ENV_QUEUE_URL + " environment variable is required."
            );
        }

        String normalized = queueUrl.trim();

        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();

            if (!uri.isAbsolute()
                    || scheme == null
                    || (!("https".equalsIgnoreCase(scheme))
                    && !("http".equalsIgnoreCase(scheme)))
                    || uri.getHost() == null
                    || uri.getHost().isBlank()
                    || uri.getPath() == null
                    || uri.getPath().isBlank()
                    || "/".equals(uri.getPath())) {
                throw invalidQueueUrl();
            }
        } catch (URISyntaxException exception) {
            throw invalidQueueUrl();
        }

        return normalized;
    }

    private static IllegalStateException invalidQueueUrl() {
        return new IllegalStateException(
                ENV_QUEUE_URL + " must be a valid SQS queue URL."
        );
    }
}
