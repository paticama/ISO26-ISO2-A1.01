package com.sescam.radiodiagnostics.diagnostics.repository;

import com.sescam.radiodiagnostics.diagnostics.domain.DiagnosticReport;

import java.util.Optional;

public interface ReportRepository {

    DiagnosticReport save(DiagnosticReport report);

    Optional<DiagnosticReport> findById(String reportId);

    Optional<DiagnosticReport> findByExecutionId(String executionId);

    long count();
}
