package com.wasel.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integration.weather")
public record WeatherProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        int requestsPerMinute,
        int connectTimeoutSeconds,
        int readTimeoutSeconds
) {
}
