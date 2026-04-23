package com.wasel.backend.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wasel.backend.config.WeatherProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class WeatherClient {

    private final WeatherProperties properties;
    private final FixedWindowRateLimiter rateLimiter;
    private final RestClient restClient;

    public WeatherClient(WeatherProperties properties, FixedWindowRateLimiter rateLimiter) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(ClientFactories.simple(Duration.ofSeconds(properties.connectTimeoutSeconds()), Duration.ofSeconds(properties.readTimeoutSeconds())))
                .build();
    }

    @Cacheable(value = "weather", key = "#latitude + '-' + #longitude")
    public WeatherSnapshot current(double latitude, double longitude) {
        if (!properties.enabled() || !StringUtils.hasText(properties.apiKey())) {
            return null;
        }
        rateLimiter.acquire("weather", properties.requestsPerMinute());
        WeatherResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/data/2.5/weather")
                        .queryParam("lat", latitude)
                        .queryParam("lon", longitude)
                        .queryParam("appid", properties.apiKey())
                        .queryParam("units", "metric")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(WeatherResponse.class);

        if (response == null || response.weather() == null || response.weather().length == 0) {
            return null;
        }

        WeatherElement primary = response.weather()[0];
        return new WeatherSnapshot(primary.main(), primary.description(), response.main().temp());
    }

    public record WeatherSnapshot(String condition, String description, double temperatureCelsius) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WeatherResponse(WeatherElement[] weather, TemperatureElement main) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WeatherElement(String main, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TemperatureElement(double temp) {
    }
}
