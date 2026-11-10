# UC-PAT-01 – Manage My Appointments (Patient v0.1.0)

## Use Case
- **UC:** `UC-PAT-01`
- **RF:** `FR-PAT-01`, `FR-PAT-02`, `FR-PAT-03`, `FR-PAT-04`, `FR-PAT-05`, `FR-PAT-10`
- **Phase:** Elaboration
- **Module:** `sescam-patient-portal` (v0.1.0)
- **Priority:** High (ISO/IEC/IEEE 29148:2018 / Inherited from FR-PAT-01 and FR-PAT-02)
- **UML Relationships:**
  - Related Use Cases: UC-AAD-05 creates the bookings shown here and processes the rescheduling and cancellation requests raised here (not `<<extend>>`); UC-PAT-03 notifies the patient of every change; UC-ADM-01 provides the preparation guidelines and the booking policy (confirmation and cancellation windows).
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#102](https://github.com/paticama/ISO26-ISO2-A1.01/issues/102)
- **Actor:** Patient
- **Secondary Actors:** Admissions Staff (processes rescheduling and cancellation requests through UC-AAD-05)
- **Description:** Enables a patient to consult their scheduled radiological appointments and the associated pre-procedure preparation instructions, and to confirm an appointment, request its rescheduling, or cancel it according to the booking policy.
- **Preconditions:**
  1. The patient is authenticated via corporate Single Sign-On (SSO).
  2. The patient is registered in the system (UC-AAD-01).
- **Postconditions:**
  1. The patient has consulted only their own appointments and preparation instructions.
  2. If confirmed, the appointment status changes to `Confirmed`.
  3. If rescheduling was requested, a rescheduling request is stored as `Pending Review` and the appointment remains unchanged until Admissions processes it.
  4. If cancelled, the appointment status changes to `Cancelled`, the slot is released according to policy, and Admissions is informed.
  5. Every state change is recorded with actor, timestamp, and outcome.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** The patient opens the *My Appointments* section of the patient portal (web or mobile).
  2. **[Server]** The system retrieves the upcoming appointments that belong to the authenticated patient, with date, time, examination type, modality, location/room, and status.
  3. **[Client]** The system displays the appointments in chronological order.
  4. **[Client]** The patient selects an appointment.
  5. **[Server]** The system returns the appointment details, the preparation instructions defined in the Catalogue of Examination Types (fasting, contrast medium, informed consent, items to bring), and the actions permitted by the booking policy.
  6. **[Client]** The system displays the details, the preparation instructions, and the available actions (*Confirm*, *Request rescheduling*, *Cancel*).
  7. **[Client]** The patient chooses *Confirm*.
  8. **[Server]** The server checks ownership, current status, and the confirmation window; stores the confirmation; changes the status to `Confirmed`; and records an audit entry.
  9. **[Client]** The system displays the confirmation.
- **Alternative / Exception Flows:**
  - **A1. No scheduled appointments [Server -> Client]:** The system informs the patient that there are no upcoming appointments and offers access to planned follow-ups (UC-PAT-04).
  - **A2. Request rescheduling [Client -> Server] (at step 7):** The patient gives a reason and optional preferred dates. The server stores a rescheduling request as `Pending Review`, routes it to Admissions (UC-AAD-05), and keeps the appointment unchanged. The patient can see the request status.
  - **A3. Cancel appointment [Client -> Server] (at step 7):** The system asks for explicit confirmation and an optional reason. The server cancels the appointment, releases the slot according to policy, informs Admissions, and triggers a cancellation notice (UC-PAT-03).
  - **A4. Cancellation of an Urgent/Stat or Preferential examination [Server -> Client]:** Before cancelling, the system warns that the examination has clinical priority and recommends contacting the service. If the patient proceeds, the ordering physician can see the cancellation in the order status (UC-PHY-03).
  - **A5. Action outside the policy window [Server -> Client]:** If the configured confirmation or cancellation deadline has passed, the action is rejected and the service contact information is shown.
  - **A6. Pending request already exists [Server -> Client]:** A second rescheduling request for the same appointment is rejected and the existing request status is shown.
  - **A7. Appointment executed or already cancelled [Server -> Client]:** All actions are disabled and the current status is shown.
  - **A8. Appointment not owned by the patient [Server -> Client]:** Access is denied and the attempt is audited.

## RADIT Disciplines
- [x] Requirements: Specify appointment consultation, preparation instructions, confirmation, rescheduling request, and cancellation workflow.
- [ ] Architecture: Define the boundary between the patient portal and the admissions booking service.
- [ ] Design: Define appointment DTOs, request state machine (`Pending Review`, `Accepted`, `Rejected`), and service interfaces.
- [ ] Implementation: Implement the patient appointment service and portal views.
- [ ] Testing: Unit and integration tests for ownership, policy windows, state transitions, and request routing.

## Contracts and Interfaces
- `Client:` Patient portal (web and mobile) for appointment list, details, instructions, and actions.
- `Server:` Patient appointment service.
- `Appointment interface:` Reads the patient's appointments and applies `Confirmed` and `Cancelled` transitions.
- `Catalogue interface:` Reads preparation instructions for the examination type (UC-ADM-01).
- `Admissions request interface:` Sends rescheduling and cancellation requests to UC-AAD-05.
- `Notification interface:` Publishes appointment events to UC-PAT-03.
- `Audit interface:` Records actor, timestamp, operation, and outcome.
- `Errors:` `APPOINTMENT_NOT_FOUND`, `ACCESS_DENIED`, `INVALID_APPOINTMENT_STATE`, `ACTION_OUTSIDE_POLICY_WINDOW`, and `DUPLICATE_PENDING_REQUEST` are returned explicitly; no error is treated as success.

## Acceptance Criteria
- [ ] The patient sees only their own appointments; any attempt to access another patient's appointment is denied and audited.
- [ ] Each appointment shows date, time, examination type, location, and status; its details include the preparation instructions of the examination type.
- [ ] A confirmation changes the status to `Confirmed` and is audited.
- [ ] A rescheduling request never modifies the appointment until Admissions processes it, and its status is visible to the patient.
- [ ] A cancellation requires explicit confirmation, releases the slot, and informs Admissions and the patient.
- [ ] Actions outside the policy window, or on executed or cancelled appointments, are rejected.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../feature/CU00-initial-structure/DECISIONS.md).
- Operational assumption: confirmation and cancellation windows are configurable booking-policy parameters (UC-ADM-01).

## Effort Estimation
- **Estimated effort without AI:** 14 hours
- **Estimated effort with AI (Copilot):** 8 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
