package com.wasel.backend.mobility.route;

import com.wasel.backend.integration.RoutingClient;
import com.wasel.backend.integration.WeatherClient;
import com.wasel.backend.mobility.GeoUtils;
import com.wasel.backend.mobility.checkpoint.CheckpointStatus;
import com.wasel.backend.mobility.checkpoint.CheckpointRepository;
import com.wasel.backend.mobility.incident.Incident;
import com.wasel.backend.mobility.incident.IncidentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteService {

    private final RoutingClient routingClient;
    private final WeatherClient weatherClient;
    private final IncidentService incidentService;
    private final CheckpointRepository checkpointRepository;

    public RouteService(
            RoutingClient routingClient,
            WeatherClient weatherClient,
            IncidentService incidentService,
            CheckpointRepository checkpointRepository
    ) {
        this.routingClient = routingClient;
        this.weatherClient = weatherClient;
        this.incidentService = incidentService;
        this.checkpointRepository = checkpointRepository;
    }

    public RouteDtos.RouteEstimateResponse estimate(RouteDtos.EstimateRouteRequest request) {
        double minLat = Math.min(request.originLatitude(), request.destinationLatitude()) - 0.05;
        double maxLat = Math.max(request.originLatitude(), request.destinationLatitude()) + 0.05;
        double minLon = Math.min(request.originLongitude(), request.destinationLongitude()) - 0.05;
        double maxLon = Math.max(request.originLongitude(), request.destinationLongitude()) + 0.05;

        RoutingClient.RouteResult routeResult = routingClient.route(
                request.originLatitude(),
                request.originLongitude(),
                request.destinationLatitude(),
                request.destinationLongitude()
        );

        double distanceKm = routeResult != null
                ? routeResult.distanceMeters() / 1000.0
                : GeoUtils.haversineKm(
                request.originLatitude(),
                request.originLongitude(),
                request.destinationLatitude(),
                request.destinationLongitude()
        ) * 1.15;

        double durationMinutes = routeResult != null
                ? routeResult.durationSeconds() / 60.0
                : (distanceKm / 35.0) * 60.0;

        List<String> factors = new ArrayList<>();
        if (routeResult == null) {
            factors.add("Routing provider unavailable; used heuristic straight-line estimate");
        } else {
            factors.add("Base estimate from " + routeResult.provider());
        }

        List<Incident> activeIncidents = incidentService.activeInBoundingBox(minLat, maxLat, minLon, maxLon);
        if (!activeIncidents.isEmpty()) {
            durationMinutes += activeIncidents.size() * 5;
            factors.add("Adjusted for " + activeIncidents.size() + " verified incidents along the route corridor");
        }

        long blockingCheckpoints = checkpointRepository.findAll().stream()
                .filter(checkpoint -> GeoUtils.insideBounds(
                        checkpoint.getLatitude(),
                        checkpoint.getLongitude(),
                        minLat,
                        maxLat,
                        minLon,
                        maxLon))
                .filter(checkpoint -> checkpoint.getCurrentStatus() != CheckpointStatus.OPEN)
                .count();

        if (request.avoidCheckpoints()) {
            if (blockingCheckpoints > 0) {
                distanceKm *= 1.12;
                durationMinutes *= 1.15;
                factors.add("Avoidance mode applied around known checkpoint constraints");
            }
        } else if (blockingCheckpoints > 0) {
            durationMinutes += blockingCheckpoints * 7;
            factors.add("Checkpoint delays included in route estimate");
        }

        if (request.avoidedAreas() != null) {
            for (RouteDtos.AreaConstraint area : request.avoidedAreas()) {
                if (intersectsBounds(request, area)) {
                    distanceKm *= 1.08;
                    durationMinutes *= 1.10;
                    factors.add("Detour applied to avoid area: " + area.name());
                }
            }
        }

        WeatherClient.WeatherSnapshot weather = weatherClient.current(request.destinationLatitude(), request.destinationLongitude());
        if (weather != null && ("Rain".equalsIgnoreCase(weather.condition()) || "Thunderstorm".equalsIgnoreCase(weather.condition()))) {
            durationMinutes *= 1.10;
            factors.add("Weather slowdown applied: " + weather.description());
        }

        return new RouteDtos.RouteEstimateResponse(
                round(distanceKm),
                round(durationMinutes),
                factors,
                routeResult != null ? routeResult.provider() : "HEURISTIC"
        );
    }

    public List<RouteDtos.GeocodeResponse> geocode(String query) {
        return routingClient.geocode(query).stream()
                .map(result -> new RouteDtos.GeocodeResponse(result.displayName(), result.latitude(), result.longitude()))
                .toList();
    }

    private boolean intersectsBounds(RouteDtos.EstimateRouteRequest request, RouteDtos.AreaConstraint area) {
        double routeMinLat = Math.min(request.originLatitude(), request.destinationLatitude());
        double routeMaxLat = Math.max(request.originLatitude(), request.destinationLatitude());
        double routeMinLon = Math.min(request.originLongitude(), request.destinationLongitude());
        double routeMaxLon = Math.max(request.originLongitude(), request.destinationLongitude());
        return routeMinLat <= area.maxLatitude()
                && routeMaxLat >= area.minLatitude()
                && routeMinLon <= area.maxLongitude()
                && routeMaxLon >= area.minLongitude();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
