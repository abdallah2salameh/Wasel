package com.wasel.backend.mobility.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class RouteDtos {

    public record EstimateRouteRequest(
            @Min(-90) @Max(90) double originLatitude,
            @Min(-180) @Max(180) double originLongitude,
            @Min(-90) @Max(90) double destinationLatitude,
            @Min(-180) @Max(180) double destinationLongitude,
            boolean avoidCheckpoints,
            @Valid List<AreaConstraint> avoidedAreas
    ) {
    }

    public record AreaConstraint(
            @NotEmpty String name,
            @Min(-90) @Max(90) double minLatitude,
            @Min(-90) @Max(90) double maxLatitude,
            @Min(-180) @Max(180) double minLongitude,
            @Min(-180) @Max(180) double maxLongitude
    ) {
    }

    public record RouteEstimateResponse(
            double estimatedDistanceKm,
            double estimatedDurationMinutes,
            List<String> factors,
            String provider
    ) {
    }

    public record GeocodeResponse(String displayName, double latitude, double longitude) {
    }
}
