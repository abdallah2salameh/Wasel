package com.wasel.backend.mobility.report;

import com.wasel.backend.security.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportVoteRepository extends JpaRepository<ReportVote, UUID> {
    Optional<ReportVote> findByReportIdAndUser(UUID reportId, UserAccount user);

    List<ReportVote> findByReportId(UUID reportId);
}
