package com.chaerok.backend.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI chaerokOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chaerok API")
                        .description("충남 소도시 여행 기록 서비스 채록 백엔드 API")
                        .version("v1.0.0"));
    }
}