package com.wasel.backend.mobility.incident;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {
    List<Incident> findByStatusAndVerifiedAndLatitudeBetweenAndLongitudeBetween(
            IncidentStatus status,
            boolean verified,
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude
    );

    long countByCreatedAtAfter(Instant instant);
}
