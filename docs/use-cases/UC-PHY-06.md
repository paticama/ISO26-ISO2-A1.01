# UC-PHY-06 – Consult Operational Metrics on Orders (Physicians v0.1.0)

## Use Case
- **UC:** `UC-PHY-06`
- **RF:** `FR-PHY-18`, `FR-PHY-19`, `FR-PHY-20`
- **Phase:** Late Construction
- **Module:** `sescam-analytics` (v0.1.0)
- **Priority:** Low (ISO/IEC/IEEE 29148:2018)
- **UML Relationships:**
  - None
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#24](https://github.com/paticama/ISO26-ISO2-A1.01/issues/24)
- **Actor:** Physician
- **Secondary Actors:** —
- **Description:** Enables the physician to consult analytical and operational metrics regarding their prescribed orders, including patient waiting times, equipment utilization, and follow-up protocol adherence.
- **Preconditions:**
  1. The physician is authenticated in the system via SSO.
- **Postconditions:**
  1. The system computes and displays analytical reports and operational metrics.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** The physician navigates to the operational analytics section.
  2. **[Client]** The physician defines filter parameters (date range, modality, procedure type, priority tier).
  3. **[Server]** The system calculates metrics associated with the physician's orders:
     - Patient waiting times (prescription to execution, execution to report).
     - Technical resource utilization across imaging modalities and rooms.
     - Adherence percentage for periodic follow-up surveillance protocols.
  4. **[Client]** Renders summary indicator cards and interactive charts.
- **Alternative / Exception Flows:**
  - **3a. No data matching criteria [Server -> Client]:** The system informs the physician that no order records match the specified query filters.

## RADIT Disciplines
- [x] Requirements: Specify metrics computation (waiting times, utilization, surveillance adherence).
- [ ] Architecture: Aggregation and query service architecture.
- [ ] Design: Analytics interfaces and DTOs.
- [ ] Implementation: Analytics service and summary cards.
- [ ] Testing: Calculation accuracy verification tests.

## Contracts and Interfaces
- `es.uclm.esi.sescam.analytics.api.OperationalMetricsService`
- `MetricsFilterDTO`, `OperationalMetricsDTO`

## Acceptance Criteria
- [ ] Metrics calculated correctly across specified date ranges and modalities.
- [ ] Empty state handled gracefully when no records match filter criteria.

## Decisions (DECISIONS.md)
- Complies with **ADR-003** and **ADR-009** (Product Quality).

## Effort Estimation
- **Estimated effort without AI:** 6 hours
- **Estimated effort with AI (Copilot):** 2 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
