package com.wasel.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integration.routing")
public record RoutingProperties(
        boolean enabled,
        String baseUrl,
        String geocodingUrl,
        int requestsPerMinute,
        int connectTimeoutSeconds,
        int readTimeoutSeconds
) {
}
