# UC-ADM-05 – Generate Statistics & Usage Analytics (Administrators v0.5.0)

## Use Case
- **UC:** `UC-ADM-05`
- **RF:** `FR-ADM-22`, `FR-ADM-23`, `FR-ADM-24`, `FR-ADM-25`, `FR-ADM-26`
- **Phase:** Inception
- **Module:** `sescam-system-admin` (v0.5.0)
- **Priority:** Low
- **Minutes:** [Minutes of meetings](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings)
- **GitHub Issue:** [#65](https://github.com/paticama/ISO26-ISO2-A1.01/issues/65)
- **Actor:** System administrator
- **Description:** Enables an authorized system administrator to generate and review global statistics and usage reports to understand system activity and adoption.
- **Preconditions:**
  1. The administrator is authenticated and authorized to view global reporting data.
  2. Data is available for the requested period and metrics.
- **Postconditions:**
  1. A report is displayed with its metrics, period, filters, and data coverage.
  2. Empty, incomplete, invalid, unauthorized, and failed results are reported explicitly.
- **Main Success Scenario:**
  1. The administrator opens the statistics and analytics area.
  2. The administrator selects an available report, time range, and supported filters or grouping options.
  3. The system validates the request and calculates the report from available data.
  4. The system presents results with metric names, time boundaries, and data-coverage limitations.
  5. The administrator optionally exports the report in a supported format.
- **Alternative / Exception Flows:**
  - **A1. Unauthorized administrator:** Access to global reports is denied.
  - **A2. No data for period:** The system indicates that rather than presenting misleading zero or partial results.
  - **A3. Invalid or broad request:** The system explains how the request can be narrowed.
  - **A4. Report generation failure:** The system reports the failure and does not show incomplete results as complete.

## RADIT Disciplines
- [x] Requirements
- [x] Analysis
- [ ] Design
- [ ] Implementation
- [ ] Testing

## Contracts and Interfaces
- **Actor:** System administrator.
- **System:** Administrator Server.
- **Inputs:** Report or metric selection, time range, supported filters and grouping options, and authenticated administrator identity.
- **Outputs:** Global statistics and usage results, data-coverage or freshness information, and explicit validation, empty-result, authorization, and error messages.

## Acceptance Criteria
- [ ] Authorized administrators can generate global statistics and usage reports for a selected period.
- [ ] Reports identify metrics, period, and filters used.
- [ ] Empty or incomplete data coverage is communicated and not misrepresented.
- [ ] Invalid requests and report-generation failures are clearly reported.
- [ ] Unauthorized users cannot access global reporting data.
- [ ] Export follows access-control and data-protection requirements.

## Decisions
- See the [Decisions wiki](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Decisions).

## Effort Estimation
- **Estimated effort without AI:** 10 hours
- **Estimated effort with AI (Copilot):** 5 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
