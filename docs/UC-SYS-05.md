# UC-ADM-05 — Generate Statistics and Usage Analytics

## Use Case

- **UC:** `UC-ADM-05`
- **Requirements:** `FR-ADM-22`, `FR-ADM-23`, `FR-ADM-24`, `FR-ADM-25`, `FR-ADM-26`
- **Phase:** Late Construction (Iteration 4)
- **Module:** `sescam-analytics` (`v0.3.0`) — Module ADM (Administration)
- **Minutes:** [Minutes of meetings](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings) (Meeting #4 minutes)
- **GitHub Issue:** [#65](https://github.com/paticama/ISO26-ISO2-A1.01/issues/65)
- **Primary actor:** System Administrator
- **Secondary actors:** None
- **System:** SESCAM Radiodiagnostics (`sescam-analytics`)
- **Goal:** Allow a System Administrator to generate and review aggregated global statistics and system-usage reports to understand platform activity and adoption.

## UML Relationships

- **None** (no `<<include>>` or `<<extend>>` relationships apply; optional report export and asynchronous job generation are internal flows of this use case rather than standalone shared or optional use cases).
- **Related use cases (context only):** `UC-ADM-01` manages system configuration; `UC-ADM-02` controls role-based access to global reporting features; `UC-ADM-03` covers audit and compliance review.

## Goal and scope

The System Administrator requests aggregated statistics and usage analytics to understand system activity over a selected period[cite: 2]. The use case covers selection of permitted metrics, date range and filters, aggregation, privacy protection, and presentation or optional export of results. It does not authorize access to individual clinical records or replace operational throughput reporting and compliance-audit inspection (`UC-ADM-03`).

## Preconditions

1. The System Administrator is authenticated (via Corporate SSO) and authorized to view global reporting data (`UC-ADM-02`).
2. Aggregated data for the requested period and report metrics is available to the reporting service.
3. The analytics service (`sescam-analytics`) is available.

## Postconditions

- A bounded, scoped report is displayed (or queued as an export/generation job) with its period, applied filters, data coverage, and generation metadata.
- No individual-level clinical or user information is disclosed through the aggregate report.
- Report access is recorded when required by policy.

## Client–Server flow

1. The client requests the analytics catalog and available date/filter options.
2. The server returns permitted metrics and their applicable limits.
3. The System Administrator selects metrics, a date range, and any supported filters or grouping options, then submits a report request.
4. The server authorizes the request, validates its bounds, and queries an aggregate reporting source.
5. The server returns a completed report (including data-coverage information) or a tracked asynchronous job for a larger query/export.
6. The client displays results or job status, including period, applied filters, generation time, and a correlation reference.

## Main flow

1. The System Administrator opens system analytics.
2. The client presents available global statistics and usage metrics.
3. The System Administrator selects the report, valid time period, and supported filters or grouping options.
4. The server validates the request and retrieves data from an authorized aggregate source.
5. The server computes totals and trends, applies any configured minimum-group or privacy rules, and records the report request.
6. The server returns the result (or creates an export/generation job when the result cannot be generated synchronously or export is requested).
7. The System Administrator reviews the report and may refresh, adjust the query within allowed bounds, or optionally export the results if supported.

## Alternate and error flows

- **Invalid or excessive date range:** Invalid or overly broad date range -> return `ANALYTICS_RANGE_INVALID` and explain how the request can be narrowed.
- **Unsupported query:** Unsupported metric or filter combination -> return `ANALYTICS_QUERY_INVALID`.
- **Unauthorized or unauthenticated access:** Requested data falls outside the System Administrator's scope -> return `FORBIDDEN`; unauthenticated requests -> return `UNAUTHENTICATED`.
- **Empty period:** No data exists for the selected period -> return a successful empty or zero-valued aggregate with explicit period and data-coverage metadata rather than misleading partial results.
- **Privacy threshold suppression:** A group is too small to satisfy configured privacy rules -> suppress or combine it and indicate suppression without exposing underlying records.
- **Asynchronous generation or export:** A large report or export is queued -> return a job identifier (`AnalyticsJob`) and allow the client to retrieve progress and final status.
- **Service or generation failure:** Reporting source unavailable or job fails -> return `SERVICE_UNAVAILABLE` or `ANALYTICS_GENERATION_FAILED`; do not present partial or incomplete data as complete.

## RADIT

### Requirements

- Generate global statistics and usage reports for permitted metrics and periods[cite: 2].
- Validate date ranges, filters, and query bounds.
- Return reproducibility metadata (metrics, period, filters, data coverage) and explicit empty, pending, success, and failure states.
- Use aggregated data and apply configured privacy protections.

### Analysis

Analytics should be computed from a reporting read model or consistent snapshot so queries do not disrupt transactional operations. A report's period, filters, data coverage/freshness, generation time, and source snapshot should be available to support interpretation. The aggregate interface must not provide a path to retrieve individual clinical records. The metric catalogue, definitions, aggregation rules, refresh cadence, retention, and export formats are refined during analysis.

### Design

Use `AnalyticsCatalogQuery`, `GenerateAnalyticsCommand`, `AnalyticsReport`, and `AnalyticsJobStatus`. The server owns metric definitions, scope validation, aggregation, privacy thresholds, and job lifecycle. Report results include metadata sufficient to explain the query.

### Implementation

Implement bounded metric queries, snapshot or read-model aggregation, privacy suppression where configured, access audit, pagination or asynchronous jobs, and stable error responses (`ANALYTICS_RANGE_INVALID`, `ANALYTICS_QUERY_INVALID`, `FORBIDDEN`, `UNAUTHENTICATED`, `SERVICE_UNAVAILABLE`, `ANALYTICS_GENERATION_FAILED`). Keep metric definitions centralized to ensure consistent calculations.

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
- Return report period, filters, data coverage/freshness, generation time, and a correlation identifier.
- Distinguish zero-result reports from incomplete or failed generation using stable error codes (`ANALYTICS_RANGE_INVALID`, `ANALYTICS_QUERY_INVALID`, `FORBIDDEN`, `UNAUTHENTICATED`, `SERVICE_UNAVAILABLE`, `ANALYTICS_GENERATION_FAILED`).
- Never expose individual clinical records through this aggregate use case.

## Acceptance criteria

- A System Administrator can generate global statistics and usage reports for valid, permitted periods and metrics[cite: 2].
- Invalid, excessive, or unauthorized requests receive explicit stable errors.
- Empty or incomplete data coverage is represented explicitly with metadata and not misrepresented as a complete report.
- Results include period, applied filters, and generation metadata so they can be interpreted.
- Privacy rules prevent disclosure of individual-level or sufficiently small-group data.
- Long-running reports and exports expose a definitive job status and do not present partial results as complete.

## Decisions (DECISIONS.md)

- Refer to `DECISIONS.md` and issue [#65](https://github.com/paticama/ISO26-ISO2-A1.01/issues/65) for architectural, metric-catalogue, and privacy-threshold decisions.
