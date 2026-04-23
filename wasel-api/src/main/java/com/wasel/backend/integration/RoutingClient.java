package com.wasel.backend.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wasel.backend.config.RoutingProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;

@Component
public class RoutingClient {

    private final RoutingProperties properties;
    private final FixedWindowRateLimiter rateLimiter;
    private final RestClient routingRestClient;
    private final RestClient geocodingRestClient;

    public RoutingClient(RoutingProperties properties, FixedWindowRateLimiter rateLimiter) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.routingRestClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(ClientFactories.simple(Duration.ofSeconds(properties.connectTimeoutSeconds()), Duration.ofSeconds(properties.readTimeoutSeconds())))
                .build();
        this.geocodingRestClient = RestClient.builder()
                .baseUrl(properties.geocodingUrl())
                .requestFactory(ClientFactories.simple(Duration.ofSeconds(properties.connectTimeoutSeconds()), Duration.ofSeconds(properties.readTimeoutSeconds())))
                .defaultHeader("User-Agent", "wasel-palestine-backend/1.0")
                .build();
    }

    @Cacheable("routes")
    public RouteResult route(double originLat, double originLon, double destinationLat, double destinationLon) {
        if (!properties.enabled()) {
            return null;
        }
        rateLimiter.acquire("routing", properties.requestsPerMinute());
        String path = "/route/v1/driving/%s,%s;%s,%s".formatted(originLon, originLat, destinationLon, destinationLat);
        RoutingResponse response = routingRestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).queryParam("overview", "false").build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(RoutingResponse.class);
        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            return null;
        }
        RouteElement route = response.routes().getFirst();
        return new RouteResult(route.distance(), route.duration(), "OSRM");
    }

    @Cacheable("geocoding")
    public List<GeocodingResult> geocode(String query) {
        if (!properties.enabled()) {
            return List.of();
        }
        rateLimiter.acquire("geocoding", properties.requestsPerMinute());
        String uri = UriComponentsBuilder.fromPath("/search")
                .queryParam("format", "jsonv2")
                .queryParam("limit", 5)
                .queryParam("q", query)
                .build()
                .toUriString();
        GeocodingResponse[] response = geocodingRestClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GeocodingResponse[].class);
        if (response == null) {
            return List.of();
        }
        return java.util.Arrays.stream(response)
                .map(item -> new GeocodingResult(item.displayName(), Double.parseDouble(item.lat()), Double.parseDouble(item.lon())))
                .toList();
    }

    public record RouteResult(double distanceMeters, double durationSeconds, String provider) {
    }

    public record GeocodingResult(String displayName, double latitude, double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RoutingResponse(List<RouteElement> routes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RouteElement(double distance, double duration) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeocodingResponse(String displayName, String lat, String lon) {
    }
}
