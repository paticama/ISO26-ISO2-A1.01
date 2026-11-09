# UC-AAD-01 – Manage Patient (Admissions v0.1.0)

## Use Case
- **UC:** `UC-AAD-01`
- **RF:** `FR-AAD-01`, `FR-AAD-02`, `FR-AAD-03`
- **Phase:** Elaboration
- **Module:** `AAD` (v0.1.0)
- **Minutes:** Neutral
- **GitHub Issue:** Neutral
- **Actor:** Admissions Staff
- **Secondary Actors:** Admissions system, Electronic Health Record (EHR / HCE), System Administrator (configuration and access only)
- **Description:** Enables Admissions Staff to register, consult, update, and discharge patient records while preserving identity, contact, and administrative data and synchronizing the configured patient-record system.
- **Preconditions:**
  1. Admissions Staff is authenticated and authorized.
  2. The AAD application and patient-record integration are available.
  3. For an update or discharge, the patient can be located with an approved identifier.
- **Postconditions:**
  1. The requested patient operation is stored with its actor, timestamp, and outcome.
  2. The external patient record is synchronized or the synchronization failure is explicitly recorded.
  3. The patient is available to related admissions workflows.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** Admissions Staff opens patient management and searches by an approved identifier.
  2. **[Server]** The system retrieves matching records and possible duplicate warnings.
  3. **[Client]** Admissions Staff selects an existing patient or starts a new registration.
  4. **[Client]** Admissions Staff enters or updates required patient information and confirms the operation.
  5. **[Server]** The server validates mandatory fields, formats, duplicate rules, and authorization.
  6. **[Server]** The server persists the registration, consultation, update, or discharge, records an audit entry, and synchronizes the configured external record.
  7. **[Client]** The system displays the result and synchronization status.
- **Alternative / Exception Flows:**
  - **A1. Possible duplicate [Server -> Client]:** The system displays suspected duplicates and requires selection of the existing record or an approved justification.
  - **A2. Invalid or incomplete data [Client]:** Submission is blocked and invalid fields are highlighted.
  - **A3. Synchronization failure [Server -> Client]:** The system reports the failure and marks synchronization as pending or failed according to policy.
  - **A4. Concurrent update [Server]:** The system detects a newer version and requires the user to review before saving again.
  - **A5. Unauthorized operation [Server -> Client]:** The system denies the operation and records the authorization failure.

## RADIT Disciplines
- [x] Requirements: Specify registration, consultation, update, and discharge workflow.
- [ ] Architecture: Define the patient-record and EHR integration boundaries.
- [ ] Design: Define patient DTOs, validation rules, and service interfaces.
- [ ] Implementation: Implement the AAD patient-management service and integration adapter.
- [ ] Testing: Unit and integration tests for validation, audit, synchronization, and authorization.

## Contracts and Interfaces
- `Client:` Admissions UI for search, registration, consultation, update, and discharge.
- `Server:` AAD patient-management service.
- `Patient/EHR interface:` Patient record and EHR synchronization adapter.
- `Audit interface:` Records actor, timestamp, operation, and outcome.
- `Errors:` `PATIENT_NOT_FOUND`, `DUPLICATE_PATIENT`, `INVALID_PATIENT_DATA`, `UNAUTHORIZED_OPERATION`, `EHR_SYNC_FAILED`, and `CONCURRENT_UPDATE` are returned explicitly; no error is treated as success.

## Acceptance Criteria
- [ ] Admissions Staff can register, consult, update, and discharge a patient.
- [ ] Mandatory fields, formats, and duplicate warnings are enforced.
- [ ] Successful and rejected operations are auditable.
- [ ] Synchronization outcomes are visible and never represented as silent success.
- [ ] System Administrator configuration privileges remain distinct from Admissions Staff operational privileges.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../feature/CU00-initial-structure/DECISIONS.md).
- Requirements meeting minutes remain neutral until a confirmed link is available.

## Effort Estimation
- **Analysis:** 2 hours
- **Design:** 1.5 hours
- **Implementation:** 2.5 hours
- **Testing:** 1 hour
- **Documentation:** 1 hour
- **Total without AI:** 8 hours
- **Total with AI (Copilot):** 4 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
