# UC-AAD-02 – Manage Schedules (Admissions v0.2.0)

## Use Case
- `UC-AAD-02 · FR-AAD-04, FR-AAD-05, FR-AAD-06, FR-AAD-07 · Elaboration · AAD (v0.2.0) · Minutes of meetings`
- **Priority:** High (realizes FR-AAD-04, FR-AAD-05, FR-AAD-06, FR-AAD-07)
- **UML Relationships:**
  - `<<include>>` Validate Schedule Availability & Conflicts [Condition: a schedule is opened, updated, closed, or reopened]

### Minutes (linked to wiki)
[Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)

- **Related Use Cases:** UC-AAD-04 reflects schedule changes; UC-AAD-05 consumes available slots.
- **GitHub Issue:** [#85](https://github.com/paticama/ISO26-ISO2-A1.01/issues/85)
- **Actor:** Admissions Staff
- **Secondary Actors:** Physician, Medical staff, System Administrator (configuration and access only)
- **Description:** Enables Admissions Staff to open, update, close, and reopen examination schedules while identifying affected appointments and supporting approved rescheduling actions.
- **Preconditions:**
  1. Admissions Staff is authenticated and authorized.
  2. Examination types, resources, locations, and scheduling rules are configured.
  3. Existing bookings are available for conflict and impact analysis.
- **Postconditions:**
  1. The schedule change is stored with an audit record.
  2. Affected appointments are identified and preserved for rescheduling or cancellation.
  3. Confirmed availability is exposed to the booking workflow.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** Admissions Staff opens schedule management and selects a date, resource, or location.
  2. **[Server]** The system retrieves current schedules, capacity, bookings, and applicable rules.
  3. **[Client]** Admissions Staff creates or edits a schedule, including time window, resource, location, and capacity.
  4. **[Client]** Admissions Staff confirms opening, updating, closing, or reopening the schedule.
  5. **[Server]** The server validates dates, capacity, overlaps, and resource availability.
  6. **[Server]** The server identifies affected appointments, persists the change, and records the audit event.
  7. **[Client]** The system displays resulting availability and any required rescheduling actions.
- **Alternative / Exception Flows:**
  - **A1. Schedule overlap [Server -> Client]:** The change is rejected and conflicting schedules are displayed.
  - **A2. Existing appointments affected [Server -> Client]:** The system lists affected appointments before availability is reduced.
  - **A3. Close schedule [Server]:** New bookings are prevented and existing appointments remain preserved.
  - **A4. Reopen schedule [Server]:** Capacity and conflicts are revalidated before slots become bookable.
  - **A5. Invalid configuration [Server -> Client]:** Invalid dates, resources, or capacity are reported and the change is not saved.
  - **A6. Unauthorized administration [Server -> Client]:** Configuration reserved for System Administrator is denied.

## RADIT Disciplines
- [x] Requirements: Specify schedule lifecycle and appointment impact handling.
- [ ] Analysis: Define schedule, resource, and booking boundaries.
- [ ] Design: Define schedule DTOs, conflict rules, and rescheduling contracts.
- [ ] Implementation: Implement schedule management and availability services.
- [ ] Testing: Test overlap, capacity, closing, reopening, and affected appointments.

## Contracts and Interfaces
- `Actor:` Admissions Staff.
- `System:` Admissions Workstation / AAD Module (`AAD` v0.2.0).
- `Detailed Specification:` [`UC-AAD-02-manage-schedules.md`](https://github.com/paticama/ISO26-ISO2-A1.01/blob/CU00-Alba/docs/use-cases/UC-AAD-02-manage-schedules.md)
- `Client:` Admissions UI for schedule search, creation, update, closing, and reopening.
- `Server:` AAD schedule and availability service.
- `Schedule interface:` Schedule, resource, capacity, and conflict-validation contract.
- `Booking interface:` Affected-appointment lookup and rescheduling hand-off.
- `Errors:` `SCHEDULE_OVERLAP`, `CAPACITY_EXCEEDED`, `INVALID_SCHEDULE`, `APPOINTMENTS_AFFECTED`, and `UNAUTHORIZED_OPERATION` are returned explicitly; no schedule change is silently committed.

## Acceptance Criteria
- [ ] Admissions Staff can open, update, close, and reopen authorized schedules.
- [ ] Invalid overlaps and capacity violations are rejected.
- [ ] Every affected appointment is identified before a change is committed.
- [ ] Closed schedules cannot accept new bookings.
- [ ] Reopened schedules pass availability and conflict checks.
- [ ] System Administrator configuration remains distinct from Admissions Staff operations.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../feature/CU00-initial-structure/DECISIONS.md).

## Effort Estimation
- **Estimated effort without AI:** 12 hours
- **Estimated effort with AI (Copilot):** 6 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
