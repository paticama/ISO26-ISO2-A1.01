# UC-AAD-03 – Manage Waitlist and Priority (Admissions v0.3.0)

## Use Case
- **UC:** `UC-AAD-03`
- **RF:** `FR-AAD-10`, `FR-AAD-11`
- **Phase:** Elaboration
- **Module:** `AAD` (v0.3.0)
- **Minutes:** Neutral
- **GitHub Issue:** Neutral
- **Actor:** Admissions Staff
- **Secondary Actors:** Physician, Patient, System Administrator (policy and access configuration only)
- **Description:** Enables Admissions Staff to manage pending examination requests, order them by priority, apply auditable corrections, and pass eligible requests to booking.
- **Preconditions:**
  1. Admissions Staff is authenticated and authorized.
  2. A valid patient and pending examination request are available.
  3. The priority policy is configured and active.
- **Postconditions:**
  1. The waitlist entry has an explicit state and queue position.
  2. Priority corrections and removals are auditable.
  3. Eligible requests can be passed to booking without losing priority context.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** Admissions Staff opens waitlist and priority management.
  2. **[Server]** The system retrieves pending requests and validates patient, examination, priority, and duplicate status.
  3. **[Client]** Admissions Staff reviews calculated priority and queue position.
  4. **[Client]** Admissions Staff confirms an entry or records an authorized correction with a reason.
  5. **[Server]** The server orders active entries using priority and waiting-time rules and records the audit event.
  6. **[Client]** Admissions Staff selects an eligible request for booking.
  7. **[Server]** The system updates the queue and transfers the request to the booking workflow.
- **Alternative / Exception Flows:**
  - **A1. Duplicate active request [Server]:** The system prevents a duplicate entry and displays the existing request.
  - **A2. Missing priority data [Server -> Client]:** The entry is marked incomplete and cannot be promoted.
  - **A3. Priority correction [Server]:** Original value, new value, reason, actor, and timestamp are stored.
  - **A4. Capacity unavailable [Server -> Client]:** The request remains waiting and its position is recalculated.
  - **A5. Unauthorized priority change [Server -> Client]:** The change is denied and the queue remains unchanged.

## RADIT Disciplines
- [x] Requirements: Specify waitlist intake, ordering, correction, and promotion.
- [ ] Architecture: Define request, waitlist, and priority-policy boundaries.
- [ ] Design: Define queue-ordering and audit interfaces.
- [ ] Implementation: Implement waitlist and priority management services.
- [ ] Testing: Test duplicate prevention, ordering, corrections, removals, and promotion.

## Contracts and Interfaces
- `Client:` Admissions UI for waitlist review, priority correction, removal, and promotion.
- `Server:` AAD waitlist and priority service.
- `Waitlist interface:` Pending-request intake, queue ordering, correction, removal, and promotion contract.
- `Booking interface:` Transfers an eligible request to the examination-booking service.
- `Errors:` `DUPLICATE_WAITLIST_ENTRY`, `MISSING_PRIORITY`, `INVALID_REQUEST_STATE`, `CAPACITY_UNAVAILABLE`, and `UNAUTHORIZED_OPERATION` are returned explicitly; no request is silently promoted or removed.

## Acceptance Criteria
- [ ] Pending examination requests can be received and displayed.
- [ ] Active entries are ordered deterministically by priority and waiting time.
- [ ] Corrections and removals require a reason and audit information.
- [ ] Duplicate active entries are prevented.
- [ ] Eligible requests can be transferred to booking with priority context intact.
- [ ] System Administrator policy configuration remains separate from Admissions Staff queue operations.

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
