# UC-ADM-03 – Audit & Compliance Management (C1 v1.1.0)

## Use Case
- **UC:** `UC-ADM-03`
- **RF:** `FR-ADM-11`, `FR-ADM-12`, `FR-ADM-13`, `FR-ADM-14`, `FR-ADM-15`
- **Phase:** Elaboration
- **Iteration:** 1
- **Module:** `sescam-security` (C1, v1.1.0)
- **Priority:** High
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#54](https://github.com/paticama/ISO26-ISO2-A1.01/issues/54)
- **Actor:** System administrator or authorized compliance reviewer
- **Description:** Enables an authorized actor to investigate system activity and access to clinical data using protected audit records.
- **Preconditions:**
  1. The actor is authenticated and authorized to view audit information.
  2. Audit records are available for the requested period and event types.
- **Postconditions:**
  1. Matching audit events are displayed or an explicit empty/error result is returned.
  2. The workflow does not modify or delete audit records.
- **Main Success Scenario:**
  1. The actor opens the audit and compliance area.
  2. The actor selects a time range and supported filters.
  3. The system returns matching events in a readable, time-ordered view.
  4. The actor inspects event details, including actor, action, timestamp, and relevant target or outcome where recorded.
  5. The actor optionally exports or records the results where supported.
- **Alternative / Exception Flows:**
  - **A1. Unauthorized actor:** Access to audit records is denied.
  - **A2. No matching records:** The system clearly reports the empty result.
  - **A3. Broad query or query failure:** The system explains the problem and allows the actor to refine or retry the request.

## RADIT Disciplines
- [x] Requirements
- [x] Analysis
- [x] Design
- [x] Implementation
- [x] Testing

## Contracts and Interfaces
- **System:** sescam-security.
- **Interfaces (`sescam-common`, `com.sescam.radiodiagnostics.common.security`):** `AuditQueryService` (new, read only: `searchEvents`, `exportEventsAsCsv`) and `AuditService` (`recordEvent`, used by every module to write audit events).
- **DTOs:** `AuditSearchCriteriaDTO`, `AuditSearchResultDTO`, `AuditRecordDTO`, `AuditResultStatus`, `AuditEventDTO`.
- **Errors:** `UnauthorizedAuditAccessException` (A1, HTTP 403), `InvalidAuditQueryException` (A3, 400), `AuditQueryTooBroadException` (A3, 422: range > 31 days or > 500 matches), `AuditQueryFailedException` (A3, 503).
- **Client view (`sescam-client-app`):** `/security/audit` (form, time-ordered table, details, CSV export), REST client `AuditApiClient`, form validations in `AuditFilterForm` (range required, from ≤ to, ≤ 31 days, safe text filters).
- **REST (`sescam-server-app`):** `GET /api/v1/audit/events?from&to&performedBy&action&resourceKey` and `GET /api/v1/audit/events/export` (CSV), headers `X-Actor-Id`, `X-Actor-Role`. No write endpoints.
- **Inputs:** Time range, supported audit filters, and authenticated actor identity.
- **Outputs:** Matching audit events and details, or explicit empty, validation, authorization, and query-error results.

## Analysis and Design Diagrams
- Analysis: [class diagram](../diagrams/UC-ADM-03/uc-adm-03-class-analysis.puml), [communication – normal flow](../diagrams/UC-ADM-03/uc-adm-03-comm-normal.puml), [communication – alternative flows](../diagrams/UC-ADM-03/uc-adm-03-comm-alt.puml).
- Design: [class diagram](../diagrams/UC-ADM-03/uc-adm-03-class-design.puml), [sequence diagram](../diagrams/UC-ADM-03/uc-adm-03-sequence-design.puml).
- Design decisions: audit events are immutable and the repository is append-only (ADR-008); each search, export or denied attempt is itself recorded as a new audit event (`AUDIT_RECORDS_QUERIED`, `AUDIT_RECORDS_EXPORTED`, `AUDIT_ACCESS_DENIED`), so existing records are never changed; an empty result is a `NO_MATCHES` status, while failures are always errors.

## Acceptance Criteria
- [x] Authorized actors can search or filter audit activity for a selected time range.
- [x] Results identify the recorded actor, action, timestamp, and relevant target or outcome where available.
- [x] Unauthorized actors cannot access audit records.
- [x] Empty results and failed queries are distinguishable.
- [x] Searching audit records does not alter the underlying records.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../DECISIONS.md).

## Effort Estimation
- **Estimated effort without AI:** 10 hours
- **Estimated effort with AI (Copilot):** 5 hours
- **Estimated effort with AI + 25% safety margin:** 6.25 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
