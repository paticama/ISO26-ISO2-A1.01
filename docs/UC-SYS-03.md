# UC-ADM-03 — Audit and Compliance Management

## Use Case

- **UC:** `UC-ADM-03`
- **Requirements:** `FR-ADM-11`, `FR-ADM-12`, `FR-ADM-13`, `FR-ADM-14`, `FR-ADM-15`
- **Phase:** Elaboration (Iteration 1)
- **Module:** `sescam-security` (`v0.2.0`) — Module ADM (Administration)
- **Minutes:** [Minutes of meetings](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings) (Meeting #4 minutes)
- **GitHub Issue:** [#54](https://github.com/paticama/ISO26-ISO2-A1.01/issues/54)
- **Primary actor:** System Administrator
- **Secondary actors:** None
- **System:** SESCAM Radiodiagnostics (`sescam-security`)
- **Goal:** Allow a System Administrator to investigate system activity and access to clinical data using reliable, read-only, and appropriately protected audit records.

## UML Relationships

- **None** (no `<<include>>` or `<<extend>>` relationships apply; optional result export and audit-of-audit inspection logging are internal flows of this use case rather than standalone shared or optional use cases).
- **Related use cases (context only):** `UC-ADM-02` manages user roles and access control (access to audit records depends on the System Administrator having audit-view permission). The audit events shown by this use case are generated through `Record Audit Event` across mutating use cases (`UC-ADM-01`, `UC-ADM-02`, `UC-ADM-04`), whose generation mechanism is outside the scope of this use case.

## Goal and scope

The System Administrator searches and inspects audit records for system activity and clinical-data access to support security inspections and compliance review[cite: 2]. The use case covers authorized, read-only audit queries, review of event details, and optional result export where supported. It does not permit modification or deletion of audit records and does not specify the underlying audit event-generation mechanism (`Record Audit Event`).

## Preconditions

1. The System Administrator is authenticated (via Corporate SSO) and has permission to view audit information (`UC-ADM-02`).
2. Audit records are available for the requested period and event types.
3. The security/audit service (`sescam-security`) is available.

## Postconditions

- Matching audit events are displayed only within the System Administrator's authorized scope.
- Audit access is itself recorded where required by policy.
- Audit records remain completely unchanged (immutable) by the inspection.

## Client–Server flow

1. The client presents permitted filters such as date range, actor, event category, resource context, and outcome.
2. The System Administrator submits an audit query.
3. The server validates filters, applies authorization scope, and queries the audit store using bounded pagination.
4. The server returns matching event summaries in chronological order and a continuation token where applicable.
5. The System Administrator selects an event to inspect its permitted detail (or optionally exports the permitted results where supported).
6. The server rechecks access and returns a redacted detail view; the client displays it without altering the source event.

## Main flow

1. The System Administrator opens audit and compliance management.
2. The System Administrator chooses a time range and one or more supported filters (such as user, activity type, or record context).
3. The system returns matching audit entries in a readable, time-ordered view, including timestamp, actor, action, resource category, outcome, and correlation reference as permitted.
4. The System Administrator opens a record to inspect additional authorized details and event context.
5. The system records the inspection event when policy requires audit-of-audit access, and optionally allows exporting the results if supported.
6. The System Administrator returns to results or refines the query.

## Alternate and error flows

- **Invalid or excessive date range:** Invalid or overly broad date range -> return `AUDIT_RANGE_INVALID`, do not run an unbounded query, and allow the System Administrator to narrow or retry it.
- **Unauthorized or out-of-scope access:** Filter or resource outside System Administrator scope -> return `FORBIDDEN` without revealing whether inaccessible records exist; unauthenticated requests -> return `UNAUTHENTICATED`.
- **Empty results:** No matching events -> return a successful empty result clearly distinguishing zero matches from a query failure, with explicit pagination metadata.
- **Restricted detail fields:** An event detail contains restricted fields -> return a redacted representation rather than protected values.
- **Expired pagination token:** An expired or invalid continuation token -> return `AUDIT_QUERY_EXPIRED` and require a new search.
- **Audit store unavailable:** Query or audit store fails -> return `SERVICE_UNAVAILABLE`; do not present partial or incomplete results as complete.

## RADIT

### Requirements

- Search and inspect system activity and clinical-data access audit records[cite: 2].
- Support bounded filters, stable chronological ordering, and pagination.
- Enforce access scope and redact fields not required for the inspection.
- Preserve audit records as read-only and record audit review where configured.

### Analysis

Audit records are compliance evidence and must remain immutable through this use case. Querying must be scoped and bounded to avoid exposing unrelated records or overwhelming the audit service. A clear distinction is required between an empty result and an incomplete or failed query. Event types, retention periods, permitted filters, and export rules are refined during analysis.

### Design

Use `AuditSearchQuery`, `AuditEventSummary`, `AuditEventDetail`, and `AuditPage`. The server applies authorization and field redaction before returning data. Results include a stable ordering key and continuation token.

### Implementation

Implement indexed event queries, filter validation, scoped authorization, field redaction, pagination, correlation identifiers, and audit-of-audit events where required. Keep all inspection endpoints strictly read-only.

### Testing

Test query filters, range limits, ordering and pagination, empty results, authorization boundaries, redaction, immutable records, audit-of-audit behavior, token expiry, and service failures.

### Deployment

Deploy audit indexes and retention configuration with access monitoring. Validate representative query performance, record integrity, and role-based visibility before enabling System Administrator inspection.

## Contracts and interfaces

### Client contract

- `searchAuditEvents(query) -> AuditPage`
- `getAuditEvent(eventId) -> AuditEventDetail`

### Server contract

- Enforce scope, filter limits, pagination, and field redaction on every request.
- Never expose audit mutation or deletion operations through the inspection interface.
- Indicate whether results are complete and return stable error codes (`AUDIT_RANGE_INVALID`, `FORBIDDEN`, `UNAUTHENTICATED`, `AUDIT_QUERY_EXPIRED`, `SERVICE_UNAVAILABLE`).

## Acceptance criteria

- A System Administrator can search and inspect audit records of system activities and clinical-data access within their authorized scope[cite: 2].
- Results identify the recorded actor, action, timestamp, and relevant target or outcome where recorded.
- Filters and pagination return deterministic, bounded results.
- Unauthorized records and restricted fields are not disclosed.
- The use case cannot modify or delete audit evidence.
- Empty results and service/query failures are represented distinctly.
- Audit access is recorded when required by configured policy.

## Decisions (DECISIONS.md)

- Refer to `DECISIONS.md` and issue [#54](https://github.com/paticama/ISO26-ISO2-A1.01/issues/54) for architectural, retention, and audit-policy decisions.
