package com.wasel.backend.mobility.incident;

import com.wasel.backend.common.PageResponse;
import com.wasel.backend.common.ResourceNotFoundException;
import com.wasel.backend.mobility.alert.AlertService;
import com.wasel.backend.mobility.checkpoint.Checkpoint;
import com.wasel.backend.mobility.checkpoint.CheckpointRepository;
import com.wasel.backend.mobility.report.ModerationAudit;
import com.wasel.backend.mobility.report.ModerationAuditRepository;
import com.wasel.backend.mobility.report.ModerationEntityType;
import com.wasel.backend.security.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentQueryRepository incidentQueryRepository;
    private final CheckpointRepository checkpointRepository;
    private final ModerationAuditRepository moderationAuditRepository;
    private final AlertService alertService;

    public IncidentService(
            IncidentRepository incidentRepository,
            IncidentQueryRepository incidentQueryRepository,
            CheckpointRepository checkpointRepository,
            ModerationAuditRepository moderationAuditRepository,
            AlertService alertService
    ) {
        this.incidentRepository = incidentRepository;
        this.incidentQueryRepository = incidentQueryRepository;
        this.checkpointRepository = checkpointRepository;
        this.moderationAuditRepository = moderationAuditRepository;
        this.alertService = alertService;
    }

    @Transactional
    public IncidentDtos.IncidentResponse create(IncidentDtos.CreateIncidentRequest request, UserAccount actor) {
        Incident incident = new Incident();
        incident.setTitle(request.title());
        incident.setDescription(request.description());
        incident.setCategory(request.category());
        incident.setSeverity(request.severity());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setSourceType(IncidentSourceType.OFFICIAL);
        incident.setLatitude(request.latitude());
        incident.setLongitude(request.longitude());
        incident.setVerified(false);
        incident.setCreatedBy(actor);
        if (request.checkpointId() != null) {
            Checkpoint checkpoint = checkpointRepository.findById(request.checkpointId())
                    .orElseThrow(() -> new ResourceNotFoundException("Checkpoint not found"));
            incident.setCheckpoint(checkpoint);
        }
        incidentRepository.save(incident);
        return toResponse(incident);
    }

    public PageResponse<IncidentDtos.IncidentResponse> list(
            IncidentCategory category,
            IncidentSeverity severity,
            IncidentStatus status,
            Boolean verified,
            int page,
            int size,
            String sortBy
    ) {
        Specification<Incident> specification = Specification.where(null);
        if (category != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (severity != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
        }
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (verified != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("verified"), verified));
        }

        Page<IncidentDtos.IncidentResponse> incidents = incidentRepository.findAll(
                        specification,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, normalizeSort(sortBy, "reportedAt"))))
                .map(this::toResponse);
        return PageResponse.from(incidents);
    }

    public IncidentDtos.IncidentResponse details(UUID id) {
        return toResponse(getIncident(id));
    }

    @Transactional
    public IncidentDtos.IncidentResponse update(UUID id, IncidentDtos.UpdateIncidentRequest request) {
        Incident incident = getIncident(id);
        if (request.title() != null) {
            incident.setTitle(request.title());
        }
        if (request.description() != null) {
            incident.setDescription(request.description());
        }
        if (request.severity() != null) {
            incident.setSeverity(request.severity());
        }
        if (request.status() != null) {
            incident.setStatus(request.status());
        }
        return toResponse(incident);
    }

    @Transactional
    public IncidentDtos.IncidentResponse verify(UUID id, UserAccount moderator, String reason) {
        Incident incident = getIncident(id);
        incident.setVerified(true);
        incident.setStatus(IncidentStatus.VERIFIED);
        incident.setVerifiedAt(Instant.now());
        saveAudit(incident.getId(), "VERIFY", reason, moderator);
        alertService.createAlertRecordsForIncident(incident);
        return toResponse(incident);
    }

    @Transactional
    public IncidentDtos.IncidentResponse close(UUID id, UserAccount moderator, String reason) {
        Incident incident = getIncident(id);
        incident.setStatus(IncidentStatus.CLOSED);
        incident.setClosedAt(Instant.now());
        saveAudit(incident.getId(), "CLOSE", reason, moderator);
        return toResponse(incident);
    }

    public IncidentDtos.IncidentAnalyticsResponse analytics() {
        List<IncidentDtos.IncidentAnalyticsRow> rows = incidentQueryRepository.summaryByCategoryAndSeverity()
                .stream()
                .map(row -> new IncidentDtos.IncidentAnalyticsRow(row.category(), row.severity(), row.total()))
                .toList();
        return new IncidentDtos.IncidentAnalyticsResponse(rows);
    }

    public List<Incident> activeInBoundingBox(double minLat, double maxLat, double minLon, double maxLon) {
        return incidentRepository.findByStatusAndVerifiedAndLatitudeBetweenAndLongitudeBetween(
                IncidentStatus.VERIFIED,
                true,
                minLat,
                maxLat,
                minLon,
                maxLon
        );
    }

    public IncidentDtos.IncidentResponse toResponse(Incident incident) {
        return new IncidentDtos.IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getCategory(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getSourceType(),
                incident.getLatitude(),
                incident.getLongitude(),
                incident.isVerified(),
                incident.getReportedAt(),
                incident.getVerifiedAt(),
                incident.getClosedAt(),
                incident.getCheckpoint() != null ? incident.getCheckpoint().getId() : null,
                incident.getCreatedBy() != null ? incident.getCreatedBy().getEmail() : null
        );
    }

    private Incident getIncident(UUID id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
    }

    private void saveAudit(UUID entityId, String action, String reason, UserAccount moderator) {
        ModerationAudit audit = new ModerationAudit();
        audit.setEntityType(ModerationEntityType.INCIDENT);
        audit.setEntityId(entityId);
        audit.setAction(action);
        audit.setReason(reason);
        audit.setModerator(moderator);
        moderationAuditRepository.save(audit);
    }

    private String normalizeSort(String sortBy, String fallback) {
        String candidate = sortBy == null ? fallback : sortBy;
        return switch (candidate) {
            case "reportedAt", "severity", "verifiedAt", "createdAt" -> candidate;
            default -> fallback;
        };
    }
}
