package com.sescam.radiodiagnostics.diagnostics.repository;

import com.sescam.radiodiagnostics.common.diagnostics.ReportStateConflictException;
import com.sescam.radiodiagnostics.diagnostics.domain.DiagnosticReport;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store ensuring at most one diagnostic report per procedure execution.
 */
public class InMemoryReportRepository implements ReportRepository {

    private final Map<String, DiagnosticReport> byId = new ConcurrentHashMap<>();
    private final Map<String, String> reportIdByExecutionId = new ConcurrentHashMap<>();

    @Override
    public DiagnosticReport save(DiagnosticReport report) {
        String existingReportId = reportIdByExecutionId.putIfAbsent(report.getExecutionId(), report.getId());
        if (existingReportId != null && !existingReportId.equals(report.getId())) {
            throw new ReportStateConflictException(
                    "A diagnostic report already exists for execution '" + report.getExecutionId() + "'.");
        }
        byId.put(report.getId(), report);
        return report;
    }

    @Override
    public Optional<DiagnosticReport> findById(String reportId) {
        return reportId == null ? Optional.empty() : Optional.ofNullable(byId.get(reportId));
    }

    @Override
    public Optional<DiagnosticReport> findByExecutionId(String executionId) {
        if (executionId == null) {
            return Optional.empty();
        }
        String reportId = reportIdByExecutionId.get(executionId);
        return reportId == null ? Optional.empty() : Optional.ofNullable(byId.get(reportId));
    }

    @Override
    public long count() {
        return byId.size();
    }
}
