# UC-SYS-05 — Generate Statistics & Usage Analytics

## Use Case

- **UC:** `UC-SYS-05`
- **Requirement:** `FR-ADM-22`
- **Phase:** Inception
- **Module:** Administrators (`v0.5.0`)
- **Minutes:** [Minutes of meetings](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings) (details TBD)
- **GitHub Issue:** [#65](https://github.com/paticama/ISO26-ISO2-A1.01/issues/65)
- **Primary actor:** System Administrator
- **System:** Administrator Server
- **Goal:** Generate global statistics and system-usage reports.

## Goal and scope

The System Administrator requests aggregated statistics and usage analytics to understand system activity over a selected period. The use case covers selection of permitted metrics, date range and filters, aggregation, and presentation of results. It does not authorize access to individual clinical records or replace operational throughput reporting and compliance-audit inspection.

## Preconditions

1. The administrator is authenticated and authorized to view global analytics.
2. Aggregated data for the requested period is available to the reporting service.

## Postconditions

- A bounded, scoped report is displayed with its period and generation metadata.
- No individual-level clinical information is disclosed through the aggregate report.
- Report access is recorded when required by policy.

## Client–Server flow

1. The client requests the analytics catalog and available date/filter options.
2. The server returns permitted metrics and their applicable limits.
3. The administrator selects metrics, a date range, and any supported filters, then submits a report request.
4. The server authorizes the request, validates its bounds, and queries an aggregate reporting source.
5. The server returns a completed report or a tracked asynchronous job for a larger query.
6. The client displays results or job status, including period, applied filters, generation time, and a correlation reference.

## Main flow

1. The administrator opens system analytics.
2. The client presents available global statistics and usage metrics.
3. The administrator selects the report and a valid time period.
4. The server validates the request and retrieves data from an authorized aggregate source.
5. The server computes totals and trends, applies any configured minimum-group or privacy rules, and records the report request.
6. The server returns the result or creates an export job when the result cannot be generated synchronously.
7. The administrator reviews the report and may refresh or adjust the query within allowed bounds.

## Alternate and error flows

- Invalid or excessive date range: return `ANALYTICS_RANGE_INVALID`.
- Unsupported metric or filter combination: return `ANALYTICS_QUERY_INVALID`.
- Requested data falls outside the administrator's scope: return `FORBIDDEN`.
- No data exists for the period: return a successful empty or zero-valued aggregate with explicit period metadata.
- A group is too small to satisfy configured privacy rules: suppress or combine it and indicate suppression without exposing underlying records.
- A large report is queued: return a job identifier and allow the client to retrieve progress and final status.
- Reporting source unavailable or job fails: return `SERVICE_UNAVAILABLE` or `ANALYTICS_GENERATION_FAILED`; do not present partial data as complete.

## RADIT

### Requirements

- Generate global statistics and usage reports for permitted metrics and periods.
- Validate date ranges, filters, and query bounds.
- Return reproducibility metadata and explicit empty, pending, success, and failure states.
- Use aggregated data and apply configured privacy protections.

### Analysis

Analytics should be computed from a reporting read model or consistent snapshot so queries do not disrupt transactional operations. A report's period, filters, generation time, and source snapshot should be available to support interpretation. The aggregate interface must not provide a path to retrieve individual clinical records.

### Design

Use `AnalyticsCatalogQuery`, `GenerateAnalyticsCommand`, `AnalyticsReport`, and `AnalyticsJobStatus`. The server owns metric definitions, scope validation, aggregation, privacy thresholds, and job lifecycle. Report results include metadata sufficient to explain the query.

### Implementation

Implement bounded metric queries, snapshot or read-model aggregation, privacy suppression where configured, access audit, pagination or asynchronous jobs, and stable error responses. Keep metric definitions centralized to ensure consistent calculations.

### Testing

Test metric correctness, time boundaries, filter validation, empty periods, privacy suppression, authorization, sensitive-data exclusion, asynchronous completion/failure, and consistency against representative source data.

### Deployment

Deploy reporting views or aggregation jobs, indexes, job workers if needed, retention settings, and monitoring. Reconcile sample aggregates against source data and verify privacy controls before enabling global analytics.

## Contracts and interfaces

### Client contract

- `listAnalyticsMetrics() -> AnalyticsMetric[]`
- `generateAnalytics(command) -> AnalyticsReport | AnalyticsJob`
- `getAnalyticsJob(jobId) -> AnalyticsJobStatus`

### Server contract

- Enforce authorization, query bounds, aggregation, and privacy rules server-side.
- Return report period, filters, generation time, and a correlation identifier.
- Distinguish zero-result reports from incomplete or failed generation.
- Never expose individual clinical records through this aggregate use case.

## Acceptance criteria

- A System Administrator can generate global statistics and usage reports for valid, permitted periods and metrics.
- Invalid, excessive, or unauthorized requests receive explicit stable errors.
- Empty periods are represented as valid empty/zero-valued results with metadata.
- Results include period, applied filters, and generation metadata.
- Privacy rules prevent disclosure of individual-level or sufficiently small-group data.
- Long-running reports expose a definitive job status and do not present partial results as complete.

## Decisions (DECISIONS.md)

- No specific decision is identified in issue [#65](https://github.com/paticama/ISO26-ISO2-A1.01/issues/65); record applicable decisions here when established.
