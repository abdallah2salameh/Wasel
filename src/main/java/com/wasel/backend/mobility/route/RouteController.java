package com.wasel.backend.mobility.route;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/estimate")
    public RouteDtos.RouteEstimateResponse estimate(@Valid @RequestBody RouteDtos.EstimateRouteRequest request) {
        return routeService.estimate(request);
    }

    @GetMapping("/geocode")
    public List<RouteDtos.GeocodeResponse> geocode(@RequestParam String q) {
        return routeService.geocode(q);
    }
}
