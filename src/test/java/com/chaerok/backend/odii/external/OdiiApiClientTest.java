package com.chaerok.backend.odii.external;

import com.chaerok.backend.odii.config.OdiiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OdiiApiClientTest {

    @Test
    void 정상적인_빈_테마와_스토리_응답은_빈_목록이다() {
        OdiiApiClient client = clientWithResponse(HttpStatus.OK, successBody("{}"));
        OdiiApiClient emptyArrayClient = clientWithResponse(HttpStatus.OK, successBody("[]"));

        assertThat(client.searchThemes("공산성")).isEmpty();
        assertThat(client.getStories("theme-1", "list-1")).isEmpty();
        assertThat(emptyArrayClient.searchThemes("공산성")).isEmpty();
    }

    @Test
    void 정상적인_테마와_스토리_항목을_그대로_읽는다() {
        OdiiApiClient themeClient = clientWithResponse(
                HttpStatus.OK,
                successBody("{\"item\":{\"tid\":\"theme-1\",\"tlid\":\"list-1\",\"title\":\"공산성\"}}")
        );
        OdiiApiClient storyClient = clientWithResponse(
                HttpStatus.OK,
                successBody("{\"item\":{\"title\":\"공산성\",\"script\":\"해설\",\"audioUrl\":\"https://example.com/audio.mp3\"}}")
        );

        assertThat(themeClient.searchThemes("공산성")).singleElement()
                .satisfies(theme -> assertThat(theme.tid()).isEqualTo("theme-1"));
        assertThat(storyClient.getStories("theme-1", "list-1")).singleElement()
                .satisfies(story -> {
                    assertThat(story.script()).isEqualTo("해설");
                    assertThat(story.audioUrl()).isEqualTo("https://example.com/audio.mp3");
                });
    }

    @Test
    void 테마와_스토리의_HTTP_오류를_전달한다() {
        OdiiApiClient client = clientWithResponse(HttpStatus.BAD_GATEWAY, "error");

        assertThatThrownBy(() -> client.searchThemes("공산성"))
                .isInstanceOf(OdiiApiException.class);
        assertThatThrownBy(() -> client.getStories("theme-1", "list-1"))
                .isInstanceOf(OdiiApiException.class);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void 테마와_스토리의_타임아웃을_전달한다(CapturedOutput output) {
        OdiiApiClient client = clientWithError(new TimeoutException("timeout"));

        assertThatThrownBy(() -> client.searchThemes("공산성"))
                .isInstanceOf(OdiiApiException.class)
                .hasCauseInstanceOf(TimeoutException.class);
        assertThatThrownBy(() -> client.getStories("theme-1", "list-1"))
                .isInstanceOf(OdiiApiException.class)
                .hasCauseInstanceOf(TimeoutException.class);
        assertThat(output).contains("exceptionType=TimeoutException");
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void 연결_실패를_전달한다(CapturedOutput output) {
        OdiiApiClient client = clientWithError(new WebClientRequestException(
                new ConnectException("connection refused"),
                HttpMethod.GET,
                URI.create("https://odii.example.com/themeSearchList?serviceKey=secret-value"),
                HttpHeaders.EMPTY
        ));

        assertThatThrownBy(() -> client.searchThemes("공산성"))
                .isInstanceOf(OdiiApiException.class);
        assertThat(output).contains("exceptionType=WebClientRequestException")
                .doesNotContain("secret-value")
                .doesNotContain("https://odii.example.com/themeSearchList");
    }

    @Test
    void 예상하지_못한_프로그래밍_오류는_외부_API_오류로_바꾸지_않는다() {
        OdiiApiClient client = clientWithError(new IllegalStateException("unexpected"));

        assertThatThrownBy(() -> client.searchThemes("공산성"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 명시적_API_오류를_전달한다() {
        OdiiApiClient client = clientWithResponse(
                HttpStatus.OK,
                "{\"response\":{\"header\":{\"resultCode\":\"9999\",\"resultMsg\":\"error\"},\"body\":{}}}"
        );

        assertThatThrownBy(() -> client.searchThemes("공산성"))
                .isInstanceOf(OdiiApiException.class);
        assertThatThrownBy(() -> client.getStories("theme-1", "list-1"))
                .isInstanceOf(OdiiApiException.class);
    }

    @Test
    void 비정상_응답_구조를_오류로_전달한다() {
        OdiiApiClient missingBody = clientWithResponse(
                HttpStatus.OK,
                "{\"response\":{\"header\":{\"resultCode\":\"0000\"}}}"
        );
        OdiiApiClient invalidItem = clientWithResponse(
                HttpStatus.OK,
                successBody("{\"item\":42}")
        );

        assertThatThrownBy(() -> missingBody.searchThemes("공산성"))
                .isInstanceOf(OdiiApiException.class);
        assertThatThrownBy(() -> invalidItem.getStories("theme-1", "list-1"))
                .isInstanceOf(OdiiApiException.class);
    }

    @Test
    void 필수_테마_필드가_없으면_오류로_전달한다() {
        OdiiApiClient client = clientWithResponse(
                HttpStatus.OK,
                successBody("{\"item\":{\"title\":\"공산성\"}}")
        );

        assertThatThrownBy(() -> client.searchThemes("공산성"))
                .isInstanceOf(OdiiApiException.class);
    }

    @Test
    void JSON_파싱_실패를_전달한다() {
        OdiiApiClient client = clientWithResponse(HttpStatus.OK, "{invalid");

        assertThatThrownBy(() -> client.searchThemes("공산성"))
                .isInstanceOf(OdiiApiException.class);
    }

    @Test
    void emptyStringItemsWithZeroCountAreEmptyForThemesAndStories() {
        OdiiApiClient client = clientWithResponse(HttpStatus.OK, emptyStringBody(0));

        assertThat(client.searchThemes("향천사")).isEmpty();
        assertThat(client.getStories("theme-1", "list-1")).isEmpty();
    }

    @Test
    void nullAndEmptyArrayItemsAreEmptyForThemesAndStories() {
        for (String items : new String[]{"null", "[]"}) {
            OdiiApiClient client = clientWithResponse(HttpStatus.OK, successBody(items));

            assertThat(client.searchThemes("향천사")).isEmpty();
            assertThat(client.getStories("theme-1", "list-1")).isEmpty();
        }
    }

    @Test
    void unexpectedStringItemsRemainErrorsForThemesAndStories() {
        for (String response : new String[]{
                emptyStringBody(1),
                successBody("\"\""),
                successBody("\"unexpected\"")
        }) {
            OdiiApiClient client = clientWithResponse(HttpStatus.OK, response);

            assertThatThrownBy(() -> client.searchThemes("향천사"))
                    .isInstanceOf(OdiiApiException.class);
            assertThatThrownBy(() -> client.getStories("theme-1", "list-1"))
                    .isInstanceOf(OdiiApiException.class);
        }
    }

    @Test
    void http500IsRetriedOnceAndThenSucceedsForThemesAndStories() {
        AtomicInteger themeAttempts = new AtomicInteger();
        OdiiApiClient themeClient = clientWithExchange(request -> response(
                themeAttempts.incrementAndGet() == 1 ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.OK,
                successBody("{}")));
        AtomicInteger storyAttempts = new AtomicInteger();
        OdiiApiClient storyClient = clientWithExchange(request -> response(
                storyAttempts.incrementAndGet() == 1 ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.OK,
                successBody("{}")));

        assertThat(themeClient.searchThemes("공산성", Duration.ofSeconds(2))).isEmpty();
        assertThat(storyClient.getStories("theme-1", "list-1", Duration.ofSeconds(2))).isEmpty();
        assertThat(themeAttempts).hasValue(2);
        assertThat(storyAttempts).hasValue(2);
    }

    @Test
    void consecutiveHttp500FailsAfterOneRetry() {
        AtomicInteger attempts = new AtomicInteger();
        OdiiApiClient client = clientWithExchange(request -> {
            attempts.incrementAndGet();
            return response(HttpStatus.INTERNAL_SERVER_ERROR, "error");
        });

        assertThatThrownBy(() -> client.searchThemes("공산성", Duration.ofSeconds(2)))
                .isInstanceOf(OdiiApiException.class);
        assertThatThrownBy(() -> client.getStories("theme-1", "list-1", Duration.ofSeconds(2)))
                .isInstanceOf(OdiiApiException.class);
        assertThat(attempts).hasValue(4);
    }

    @Test
    void http4xxEmptyAndMalformedResultsAreNotRetried() {
        for (HttpStatus status : new HttpStatus[]{HttpStatus.BAD_REQUEST, HttpStatus.TOO_MANY_REQUESTS}) {
            AtomicInteger attempts = new AtomicInteger();
            OdiiApiClient client = clientWithExchange(request -> {
                attempts.incrementAndGet();
                return response(status, "error");
            });

            assertThatThrownBy(() -> client.searchThemes("공산성", Duration.ofSeconds(2)))
                    .isInstanceOf(OdiiApiException.class);
            assertThatThrownBy(() -> client.getStories("theme-1", "list-1", Duration.ofSeconds(2)))
                    .isInstanceOf(OdiiApiException.class);
            assertThat(attempts).hasValue(2);
        }

        AtomicInteger emptyAttempts = new AtomicInteger();
        OdiiApiClient emptyClient = clientWithExchange(request -> {
            emptyAttempts.incrementAndGet();
            return response(HttpStatus.OK, emptyStringBody(0));
        });
        assertThat(emptyClient.searchThemes("향천사", Duration.ofSeconds(2))).isEmpty();
        assertThat(emptyClient.getStories("theme-1", "list-1", Duration.ofSeconds(2))).isEmpty();
        assertThat(emptyAttempts).hasValue(2);

        AtomicInteger malformedAttempts = new AtomicInteger();
        OdiiApiClient malformedClient = clientWithExchange(request -> {
            malformedAttempts.incrementAndGet();
            return response(HttpStatus.OK, "{invalid");
        });
        assertThatThrownBy(() -> malformedClient.searchThemes("공산성", Duration.ofSeconds(2)))
                .isInstanceOf(OdiiApiException.class);
        assertThatThrownBy(() -> malformedClient.getStories("theme-1", "list-1", Duration.ofSeconds(2)))
                .isInstanceOf(OdiiApiException.class);
        assertThat(malformedAttempts).hasValue(2);
    }

    @Test
    void themeAndStoryTimeoutsDoNotRetry() {
        AtomicInteger attempts = new AtomicInteger();
        OdiiApiClient client = clientWithExchange(request -> {
            attempts.incrementAndGet();
            return Mono.never();
        });

        assertThatThrownBy(() -> client.searchThemes("공산성", Duration.ofMillis(100)))
                .isInstanceOf(OdiiApiException.class);
        assertThatThrownBy(() -> client.getStories("theme-1", "list-1", Duration.ofMillis(100)))
                .isInstanceOf(OdiiApiException.class);
        assertThat(attempts).hasValue(2);
    }

    @Test
    void retryBackoffIsIncludedInTotalTimeBudget() {
        AtomicInteger attempts = new AtomicInteger();
        OdiiApiClient client = clientWithExchange(request -> {
            attempts.incrementAndGet();
            return response(HttpStatus.INTERNAL_SERVER_ERROR, "error");
        });
        long startedAt = System.nanoTime();

        assertThatThrownBy(() -> client.searchThemes("공산성", Duration.ofMillis(50)))
                .isInstanceOf(OdiiApiException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofSeconds(1));
        assertThat(attempts).hasValue(1);
    }

    @Test
    void secondAttemptIsStoppedByTotalTimeBudget() {
        AtomicInteger attempts = new AtomicInteger();
        OdiiApiClient client = clientWithExchange(request ->
                attempts.incrementAndGet() == 1
                        ? response(HttpStatus.INTERNAL_SERVER_ERROR, "error")
                        : Mono.never());
        long startedAt = System.nanoTime();

        assertThatThrownBy(() -> client.getStories("theme-1", "list-1", Duration.ofMillis(350)))
                .isInstanceOf(OdiiApiException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofSeconds(1));
        assertThat(attempts).hasValue(2);
    }

    private Mono<ClientResponse> response(HttpStatus status, String body) {
        return Mono.just(ClientResponse.create(status)
                .header("Content-Type", "application/json")
                .body(body)
                .build());
    }

    private String emptyStringBody(int totalCount) {
        return "{\"response\":{\"header\":{\"resultCode\":\"0000\"},\"body\":{\"items\":\"\",\"totalCount\":"
                + totalCount + "}}}";
    }

    private String successBody(String items) {
        return "{\"response\":{\"header\":{\"resultCode\":\"0000\"},\"body\":{\"items\":"
                + items + "}}}";
    }

    private OdiiApiClient clientWithResponse(HttpStatus status, String body) {
        return clientWithExchange(request -> Mono.just(
                ClientResponse.create(status)
                        .header("Content-Type", "application/json")
                        .body(body)
                        .build()
        ));
    }

    private OdiiApiClient clientWithError(Exception exception) {
        return clientWithExchange(request -> Mono.error(exception));
    }

    private OdiiApiClient clientWithExchange(ExchangeFunction exchangeFunction) {
        OdiiProperties properties = new OdiiProperties();
        properties.setBaseUrl("https://odii.example.com");
        properties.setServiceKey("test-key");
        properties.setMobileOs("AND");
        properties.setMobileApp("Chaerok");
        properties.setLanguageCode("ko");
        OdiiApiClient client = new OdiiApiClient(properties);
        ReflectionTestUtils.setField(client, "webClient", WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .exchangeFunction(exchangeFunction)
                .build());
        return client;
    }
}
