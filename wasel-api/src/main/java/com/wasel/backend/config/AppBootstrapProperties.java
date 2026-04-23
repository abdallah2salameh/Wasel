package com.wasel.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap")
public record AppBootstrapProperties(
        String adminEmail,
        String adminPassword,
        boolean seedAdmin
) {
}
