package com.wasel.backend.mobility.checkpoint;

import com.wasel.backend.security.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "checkpoint_status_history")
public class CheckpointStatusHistory {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checkpoint_id", nullable = false)
    private Checkpoint checkpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckpointStatus status;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private UserAccount changedBy;

    @Column(nullable = false)
    private Instant recordedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }
}
