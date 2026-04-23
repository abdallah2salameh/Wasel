package com.wasel.backend.mobility.checkpoint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CheckpointStatusHistoryRepository extends JpaRepository<CheckpointStatusHistory, UUID> {
    List<CheckpointStatusHistory> findByCheckpointIdOrderByRecordedAtDesc(UUID checkpointId);
}
