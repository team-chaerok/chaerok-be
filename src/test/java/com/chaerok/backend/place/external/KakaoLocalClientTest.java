package com.chaerok.backend.place.external;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoLocalClientTest {

    @Test
    void categorySearchStopsAtRemainingBudget() {
        AtomicInteger calls = new AtomicInteger();
        ExchangeFunction exchange = request -> {
            calls.incrementAndGet();
            return Mono.never();
        };
        KakaoLocalClient client = clientWithExchange(exchange);

        long startedAt = System.nanoTime();
        assertThat(client.searchPlacesByCategory(
                "FD6", BigDecimal.ONE, BigDecimal.ONE, 2000, Duration.ofMillis(25)))
                .isEmpty();

        assertThat(calls).hasValue(1);
        assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofSeconds(1));
    }

    @Test
    void existingCategorySearchOverloadRemainsAvailable() {
        AtomicInteger calls = new AtomicInteger();
        ExchangeFunction exchange = request -> {
            calls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body("{\"documents\":[]}")
                    .build());
        };
        KakaoLocalClient client = clientWithExchange(exchange);

        assertThat(client.searchPlacesByCategory(
                "FD6", BigDecimal.ONE, BigDecimal.ONE, 2000)).isEmpty();
        assertThat(calls).hasValue(1);
    }

    private KakaoLocalClient clientWithExchange(ExchangeFunction exchange) {
        KakaoLocalClient client = new KakaoLocalClient(WebClient.builder().exchangeFunction(exchange));
        ReflectionTestUtils.setField(client, "restApiKey", "test-key");
        return client;
    }
}
