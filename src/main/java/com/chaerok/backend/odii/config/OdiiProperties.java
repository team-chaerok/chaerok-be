package com.chaerok.backend.odii.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "external.odii")
public class OdiiProperties {

    private String baseUrl;
    private String serviceKey;
    private String mobileOs;
    private String mobileApp;
    private String languageCode;
}