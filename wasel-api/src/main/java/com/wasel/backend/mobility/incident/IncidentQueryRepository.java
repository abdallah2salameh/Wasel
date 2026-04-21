package com.wasel.backend.mobility.incident;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class IncidentQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public IncidentQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<IncidentSummaryRow> summaryByCategoryAndSeverity() {
        return jdbcTemplate.query("""
                        SELECT category, severity, COUNT(*) AS total
                        FROM incidents
                        GROUP BY category, severity
                        ORDER BY total DESC, category ASC
                        """,
                (rs, rowNum) -> new IncidentSummaryRow(
                        rs.getString("category"),
                        rs.getString("severity"),
                        rs.getLong("total")
                ));
    }

    public record IncidentSummaryRow(String category, String severity, long total) {
    }
}
