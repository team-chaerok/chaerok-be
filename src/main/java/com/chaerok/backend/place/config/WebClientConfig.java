package com.chaerok.backend.place.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    private static final String TOUR_API_BASE_URL =
            "https://apis.data.go.kr/B551011/KorService2";

    @Bean
    public WebClient tourApiWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .metrics(true, uri -> {
                    int queryIndex = uri.indexOf('?');

                    if (queryIndex >= 0) {
                        return uri.substring(0, queryIndex);
                    }

                    return uri;
                });

        return builder
                .baseUrl(TOUR_API_BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}