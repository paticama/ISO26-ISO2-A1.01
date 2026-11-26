package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * Outbound port to C9 (sescam-integration) / Hospital EHR for exporting HL7 FHIR DiagnosticReport resources.
 */
public interface FhirDiagnosticReportGateway {

    EhrReportSyncStatus exportDiagnosticReport(ReportDTO report);
}
