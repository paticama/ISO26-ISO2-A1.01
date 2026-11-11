# UC-ADM-03 – Audit & Compliance Management (Administrators v0.3.0)

## Use Case
- **UC:** `UC-ADM-03`
- **RF:** `FR-ADM-11`, `FR-ADM-12`, `FR-ADM-13`, `FR-ADM-14`, `FR-ADM-15`
- **Phase:** Late Construction
- **Module:** `sescam-system-admin` (v0.3.0)
- **Priority:** Low
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
- [ ] Design
- [ ] Implementation
- [ ] Testing

## Contracts and Interfaces
- **System:** Administrator Server.
- **Inputs:** Time range, supported audit filters, and authenticated actor identity.
- **Outputs:** Matching audit events and details, or explicit empty, validation, authorization, and query-error results.

## Acceptance Criteria
- [ ] Authorized actors can search or filter audit activity for a selected time range.
- [ ] Results identify the recorded actor, action, timestamp, and relevant target or outcome where available.
- [ ] Unauthorized actors cannot access audit records.
- [ ] Empty results and failed queries are distinguishable.
- [ ] Searching audit records does not alter the underlying records.

## Decisions
- See the [Decisions wiki](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Decisions).

## Effort Estimation
- **Estimated effort without AI:** 10 hours
- **Estimated effort with AI (Copilot):** 5 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
