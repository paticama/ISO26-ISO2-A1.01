# UC-AAD-02 – Manage Schedules (C3 v1.0.0)

## Use Case
- **UC:** `UC-AAD-02`
- **RF:** `FR-AAD-04`, `FR-AAD-05`, `FR-AAD-06`, `FR-AAD-07`
- **Phase:** Elaboration
- **Iteration:** 2
- **Priority:** High
- **Module:** `sescam-schedules` (C3, v1.0.0)
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
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
- [x] Analysis: [analysis class diagram](../diagrams/UC-AAD-02/uc-aad-02-class-analysis.puml) (Schedule and Slot in C3; bookings stay in C5 and are only referenced by id) and communication diagrams ([normal](../diagrams/UC-AAD-02/uc-aad-02-comm-normal.puml), [A1-A6](../diagrams/UC-AAD-02/uc-aad-02-comm-alt.puml)).
- [x] Design: interfaces and DTOs in `sescam-common` (see Contracts below); server [design class diagram](../diagrams/UC-AAD-02/uc-aad-02-class-design.puml) and [sequence diagram](../diagrams/UC-AAD-02/uc-aad-02-sequence-design.puml). Client design diagrams come with the client slice.
- [ ] Implementation: server slice (`sescam-schedules` 1.0.0 + `ScheduleRestController`) and client slice (`sescam-client-app` schedule view).
- [ ] Testing: JUnit 5 + Mockito, one test per acceptance criterion and per alternative flow, JaCoCo ≥ 80%.

## Contracts and Interfaces
- `Client:` Admissions UI for schedule search, creation, update, closing, and reopening.
- `Server:` Schedule and availability service in `sescam-schedules`, REST endpoint in `sescam-server-app`.
- `Schedule interface:` `com.sescam.radiodiagnostics.common.schedules.ScheduleService` (C3):
  - Admissions Staff: `searchSchedules`, `getSchedule`, `openSchedule`, `updateSchedule`, `closeSchedule`, `reopenSchedule` → `ScheduleChangeResultDTO` (resulting schedule, affected appointments, `reschedulingRequired`).
  - Consumed by C5 for UC-AAD-05 (in-process, no actor): `findCompatibleSlots(modality, from, to)`, `reserveSlotAtomically(slotId, expectedVersion, bookingReference)`, `releaseSlot(slotId, bookingReference)`.
- DTOs and enums in `common.schedules`: `ScheduleDTO`, `ScheduleSummaryDTO`, `SlotDTO`, `AffectedAppointmentDTO`, `ScheduleChangeResultDTO`, `OpenScheduleRequestDTO`, `UpdateScheduleRequestDTO`, `ScheduleStatusChangeRequestDTO`, `ScheduleSearchCriteriaDTO`, `ScheduleStatus`, `ScheduleOperation`, `ScheduleErrorCode`.
- `Booking interface:` affected-appointment lookup is done in C3 from the booking references held by each slot; the rescheduling hand-off is the event `ScheduleChangedEvent` sent through the outbound port `ScheduleEventPublisher` (no-op until C5 implements it, ADR-014). Affected appointments are never dropped: they stay in `appointmentsPendingRescheduling` until C5 releases them.
- Collaborators: `PolicyService` (C2) for the limit `schedules.max-capacity-per-slot` set by the System Administrator (default 4), `AuditService` (C1) for every change, rejection and denial (NFR-11). Roles are checked in C3 by `ScheduleAccessPolicy` (Admissions Staff operates; System Administrator and clinical staff only consult) until the RBAC permissions of UC-ADM-02 are merged.
- `Errors:` `SCHEDULE_OVERLAP`, `CAPACITY_EXCEEDED`, `INVALID_SCHEDULE`, `APPOINTMENTS_AFFECTED`, and `UNAUTHORIZED_OPERATION` are returned explicitly; no schedule change is silently committed. Also `SCHEDULE_NOT_FOUND`, `CONCURRENT_MODIFICATION` (optimistic locking) and `SLOT_UNAVAILABLE` (booking hand-off). All are subclasses of `ScheduleException`.
- REST API (server-app): `GET /api/v1/schedules` (date, resourceId, locationId, status), `GET /api/v1/schedules/{id}`, `POST /api/v1/schedules` (201), `PUT /api/v1/schedules/{id}`, `POST /api/v1/schedules/{id}/close`, `POST /api/v1/schedules/{id}/reopen`, `GET /api/v1/schedules/slots/available` (modality, from, to). Status codes: 400 `INVALID_SCHEDULE`, 403 `UNAUTHORIZED_OPERATION`, 404 `SCHEDULE_NOT_FOUND`, 409 `SCHEDULE_OVERLAP` / `APPOINTMENTS_AFFECTED` / `CONCURRENT_MODIFICATION`, 422 `CAPACITY_EXCEEDED`.
- Analysis and design diagrams: [`docs/diagrams/UC-AAD-02/`](../diagrams/UC-AAD-02/)

## Acceptance Criteria
- [ ] Admissions Staff can open, update, close, and reopen authorized schedules.
- [ ] Invalid overlaps and capacity violations are rejected.
- [ ] Every affected appointment is identified before a change is committed.
- [ ] Closed schedules cannot accept new bookings.
- [ ] Reopened schedules pass availability and conflict checks.
- [ ] System Administrator configuration remains distinct from Admissions Staff operations.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../DECISIONS.md).
- Requirements meeting minutes remain neutral until a confirmed link is available.

## Effort Estimation
- **Estimated effort without AI:** 12 hours
- **Estimated effort with AI (Copilot):** 6 hours
- **Estimated effort with AI + 25% safety margin:** 7.5 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
