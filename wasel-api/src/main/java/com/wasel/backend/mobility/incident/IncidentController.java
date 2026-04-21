package com.wasel.backend.mobility.incident;

import com.wasel.backend.common.PageResponse;
import com.wasel.backend.security.UserAccount;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentDtos.IncidentResponse create(
            @Valid @RequestBody IncidentDtos.CreateIncidentRequest request,
            @AuthenticationPrincipal UserAccount actor
    ) {
        return incidentService.create(request, actor);
    }

    @GetMapping
    public PageResponse<IncidentDtos.IncidentResponse> list(
            @RequestParam(required = false) IncidentCategory category,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "reportedAt") String sortBy
    ) {
        return incidentService.list(category, severity, status, verified, page, size, sortBy);
    }

    @GetMapping("/{id}")
    public IncidentDtos.IncidentResponse details(@PathVariable UUID id) {
        return incidentService.details(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    public IncidentDtos.IncidentResponse update(
            @PathVariable UUID id,
            @RequestBody IncidentDtos.UpdateIncidentRequest request
    ) {
        return incidentService.update(id, request);
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    public IncidentDtos.IncidentResponse verify(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserAccount actor
    ) {
        return incidentService.verify(id, actor, reason);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    public IncidentDtos.IncidentResponse close(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserAccount actor
    ) {
        return incidentService.close(id, actor, reason);
    }

    @GetMapping("/analytics/summary")
    public IncidentDtos.IncidentAnalyticsResponse analytics() {
        return incidentService.analytics();
    }
}
