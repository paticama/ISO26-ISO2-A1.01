# UC-AAD-06 — Generate Operational Reports

## Phase Elaboration

- **Module:** AAD (Admissions, Appointments and Diagnostics)
- **Minutes:** Neutral
- **GitHub Issue:** Neutral
- **Primary actor:** Admissions Staff
- **Supporting actor:** System Administrator
- **Related decisions:** [CU00 initial structure decisions](../../feature/CU00-initial-structure/DECISIONS.md)

## Goal and scope

Admissions Staff generates operational reports for workload, attendance, waiting time, cancellations, capacity, and booking outcomes. System Administrator configures report definitions, access, retention, and export limits; the administrator does not receive operational data automatically through this use case.

## Client–Server flow

1. The client presents report types and permitted filters.
2. Admissions Staff selects a report, date range, scope, and output format.
3. The client sends a typed report request.
4. The server authorizes the scope, validates filters, applies the report definition, and queries the reporting source.
5. The server returns a bounded result or creates a tracked export job.
6. The client displays status, results, or an explicit error and allows a permitted download.

## Main flow

1. Admissions Staff selects a configured operational report.
2. The client requires a valid date range and shows the permitted locations and services.
3. The server validates range limits, access scope, and report parameters.
4. The server calculates metrics from consistent operational snapshots and includes the period and scope in the result.
5. Small reports return immediately; larger exports are queued and expose progress.
6. The report result includes generation time, source period, filters, and a correlation identifier.

## Alternate and error flows

- Invalid or excessive date range returns `REPORT_RANGE_INVALID`.
- A report type or filter outside actor scope returns `FORBIDDEN`.
- No data returns a successful empty report with explicit zero-row metadata, not a fabricated fallback.
- A queued export that fails returns `REPORT_GENERATION_FAILED` with a retry-safe job state.
- Unsupported format returns `REPORT_FORMAT_UNSUPPORTED`.
- Reporting source unavailability returns `SERVICE_UNAVAILABLE`.

## RADIT

### Requirements

- Generate configured operational reports with bounded filters.
- Enforce actor scope and protect sensitive data.
- Support immediate results and tracked asynchronous exports.
- Include reproducibility metadata and audit report access.

### Analysis

Reports must be reproducible for a defined period and scope. Operational queries should use a reporting read model or snapshot so long-running reports do not block transactional AAD operations. Empty data is valid and must be represented explicitly.

### Design

The client uses `ReportCatalogQuery`, `GenerateReportCommand`, `ReportResultQuery`, and `ExportJobQuery`. The server owns report definitions, authorization, aggregation, export jobs, retention, and access audit.

### Implementation

Implement parameter validation, scoped aggregation, snapshot metadata, bounded pagination, asynchronous export jobs, result expiration, and audit events. Keep report formats behind a typed formatter interface.

### Testing

Test metric correctness, date boundaries, empty results, authorization, sensitive-field exclusion, large exports, retry behavior, expiration, format validation, and source outage handling.

### Deployment

Deploy reporting views or snapshot jobs, indexes, export workers, retention policies, and monitoring. Reconcile a sample of report totals with transactional source data before enabling production exports.

## Contracts and interfaces

### Client contract

- `listReportDefinitions() -> ReportDefinition[]`
- `generateReport(command) -> ReportResult | ExportJob`
- `getReportResult(id) -> ReportResult`
- `getExportJob(id) -> ExportJobStatus`

### Server contract

- Enforce report scope, filter limits, retention, and sensitive-field policy.
- Return reproducibility metadata and stable error codes.
- Never claim completion for a failed or incomplete export.

### Interfaces

Versioned HTTP/JSON routes live under `/api/aad/reports`. Immediate results use a typed report envelope; asynchronous exports use job status and a time-limited download interface. All access is correlated and auditable.

## Acceptance criteria

- Admissions Staff can generate each permitted operational report for a valid scope.
- Invalid ranges, unsupported formats, and unauthorized scopes return explicit stable errors.
- Empty data produces a valid report with zero-row metadata.
- Large exports expose progress and a definitive success or failure state.
- Report results include period, scope, filters, generation time, and correlation identifier.
- Sensitive fields are excluded according to actor permissions.
- System Administrator report configuration and access policy remain separate from operational report generation.

## Effort estimation

| Work item | Estimate |
|---|---:|
| Requirements and metric analysis | 5 h |
| Reporting and snapshot design | 7 h |
| Client report and export views | 9 h |
| Server, aggregation, and worker implementation | 14 h |
| Automated and reconciliation testing | 12 h |
| Deployment and monitoring verification | 5 h |
| **Total** | **52 h** |

