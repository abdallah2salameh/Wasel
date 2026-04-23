package com.wasel.backend.mobility.report;

import com.wasel.backend.mobility.incident.IncidentCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReportDtos {

    public record CreateReportRequest(
            @DecimalMin("29.5") @DecimalMax("33.5") double latitude,
            @DecimalMin("34.0") @DecimalMax("35.5") double longitude,
            @NotNull IncidentCategory category,
            @Size(min = 10, max = 1000) String description
    ) {
    }

    public record ModerateReportRequest(
            @NotBlank String action,
            String reason
    ) {
    }

    public record VoteRequest(
            @NotNull ReportVoteType voteType
    ) {
    }

    public record ReportResponse(
            UUID id,
            double latitude,
            double longitude,
            IncidentCategory category,
            String description,
            ReportStatus status,
            double confidenceScore,
            int abuseFlagCount,
            Instant createdAt,
            Instant reviewedAt,
            UUID duplicateOfReportId,
            String submittedBy
    ) {
    }

    public record ModerationAuditResponse(
            UUID id,
            String action,
            String reason,
            String moderator,
            Instant createdAt
    ) {
    }

    public record ReportDetailsResponse(
            ReportResponse report,
            List<ModerationAuditResponse> audits
    ) {
    }
}
