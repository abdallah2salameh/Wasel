package com.wasel.backend.mobility.alert;

import com.wasel.backend.mobility.incident.IncidentCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public class AlertDtos {

    public record CreateSubscriptionRequest(
            @NotBlank String areaName,
            @Min(-90) @Max(90) double minLatitude,
            @Min(-90) @Max(90) double maxLatitude,
            @Min(-180) @Max(180) double minLongitude,
            @Min(-180) @Max(180) double maxLongitude,
            IncidentCategory incidentCategory
    ) {
    }

    public record SubscriptionResponse(
            UUID id,
            String areaName,
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude,
            IncidentCategory incidentCategory,
            boolean active,
            Instant createdAt
    ) {
    }

    public record AlertRecordResponse(
            UUID id,
            UUID incidentId,
            String incidentTitle,
            AlertDeliveryStatus deliveryStatus,
            Instant createdAt
    ) {
    }
}
