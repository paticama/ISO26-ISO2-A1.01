# UC-SYS-03 — Audit & Compliance Management

## Use Case

- **UC:** `UC-SYS-03`
- **Requirement:** `FR-ADM-11`
- **Phase:** Inception
- **Module:** Administrators (`v0.3.0`)
- **Minutes:** [Minutes of meetings](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings) (details TBD)
- **GitHub Issue:** [#54](https://github.com/paticama/ISO26-ISO2-A1.01/issues/54)
- **Primary actor:** System Administrator
- **System:** Administrator Server
- **Goal:** Audit system activities and clinical data access.

## Goal and scope

The System Administrator searches and inspects audit records for system activity and clinical-data access to support security inspections and compliance review. The use case covers authorized, read-only audit queries and review of event details. It does not permit modification or deletion of audit records and does not replace formal compliance-report export.

## Preconditions

1. The administrator is authenticated and has permission to inspect the requested audit scope.
2. Audit records are available for the requested period.

## Postconditions

- Matching audit events are displayed only within the actor's authorized scope.
- Audit access is itself recorded where required by policy.
- Audit records remain unchanged by the inspection.

## Client–Server flow

1. The client presents permitted filters such as date range, actor, event category, resource, and outcome.
2. The administrator submits an audit query.
3. The server validates filters, applies authorization scope, and queries the audit store using bounded pagination.
4. The server returns matching event summaries and a continuation token where applicable.
5. The administrator selects an event to inspect its permitted detail.
6. The server rechecks access and returns a redacted detail view; the client displays it without altering the source event.

## Main flow

1. The administrator opens audit and compliance management.
2. The administrator chooses a time range and one or more available filters.
3. The system returns matching audit entries in chronological order, including event time, actor, action, resource category, outcome, and correlation reference as permitted.
4. The administrator opens a record to review additional authorized details and the event context.
5. The system records the inspection event when policy requires audit-of-audit access.
6. The administrator returns to results or refines the query.

## Alternate and error flows

- Invalid or excessive date range: return `AUDIT_RANGE_INVALID` and do not run an unbounded query.
- Filter or resource outside administrator scope: return `FORBIDDEN` without revealing whether inaccessible records exist.
- No matching events: return a successful empty result with explicit pagination metadata.
- An event detail contains restricted fields: return a redacted representation, not the protected values.
- An expired or invalid continuation token: return `AUDIT_QUERY_EXPIRED` and require a new search.
- Audit store unavailable: return `SERVICE_UNAVAILABLE`; do not present partial results as complete.

## RADIT

### Requirements

- Search and inspect system activity and clinical-data access audit records.
- Support bounded filters, stable ordering, and pagination.
- Enforce access scope and redact fields not required for the inspection.
- Preserve audit records as read-only and record audit review where configured.

### Analysis

Audit records are evidence and must remain immutable through this use case. Querying must be scoped and bounded to avoid exposing unrelated records or overwhelming the audit service. A distinction is required between an empty result and an incomplete or failed query.

### Design

Use `AuditSearchQuery`, `AuditEventSummary`, `AuditEventDetail`, and `AuditPage`. The server applies authorization and redaction before returning data. Results include a stable ordering key and continuation token.

### Implementation

Implement indexed event queries, filter validation, scoped authorization, field redaction, pagination, correlation identifiers, and audit-of-audit events where required. Keep inspection endpoints read-only.

### Testing

Test query filters, range limits, ordering and pagination, empty results, authorization boundaries, redaction, immutable records, audit-of-audit behavior, token expiry, and service failures.

### Deployment

Deploy audit indexes and retention configuration with access monitoring. Validate representative query performance, record integrity, and role-based visibility before enabling administrator inspection.

## Contracts and interfaces

### Client contract

- `searchAuditEvents(query) -> AuditPage`
- `getAuditEvent(eventId) -> AuditEventDetail`

### Server contract

- Enforce scope, filter limits, pagination, and redaction on every request.
- Never expose audit mutation operations through the inspection interface.
- Indicate whether results are complete and return stable error codes.

## Acceptance criteria

- A System Administrator can search and inspect audit records of system activities and clinical-data access within their authorized scope.
- Filters and pagination return deterministic, bounded results.
- Unauthorized records and restricted fields are not disclosed.
- The use case cannot modify or delete audit evidence.
- Empty results and service/query failures are represented distinctly.
- Audit access is recorded when required by configured policy.

## Decisions (DECISIONS.md)

- No specific decision is identified in issue [#54](https://github.com/paticama/ISO26-ISO2-A1.01/issues/54); record applicable decisions here when established.
