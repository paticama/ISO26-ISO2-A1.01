# UC-AAD-05 – Book Requested Examination (Admissions v0.5.0)

## Use Case
- **UC:** `UC-AAD-05`
- **RF:** `FR-AAD-12`
- **Phase:** Elaboration
- **Module:** `AAD` (v0.5.0)
- **Minutes:** Neutral
- **GitHub Issue:** Neutral
- **Actor:** Admissions Staff
- **Secondary Actors:** Physician, Patient, Medical staff, System Administrator (policy and access configuration only)
- **Description:** Enables Admissions Staff to book a pending examination request according to priority and policy, notify the patient, and update the operational worklist.
- **Preconditions:**
  1. Admissions Staff is authenticated and authorized.
  2. A valid pending examination request and patient exist.
  3. A compatible open schedule and booking policy are available.
  4. Patient and examination information is complete.
- **Postconditions:**
  1. A successful operation creates one confirmed booking and changes the request state to `Booked`.
  2. Capacity, notification status, and daily worklist are updated consistently.
  3. Failed or cancelled operations do not leave partial bookings.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** Admissions Staff selects a pending examination request.
  2. **[Server]** The system retrieves compatible slots, priority, and booking-policy constraints.
  3. **[Client]** Admissions Staff selects a slot and confirms patient and examination details.
  4. **[Client]** The client submits the booking request.
  5. **[Server]** The server atomically revalidates request state, eligibility, conflicts, and slot availability.
  6. **[Server]** The server creates the booking, changes the request to `Booked`, consumes capacity, and records an audit event.
  7. **[Server]** The system sends the configured patient notification and updates the daily worklist.
  8. **[Client]** The system displays the booking and notification outcome.
- **Alternative / Exception Flows:**
  - **A1. Occupied slot [Server -> Client]:** The booking is rejected and current compatible slots are returned.
  - **A2. Reschedule request [Server]:** The new slot is validated, the original history is preserved, and the worklist is updated.
  - **A3. Cancellation request [Server]:** The cancellation reason is recorded, capacity is released according to policy, and the worklist is updated.
  - **A4. Notification failure [Server -> Client]:** The booking remains auditable while notification status is marked failed or pending for retry.
  - **A5. Concurrent booking [Server]:** Stale availability is rejected and capacity cannot be oversold.
  - **A6. Unauthorized policy action [Server -> Client]:** System Administrator configuration is denied to Admissions Staff.

## RADIT Disciplines
- [x] Requirements: Specify examination booking, notification, and worklist integration.
- [ ] Architecture: Define booking transaction and notification boundaries.
- [ ] Design: Define booking DTOs, state transitions, and slot-validation contracts.
- [ ] Implementation: Implement booking, notification, and worklist update services.
- [ ] Testing: Test occupied slots, priority, state transitions, cancellation, rescheduling, and concurrency.

## Contracts and Interfaces
- `Client:` Admissions UI for slot selection and booking confirmation.
- `Server:` AAD examination-booking service.
- `Schedule interface:` Retrieves and atomically reserves compatible capacity.
- `Waitlist interface:` Reads priority and promotes eligible requests.
- `Booking interface:` Creates booking and manages `Pending`, `Booked`, `Rescheduled`, and `Cancelled` states.
- `Worklist interface:` Publishes confirmed booking changes to the daily worklist.
- `Notification interface:` Sends patient booking, rescheduling, and cancellation notifications.
- `Errors:` `SLOT_UNAVAILABLE`, `REQUEST_ALREADY_PROCESSED`, `PATIENT_NOT_ELIGIBLE`, `BOOKING_CONFLICT`, `NOTIFICATION_FAILED`, and `CONCURRENT_BOOKING` are returned explicitly; no partial booking is reported as successful.

## Acceptance Criteria
- [ ] A pending examination request can be booked according to priority and policy.
- [ ] Occupied or stale slots are rejected at confirmation time.
- [ ] A successful booking changes the request state to `Booked`.
- [ ] Patient notification outcome is visible and auditable.
- [ ] Rescheduling and cancellation preserve history and update the worklist.
- [ ] A failed operation does not create a partial booking.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../feature/CU00-initial-structure/DECISIONS.md).
- Requirements meeting minutes remain neutral until a confirmed link is available.

## Effort Estimation
- **Estimated effort without AI:** 12 hours
- **Estimated effort with AI (Copilot):** 6 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
