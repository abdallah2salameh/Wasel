package com.wasel.backend.mobility.checkpoint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CheckpointRepository extends JpaRepository<Checkpoint, UUID>, JpaSpecificationExecutor<Checkpoint> {
}
