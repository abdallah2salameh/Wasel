package com.wasel.backend.mobility.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModerationAuditRepository extends JpaRepository<ModerationAudit, UUID> {
    List<ModerationAudit> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(ModerationEntityType entityType, UUID entityId);
}
