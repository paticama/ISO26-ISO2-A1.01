# UC-AAD-05 – Book Requested Examination (C5 v1.0.0)

## Use Case
- **UC:** `UC-AAD-05`
- **RF:** `FR-AAD-12`
- **Phase:** Elaboration
- **Iteration:** 2
- **Priority:** High
- **Module:** `sescam-admissions` (C5, v1.0.0)
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#88](https://github.com/paticama/ISO26-ISO2-A1.01/issues/88)
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
- [x] Analysis: Define booking transaction and notification boundaries ([`docs/diagrams/UC-AAD-05/`](../diagrams/UC-AAD-05/)).
- [x] Design: `BookingService` and DTOs/exceptions in `sescam-common` (`common.admissions`); server [design class diagram](../diagrams/UC-AAD-05/uc-aad-05-class-design.puml) and [sequence diagram](../diagrams/UC-AAD-05/uc-aad-05-sequence-design.puml).
- [x] Implementation: Server slice (`sescam-admissions` 1.0.0: `BookingServiceImpl`, `Booking`, `BookingHistoryEntry`, `WorklistItem`; `BookingRestController`, `BookingExceptionHandler`, `BookingWiringConfig` in `sescam-server-app`). Stand-ins for pending modules: `InMemoryPendingOrderGateway` (C4), `DefaultPatientEligibilityGateway` (C6), `DefaultBookingPolicyGateway` (C2), `NoOpBookingNotificationGateway` (C8). Client slice pending.
- [x] Testing: JUnit 5 + Mockito (`sescam-common` `BookingContractsTest`, `sescam-admissions` `BookingServiceImplTest`, `sescam-server-app` `BookingRestControllerTest` with live C3 `ScheduleService` integration).

## Contracts and Interfaces
- `Client:` Admissions UI for slot selection and booking confirmation (pending client slice).
- `Server:` `com.sescam.radiodiagnostics.common.admissions.BookingService` in `sescam-admissions` (v1.0.0), REST API `/api/v1/bookings` in `sescam-server-app`.
- `Schedule interface (C3):` Consumes `ScheduleService.findCompatibleSlots(modality, from, to)`, `reserveSlotAtomically(slotId, expectedVersion, bookingReference)`, and `releaseSlot(slotId, bookingReference)`.
- `Outbound ports:` `PendingOrderGateway` (C4), `PatientEligibilityGateway` (C6), `BookingPolicyGateway` (C2), `BookingNotificationGateway` (C8), `BookingEventPublisher`, `AccessControlService.canManageBookings` (C1), `AuditService` (C1).
- `Booking interface:` Creates booking and manages `PENDING`, `BOOKED`, `RESCHEDULED`, and `CANCELLED` states with immutable `BookingHistoryEntry` trail.
- `Worklist interface:` Publishes confirmed, rescheduled, and cancelled booking changes to `WorklistRepository`.
- `Errors:` `INVALID_BOOKING_DATA` (400), `UNAUTHORIZED_BOOKING_ACCESS` (403), `BOOKING_NOT_FOUND` (404), `SLOT_UNAVAILABLE` (409, with `alternativeSlots`), `REQUEST_ALREADY_PROCESSED` (409), `BOOKING_CONFLICT` (409), `CONCURRENT_BOOKING` (409), `PATIENT_NOT_ELIGIBLE` (422).

## Acceptance Criteria
- [x] A pending examination request can be booked according to priority and policy.
- [x] Occupied or stale slots are rejected at confirmation time.
- [x] A successful booking changes the request state to `Booked`.
- [x] Patient notification outcome is visible and auditable.
- [x] Rescheduling and cancellation preserve history and update the worklist.
- [x] A failed operation does not create a partial booking.

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
