package com.wasel.backend.mobility.report;

import com.wasel.backend.mobility.incident.IncidentCategory;
import com.wasel.backend.security.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrowdReportRepository extends JpaRepository<CrowdReport, UUID> {
    List<CrowdReport> findByCategoryAndCreatedAtAfterAndLatitudeBetweenAndLongitudeBetween(
            IncidentCategory category,
            Instant createdAt,
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude
    );

    long countBySubmittedByAndCreatedAtAfter(UserAccount submittedBy, Instant instant);

    long countByClientFingerprintAndCreatedAtAfter(String clientFingerprint, Instant instant);

    Page<CrowdReport> findByStatus(Pageable pageable, ReportStatus status);

    Page<CrowdReport> findAll(Pageable pageable);
}
