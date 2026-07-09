package com.chaerok.backend.place.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoLocalClient {

    private static final String BASE_URL = "https://dapi.kakao.com";
    private static final String KEYWORD_SEARCH_PATH = "/v2/local/search/keyword.json";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient.Builder webClientBuilder;

    @Value("${external.kakao.api-key}")
    private String restApiKey;

    public List<KakaoPlaceItem> searchPlacesByKeyword(
            String keyword,
            BigDecimal longitude,
            BigDecimal latitude
    ) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        try {
            KakaoPlaceResponse response = webClientBuilder
                    .baseUrl(BASE_URL)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(KEYWORD_SEARCH_PATH)
                            .queryParam("query", keyword)
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("radius", 10000)
                            .queryParam("size", 15)
                            .build())
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .retrieve()
                    .bodyToMono(KakaoPlaceResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response == null) {
                return List.of();
            }

            return response.getItems();
        } catch (WebClientResponseException e) {
            log.warn("Kakao Local keyword search response error. keyword={}, status={}, body={}",
                    keyword, e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (WebClientRequestException e) {
            log.warn("Kakao Local keyword search request error. keyword={}, message={}",
                    keyword, e.getMessage());
            return List.of();
        } catch (RuntimeException e) {
            log.error("Kakao Local keyword search unexpected error. keyword={}, message={}",
                    keyword, e.getMessage(), e);
            return List.of();
        }
    }
}