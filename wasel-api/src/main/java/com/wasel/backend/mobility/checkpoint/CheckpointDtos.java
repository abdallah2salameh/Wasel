package com.wasel.backend.mobility.checkpoint;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CheckpointDtos {

    public record CreateCheckpointRequest(
            @NotBlank String name,
            @NotBlank String governorate,
            @Min(-90) @Max(90) double latitude,
            @Min(-180) @Max(180) double longitude,
            String notes,
            @NotNull CheckpointStatus currentStatus
    ) {
    }

    public record UpdateCheckpointStatusRequest(
            @NotNull CheckpointStatus status,
            String notes
    ) {
    }

    public record CheckpointResponse(
            UUID id,
            String name,
            String governorate,
            double latitude,
            double longitude,
            CheckpointStatus currentStatus,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CheckpointHistoryResponse(
            UUID id,
            CheckpointStatus status,
            String notes,
            String changedBy,
            Instant recordedAt
    ) {
    }

    public record CheckpointDetailsResponse(
            CheckpointResponse checkpoint,
            List<CheckpointHistoryResponse> history
    ) {
    }
}
