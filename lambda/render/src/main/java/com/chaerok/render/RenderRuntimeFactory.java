package com.chaerok.render;

import com.chaerok.backend.filter.analysis.AdaptiveFilterPolicy;
import com.chaerok.backend.filter.analysis.FilterOverlayTuningPolicy;
import com.chaerok.backend.filter.analysis.ImageSceneAnalyzer;
import com.chaerok.backend.filter.engine.FilmFilterEngine;
import com.chaerok.backend.filter.preset.FilmFilterPresetProvider;
import com.chaerok.backend.filter.processor.OverlayImageCache;
import com.chaerok.render.reel.FilmStripReelRenderer;
import com.chaerok.render.media.FilteredPhotoZipWriter;
import com.chaerok.render.media.JpegImageWriter;
import com.chaerok.render.pipeline.RenderPipeline;
import com.chaerok.render.result.RenderResultPublisher;
import com.chaerok.render.result.RenderResultQueueConfig;
import com.chaerok.render.result.SqsRenderResultPublisher;
import com.chaerok.render.retry.RenderRetryConfig;
import com.chaerok.render.storage.S3ObjectStorage;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Clock;

final class RenderRuntimeFactory {

    private RenderRuntimeFactory() {
    }

    static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
                )
                .disable(
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
                );
    }

    static RenderPipeline renderPipeline(ObjectMapper objectMapper) {
        FilmFilterEngine filmFilterEngine = new FilmFilterEngine(
                new FilmFilterPresetProvider(),
                new OverlayImageCache(),
                new ImageSceneAnalyzer(),
                new AdaptiveFilterPolicy(),
                new FilterOverlayTuningPolicy()
        );

        String ffmpegPath = System.getenv()
                .getOrDefault("FFMPEG_PATH", "/opt/bin/ffmpeg");

        return new RenderPipeline(
                new S3ObjectStorage(S3Client.builder().build()),
                filmFilterEngine,
                new JpegImageWriter(),
                new FilteredPhotoZipWriter(),
                new FilmStripReelRenderer(ffmpegPath),
                objectMapper,
                Clock.systemUTC()
        );
    }

    static RenderRetryConfig retryConfig() {
        return RenderRetryConfig.fromEnvironment(System.getenv());
    }

    static RenderResultPublisher resultPublisher(
            ObjectMapper objectMapper
    ) {
        RenderResultQueueConfig config =
                RenderResultQueueConfig.fromEnvironment(
                        System.getenv()
                );

        return new SqsRenderResultPublisher(
                SqsClient.builder().build(),
                config,
                objectMapper
        );
    }
}
