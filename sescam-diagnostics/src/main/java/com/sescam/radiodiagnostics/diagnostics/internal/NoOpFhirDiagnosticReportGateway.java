package com.sescam.radiodiagnostics.diagnostics.internal;

import com.sescam.radiodiagnostics.common.diagnostics.EhrReportSyncStatus;
import com.sescam.radiodiagnostics.common.diagnostics.FhirDiagnosticReportGateway;
import com.sescam.radiodiagnostics.common.diagnostics.ReportDTO;

/**
 * TEMPORARY stand-in for C9 (sescam-integration) HL7 FHIR DiagnosticReport exporter.
 */
public class NoOpFhirDiagnosticReportGateway implements FhirDiagnosticReportGateway {

    @Override
    public EhrReportSyncStatus exportDiagnosticReport(ReportDTO report) {
        return EhrReportSyncStatus.SYNCED;
    }
}
