# UC-AAD-04 – Manage Daily Worklist (Admissions v0.4.0)

## Use Case
- `UC-AAD-04 · FR-AAD-08 · Construction · AAD (v0.4.0) · Minutes of meetings`
- **Priority:** Medium (realizes FR-AAD-08)
- **UML Relationships:** None

### Minutes (linked to wiki)
[Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)

- **Related Use Cases:** UC-AAD-02 provides schedule changes; UC-AAD-05 provides booking, rescheduling, and cancellation events.
- **GitHub Issue:** [#87](https://github.com/paticama/ISO26-ISO2-A1.01/issues/87)
- **Actor:** Admissions Staff
- **Secondary Actors:** Medical staff, System Administrator (configuration and access only)
- **Description:** Enables Admissions Staff to build and maintain the daily worklist from confirmed appointments and make the current operational workload available to authorized medical staff.
- **Preconditions:**
  1. Admissions Staff is authenticated and authorized.
  2. Confirmed appointments and current schedule data are available.
  3. The operational date and authorized scope are configured.
- **Postconditions:**
  1. The worklist reflects the current confirmed appointment state.
  2. Cancellations, reschedules, and operational status changes are auditable.
  3. Medical staff can consult the authorized daily workload.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** Admissions Staff selects an operational date and opens the daily worklist.
  2. **[Server]** The system loads confirmed appointments and current schedule data.
  3. **[Server]** The system orders entries by time, priority, resource, and configured rules.
  4. **[Client]** Admissions Staff filters the worklist and records an allowed operational status or exception.
  5. **[Server]** The server validates the transition, records actor and timestamp, and refreshes the worklist.
  6. **[Client]** Medical staff consults the current worklist within the authorized scope.
- **Alternative / Exception Flows:**
  - **A1. Appointment cancelled [Server]:** The entry is removed or marked according to policy and the cancellation is recorded.
  - **A2. Appointment rescheduled [Server]:** The entry is moved to the new date and time and its history is preserved.
  - **A3. Invalid status transition [Server -> Client]:** The system rejects the transition and shows permitted states.
  - **A4. Stale data [Server -> Client]:** The system indicates unavailable or stale data and does not imply a successful update.
  - **A5. Unauthorized scope [Server -> Client]:** The system denies access outside the actor's authorized worklist.

## RADIT Disciplines
- [x] Requirements: Specify daily worklist generation and operational status handling.
- [ ] Analysis: Define worklist composition and synchronization boundaries.
- [ ] Design: Define worklist views, filters, and status-transition contracts.
- [ ] Implementation: Implement daily worklist generation and update services.
- [ ] Testing: Test generation, filtering, cancellation, rescheduling, and authorization.

## Contracts and Interfaces
- `Actor:` Admissions Staff.
- `System:` Admissions Workstation / AAD Module (`AAD` v0.4.0).
- `Detailed Specification:` [`UC-AAD-04-manage-daily-worklist.md`](https://github.com/paticama/ISO26-ISO2-A1.01/blob/CU00-Alba/docs/use-cases/UC-AAD-04-manage-daily-worklist.md)
- `Client:` Admissions UI and Medical Staff worklist view.
- `Server:` AAD daily-worklist service.
- `Worklist interface:` Builds and filters entries from confirmed bookings.
- `Booking interface:` Reads booking, cancellation, and rescheduling events.
- `Errors:` `STALE_WORKLIST_DATA`, `INVALID_STATUS_TRANSITION`, `APPOINTMENT_NOT_FOUND`, and `UNAUTHORIZED_SCOPE` are returned explicitly; stale data is never presented as current.

## Acceptance Criteria
- [ ] The worklist is built from confirmed appointments.
- [ ] Changes, cancellations, and reschedules are reflected without duplicate entries.
- [ ] Entries can be filtered within the authorized scope.
- [ ] Only valid operational status transitions are accepted.
- [ ] Medical staff can access the current worklist permitted to them.
- [ ] System Administrator configuration remains separate from daily operational actions.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../feature/CU00-initial-structure/DECISIONS.md).

## Effort Estimation
- **Estimated effort without AI:** 10 hours
- **Estimated effort with AI (Copilot):** 5 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
