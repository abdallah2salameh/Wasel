package com.wasel.backend.mobility.report;

import com.wasel.backend.common.BadRequestException;
import com.wasel.backend.common.PageResponse;
import com.wasel.backend.common.ResourceNotFoundException;
import com.wasel.backend.mobility.GeoUtils;
import com.wasel.backend.mobility.incident.Incident;
import com.wasel.backend.mobility.incident.IncidentRepository;
import com.wasel.backend.mobility.incident.IncidentSeverity;
import com.wasel.backend.mobility.incident.IncidentSourceType;
import com.wasel.backend.mobility.incident.IncidentStatus;
import com.wasel.backend.security.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private final CrowdReportRepository crowdReportRepository;
    private final ReportVoteRepository reportVoteRepository;
    private final ModerationAuditRepository moderationAuditRepository;
    private final IncidentRepository incidentRepository;

    public ReportService(
            CrowdReportRepository crowdReportRepository,
            ReportVoteRepository reportVoteRepository,
            ModerationAuditRepository moderationAuditRepository,
            IncidentRepository incidentRepository
    ) {
        this.crowdReportRepository = crowdReportRepository;
        this.reportVoteRepository = reportVoteRepository;
        this.moderationAuditRepository = moderationAuditRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public ReportDtos.ReportResponse submit(
            ReportDtos.CreateReportRequest request,
            UserAccount user,
            String clientFingerprint
    ) {
        enforceSubmissionGuard(user, clientFingerprint);
        validateDescription(request.description());
        CrowdReport report = new CrowdReport();
        report.setLatitude(request.latitude());
        report.setLongitude(request.longitude());
        report.setCategory(request.category());
        report.setDescription(request.description());
        report.setSubmittedBy(user);
        report.setClientFingerprint(clientFingerprint);
        report.setStatus(ReportStatus.PENDING);
        report.setConfidenceScore(0.5);

        detectDuplicate(report);
        crowdReportRepository.save(report);
        return toResponse(report);
    }

    public PageResponse<ReportDtos.ReportResponse> list(ReportStatus status, int page, int size) {
        Page<CrowdReport> reports = status == null
                ? crowdReportRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                : crowdReportRepository.findByStatus(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")), status);
        return PageResponse.from(reports.map(this::toResponse));
    }

    public ReportDtos.ReportDetailsResponse details(UUID id) {
        CrowdReport report = getReport(id);
        List<ReportDtos.ModerationAuditResponse> audits = moderationAuditRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(ModerationEntityType.REPORT, id)
                .stream()
                .map(audit -> new ReportDtos.ModerationAuditResponse(
                        audit.getId(),
                        audit.getAction(),
                        audit.getReason(),
                        audit.getModerator() != null ? audit.getModerator().getEmail() : "system",
                        audit.getCreatedAt()
                ))
                .toList();
        return new ReportDtos.ReportDetailsResponse(toResponse(report), audits);
    }

    @Transactional
    public ReportDtos.ReportResponse vote(UUID reportId, ReportDtos.VoteRequest request, UserAccount user) {
        CrowdReport report = getReport(reportId);
        ReportVote vote = reportVoteRepository.findByReportIdAndUser(reportId, user).orElseGet(ReportVote::new);
        vote.setReport(report);
        vote.setUser(user);
        vote.setVoteType(request.voteType());
        reportVoteRepository.save(vote);
        recalculateConfidence(report);
        return toResponse(report);
    }

    @Transactional
    public ReportDtos.ReportResponse moderate(UUID reportId, ReportDtos.ModerateReportRequest request, UserAccount moderator) {
        CrowdReport report = getReport(reportId);
        String action = request.action().trim().toUpperCase();
        switch (action) {
            case "APPROVE" -> {
                report.setStatus(ReportStatus.APPROVED);
                createIncidentFromReport(report, moderator);
            }
            case "REJECT" -> report.setStatus(ReportStatus.REJECTED);
            case "DUPLICATE" -> report.setStatus(ReportStatus.DUPLICATE);
            default -> throw new BadRequestException("Unsupported moderation action");
        }
        report.setReviewedAt(Instant.now());
        report.setReviewedBy(moderator);
        saveAudit(reportId, action, request.reason(), moderator);
        return toResponse(report);
    }

    private void enforceSubmissionGuard(UserAccount user, String clientFingerprint) {
        Instant windowStart = Instant.now().minus(10, ChronoUnit.MINUTES);
        long recentSubmissions = user != null
                ? crowdReportRepository.countBySubmittedByAndCreatedAtAfter(user, windowStart)
                : crowdReportRepository.countByClientFingerprintAndCreatedAtAfter(clientFingerprint, windowStart);
        if (recentSubmissions >= 5) {
            throw new BadRequestException("Submission rate exceeded. Please wait before sending more reports.");
        }
    }

    private void detectDuplicate(CrowdReport candidate) {
        Instant cutoff = Instant.now().minus(2, ChronoUnit.HOURS);
        List<CrowdReport> nearbyReports = crowdReportRepository.findByCategoryAndCreatedAtAfterAndLatitudeBetweenAndLongitudeBetween(
                candidate.getCategory(),
                cutoff,
                candidate.getLatitude() - 0.02,
                candidate.getLatitude() + 0.02,
                candidate.getLongitude() - 0.02,
                candidate.getLongitude() + 0.02
        );

        nearbyReports.stream()
                .filter(existing -> GeoUtils.haversineKm(
                        candidate.getLatitude(),
                        candidate.getLongitude(),
                        existing.getLatitude(),
                        existing.getLongitude()) <= 1.5)
                .findFirst()
                .ifPresent(existing -> {
                    candidate.setStatus(ReportStatus.DUPLICATE);
                    candidate.setDuplicateOfReportId(existing.getId());
                    candidate.setConfidenceScore(0.2);
                });
    }

    private void createIncidentFromReport(CrowdReport report, UserAccount moderator) {
        Incident incident = new Incident();
        incident.setTitle("Crowdsourced " + report.getCategory().name().replace('_', ' ').toLowerCase());
        incident.setDescription(report.getDescription());
        incident.setCategory(report.getCategory());
        incident.setSeverity(IncidentSeverity.MEDIUM);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setSourceType(IncidentSourceType.CROWDSOURCED);
        incident.setLatitude(report.getLatitude());
        incident.setLongitude(report.getLongitude());
        incident.setVerified(false);
        incident.setCreatedBy(moderator);
        incidentRepository.save(incident);
    }

    private void saveAudit(UUID entityId, String action, String reason, UserAccount moderator) {
        ModerationAudit audit = new ModerationAudit();
        audit.setEntityType(ModerationEntityType.REPORT);
        audit.setEntityId(entityId);
        audit.setAction(action);
        audit.setReason(reason);
        audit.setModerator(moderator);
        moderationAuditRepository.save(audit);
    }

    private void recalculateConfidence(CrowdReport report) {
        List<ReportVote> votes = reportVoteRepository.findByReportId(report.getId());
        long confirms = votes.stream().filter(vote -> vote.getVoteType() == ReportVoteType.CONFIRM).count();
        long denies = votes.stream().filter(vote -> vote.getVoteType() == ReportVoteType.DENY).count();
        double score = 0.5 + ((confirms - denies) * 0.1);
        report.setConfidenceScore(Math.max(0.0, Math.min(1.0, score)));
    }

    private CrowdReport getReport(UUID reportId) {
        return crowdReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    private ReportDtos.ReportResponse toResponse(CrowdReport report) {
        return new ReportDtos.ReportResponse(
                report.getId(),
                report.getLatitude(),
                report.getLongitude(),
                report.getCategory(),
                report.getDescription(),
                report.getStatus(),
                report.getConfidenceScore(),
                report.getAbuseFlagCount(),
                report.getCreatedAt(),
                report.getReviewedAt(),
                report.getDuplicateOfReportId(),
                report.getSubmittedBy() != null ? report.getSubmittedBy().getEmail() : "anonymous"
        );
    }

    private void validateDescription(String description) {
        // Check for suspicious links
        if (description.contains("http://") || description.contains("https://")) {
            throw new BadRequestException("Suspicious links detected in description");
        }
        // Check for repeated words (simple spam detection)
        String[] words = description.toLowerCase().split("\\s+");
        for (String word : words) {
            long count = java.util.Arrays.stream(words).filter(w -> w.equals(word)).count();
            if (count > 5) { // If a word repeats more than 5 times, consider it spam
                throw new BadRequestException("Repeated words detected, possible spam");
            }
        }
    }
}
