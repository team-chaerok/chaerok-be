package com.chaerok.backend.place.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourApiWarmup {

    private static final Duration WARMUP_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient tourApiWebClient;

    @Value("${external.tour-api.key}")
    private String serviceKey;

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        long start = System.currentTimeMillis();

        tourApiWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/areaBasedList2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "Chaerok")
                        .queryParam("_type", "json")
                        .queryParam("numOfRows", 1)
                        .queryParam("pageNo", 1)
                        .queryParam("arrange", "A")
                        .queryParam("lDongRegnCd", "44")
                        .queryParam("lDongSignguCd", "150")
                        .build())
                .retrieve()
                .toBodilessEntity()
                .timeout(WARMUP_TIMEOUT)
                .subscribe(
                        response -> log.info(
                                "TourAPI warmup completed. elapsed={}ms",
                                System.currentTimeMillis() - start
                        ),
                        error -> log.warn(
                                "TourAPI warmup failed. elapsed={}ms, message={}",
                                System.currentTimeMillis() - start,
                                error.getMessage()
                        )
                );
    }
}