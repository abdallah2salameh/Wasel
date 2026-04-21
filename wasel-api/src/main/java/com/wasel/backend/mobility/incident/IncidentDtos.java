package com.wasel.backend.mobility.incident;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class IncidentDtos {

    public record CreateIncidentRequest(
            @NotBlank String title,
            @Size(min = 10, max = 1500) String description,
            @NotNull IncidentCategory category,
            @NotNull IncidentSeverity severity,
            @Min(-90) @Max(90) double latitude,
            @Min(-180) @Max(180) double longitude,
            UUID checkpointId
    ) {
    }

    public record UpdateIncidentRequest(
            String title,
            String description,
            IncidentSeverity severity,
            IncidentStatus status
    ) {
    }

    public record IncidentResponse(
            UUID id,
            String title,
            String description,
            IncidentCategory category,
            IncidentSeverity severity,
            IncidentStatus status,
            IncidentSourceType sourceType,
            double latitude,
            double longitude,
            boolean verified,
            Instant reportedAt,
            Instant verifiedAt,
            Instant closedAt,
            UUID checkpointId,
            String createdBy
    ) {
    }

    public record IncidentAnalyticsResponse(List<IncidentAnalyticsRow> rows) {
    }

    public record IncidentAnalyticsRow(String category, String severity, long total) {
    }
}
