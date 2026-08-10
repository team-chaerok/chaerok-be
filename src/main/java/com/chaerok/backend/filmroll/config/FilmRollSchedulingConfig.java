package com.chaerok.backend.filmroll.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        prefix = "aws.sqs",
        name = "render-queue-url"
)
public class FilmRollSchedulingConfig {
}
