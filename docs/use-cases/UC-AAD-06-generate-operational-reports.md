# UC-AAD-06 – Generate Operational Reports (Admissions v0.6.0)

## Use Case
- **UC:** `UC-AAD-06`
- **RF:** `FR-AAD-09`
- **Phase:** Elaboration
- **Module:** `AAD` (v0.6.0)
- **Minutes:** Neutral
- **GitHub Issue:** Neutral
- **Actor:** Admissions Staff
- **Secondary Actors:** System Administrator (system-wide reporting configuration and access management)
- **Description:** Enables Admissions Staff to generate operational reports on workload, occupancy, waiting times, cancellations, capacity, and booking outcomes within the permitted scope.
- **Preconditions:**
  1. Admissions Staff is authenticated and authorized.
  2. The reporting period and scope are valid.
  3. Schedule, booking, waitlist, and worklist data are available.
- **Postconditions:**
  1. The report is displayed or exported with its period and filters.
  2. Empty, incomplete, restricted, or failed results are explicitly identified.
  3. Source operational records remain unchanged and report access is auditable.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** Admissions Staff opens operational reporting and selects a report type.
  2. **[Client]** Admissions Staff selects the date range and permitted scope filters.
  3. **[Server]** The server validates authorization and calculates the requested operational metrics.
  4. **[Server]** The server returns metrics, period, filters, and data-quality status.
  5. **[Client]** The system displays the report and allows export in a supported format.
  6. **[Server]** The system records actor, filters, timestamp, and outcome.
- **Alternative / Exception Flows:**
  - **A1. No matching data [Server -> Client]:** An explicit empty result is returned with the selected period and filters.
  - **A2. Invalid filter [Client]:** The system highlights the invalid parameter and blocks generation.
  - **A3. Restricted data [Server]:** The result is limited to the actor's authorized scope and the restriction is recorded.
  - **A4. Export failure [Server -> Client]:** The failure is reported while the on-screen result is preserved when possible.
  - **A5. Reporting source unavailable [Server -> Client]:** The system returns `SERVICE_UNAVAILABLE` and does not present an incomplete report as complete.

## RADIT Disciplines
- [x] Requirements: Specify occupancy, waiting-time, cancellation, capacity, and booking reporting.
- [ ] Architecture: Define reporting data-access and authorization boundaries.
- [ ] Design: Define report filters, metric calculations, and export contracts.
- [ ] Implementation: Implement operational report generation and export.
- [ ] Testing: Test metrics, filters, permissions, empty data, and export failures.

## Contracts and Interfaces
- `Client:` Admissions UI for report type, period, filters, display, and export.
- `Server:` AAD operational-reporting service.
- `Reporting interface:` Calculates workload, occupancy, waiting-time, cancellation, capacity, and booking metrics.
- `Schedule/Booking interface:` Reads schedule capacity and appointment states.
- `Waitlist/Worklist interface:` Reads queue and operational status data.
- `Errors:` `INVALID_REPORT_FILTER`, `NO_DATA`, `RESTRICTED_SCOPE`, `SOURCE_DATA_INCOMPLETE`, and `REPORT_EXPORT_FAILED` are returned explicitly; incomplete results are never presented as complete reports.

## Acceptance Criteria
- [ ] Admissions Staff can generate permitted operational reports within the authorized scope.
- [ ] The date range and filters are visible in every result.
- [ ] Empty, invalid, restricted, and failed results are explicit.
- [ ] Reports do not mutate patients, schedules, bookings, waitlist entries, or worklists.
- [ ] Report access and export activity are auditable.
- [ ] System Administrator report configuration remains separate from operational report generation.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../feature/CU00-initial-structure/DECISIONS.md).
- Requirements meeting minutes remain neutral until a confirmed link is available.

## Effort Estimation
- **Analysis:** 2.5 hours
- **Design:** 2 hours
- **Implementation:** 3 hours
- **Testing:** 1.5 hours
- **Documentation:** 1 hour
- **Total without AI:** 10 hours
- **Total with AI (Copilot):** 5 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
