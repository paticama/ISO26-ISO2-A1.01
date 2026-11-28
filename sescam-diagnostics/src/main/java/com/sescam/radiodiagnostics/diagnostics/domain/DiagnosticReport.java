package com.sescam.radiodiagnostics.diagnostics.domain;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.diagnostics.EhrReportSyncStatus;
import com.sescam.radiodiagnostics.common.diagnostics.ReportDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ReportStateConflictException;
import com.sescam.radiodiagnostics.common.diagnostics.ReportStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate entity of C7 representing a diagnostic report authored for an executed procedure (UC-PHY-04).
 */
public class DiagnosticReport {

    private final String id;
    private final String executionId;
    private final String orderId;
    private final String patientId;
    private final String studyInstanceUid;
    private final String examTypeName;
    private final ModalityType modality;
    private String findings;
    private String conclusion;
    private boolean criticalAlert;
    private boolean followUpRecommended;
    private ReportStatus status;
    private String authorPhysicianId;
    private String digitalSignature;
    private Instant signedAt;
    private EhrReportSyncStatus ehrSyncStatus;
    private Instant ehrSyncedAt;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<ReportAddendum> addendums;

    public DiagnosticReport(String id, String executionId, String orderId, String patientId,
                            String studyInstanceUid, String examTypeName, ModalityType modality,
                            String findings, String conclusion, boolean criticalAlert, boolean followUpRecommended,
                            ReportStatus status, String authorPhysicianId, String digitalSignature, Instant signedAt,
                            EhrReportSyncStatus ehrSyncStatus, Instant ehrSyncedAt, long version,
                            Instant createdAt, Instant updatedAt, List<ReportAddendum> addendums) {
        this.id = Objects.requireNonNull(id, "id");
        this.executionId = Objects.requireNonNull(executionId, "executionId");
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.patientId = Objects.requireNonNull(patientId, "patientId");
        this.studyInstanceUid = studyInstanceUid;
        this.examTypeName = examTypeName;
        this.modality = modality;
        this.findings = findings == null ? "" : findings;
        this.conclusion = conclusion == null ? "" : conclusion;
        this.criticalAlert = criticalAlert;
        this.followUpRecommended = followUpRecommended;
        this.status = Objects.requireNonNull(status, "status");
        this.authorPhysicianId = Objects.requireNonNull(authorPhysicianId, "authorPhysicianId");
        this.digitalSignature = digitalSignature;
        this.signedAt = signedAt;
        this.ehrSyncStatus = Objects.requireNonNull(ehrSyncStatus, "ehrSyncStatus");
        this.ehrSyncedAt = ehrSyncedAt;
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.addendums = addendums == null ? new ArrayList<>() : new ArrayList<>(addendums);
    }

    public static DiagnosticReport createDraft(String id, ProcedureExecution execution, String studyInstanceUid,
                                               String findings, String conclusion, boolean criticalAlert,
                                               boolean followUpRecommended, String authorPhysicianId, Instant now) {
        Objects.requireNonNull(execution, "execution");
        Objects.requireNonNull(now, "now");
        return new DiagnosticReport(id, execution.getId(), execution.getOrderId(), execution.getPatientId(),
                studyInstanceUid, execution.getExamTypeName(), execution.getModality(),
                normalizeText(findings), normalizeText(conclusion), criticalAlert, followUpRecommended,
                ReportStatus.DRAFT, authorPhysicianId, null, null, EhrReportSyncStatus.NOT_SYNCED, null,
                1L, now, now, List.of());
    }

    public void updateDraft(String findings, String conclusion, boolean criticalAlert,
                            boolean followUpRecommended, String authorPhysicianId, Instant now) {
        if (status != ReportStatus.DRAFT) {
            throw new ReportStateConflictException(
                    "Report '" + id + "' is already " + status + "; issue an addendum instead.");
        }
        this.findings = normalizeText(findings);
        this.conclusion = normalizeText(conclusion);
        this.criticalAlert = criticalAlert;
        this.followUpRecommended = followUpRecommended;
        this.authorPhysicianId = Objects.requireNonNull(authorPhysicianId, "authorPhysicianId");
        this.updatedAt = Objects.requireNonNull(now, "now");
        this.version++;
    }

    public void finalizeAndSign(String findings, String conclusion, boolean criticalAlert,
                                boolean followUpRecommended, String authorPhysicianId,
                                String digitalSignature, Instant now) {
        if (status != ReportStatus.DRAFT) {
            throw new ReportStateConflictException("Report '" + id + "' is already " + status + ".");
        }
        this.findings = normalizeText(findings);
        this.conclusion = normalizeText(conclusion);
        this.criticalAlert = criticalAlert;
        this.followUpRecommended = followUpRecommended;
        this.authorPhysicianId = Objects.requireNonNull(authorPhysicianId, "authorPhysicianId");
        this.digitalSignature = Objects.requireNonNull(digitalSignature, "digitalSignature");
        this.signedAt = Objects.requireNonNull(now, "now");
        this.status = ReportStatus.FINALIZED;
        this.updatedAt = now;
        this.version++;
    }

    public void addAddendum(ReportAddendum addendum, Instant now) {
        Objects.requireNonNull(addendum, "addendum");
        if (status == ReportStatus.DRAFT) {
            throw new ReportStateConflictException(
                    "Cannot issue an addendum to draft report '" + id + "'; finalize it first.");
        }
        this.addendums.add(addendum);
        this.status = ReportStatus.AMENDED;
        this.updatedAt = Objects.requireNonNull(now, "now");
        this.version++;
    }

    public void markEhrSync(EhrReportSyncStatus syncStatus, Instant syncedAt) {
        this.ehrSyncStatus = Objects.requireNonNull(syncStatus, "syncStatus");
        this.ehrSyncedAt = syncedAt;
    }

    public ReportDTO toDTO() {
        return new ReportDTO(id, executionId, orderId, patientId, studyInstanceUid, examTypeName, modality,
                findings, conclusion, criticalAlert, followUpRecommended, status, authorPhysicianId,
                digitalSignature, signedAt, ehrSyncStatus, ehrSyncedAt, version, createdAt, updatedAt,
                addendums.stream().map(ReportAddendum::toDTO).toList());
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    public String getId() {
        return id;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getPatientId() {
        return patientId;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public boolean isCriticalAlert() {
        return criticalAlert;
    }

    public EhrReportSyncStatus getEhrSyncStatus() {
        return ehrSyncStatus;
    }

    public List<ReportAddendum> getAddendums() {
        return List.copyOf(addendums);
    }
}
