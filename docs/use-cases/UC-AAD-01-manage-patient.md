# UC-AAD-01 – Manage Patient (C6 v1.0.0)

## Use Case
- **UC:** `UC-AAD-01`
- **RF:** `FR-AAD-01`, `FR-AAD-02`, `FR-AAD-03`, `FR-AAD-04`
- **Phase:** Elaboration
- **Iteration:** 1
- **Priority:** High
- **Module:** `sescam-patients` (C6, v1.0.0)
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#84](https://github.com/paticama/ISO26-ISO2-A1.01/issues/84)
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
- [x] Analysis: Define the patient-record and EHR integration boundaries (class and communication diagrams).
- [x] Design: Define patient DTOs, validation rules, service interfaces, design class and sequence diagrams.
- [x] Implementation: Implement the patient-management service in `sescam-patients`, REST controller in `sescam-server-app`, and integration adapter.
- [x] Testing: Unit, repository, controller, and contract tests for validation, audit, duplicate detection, synchronization, and authorization.

## Contracts and Interfaces
- **System:** `sescam-patients` (C6 v1.0.0).
- **Interfaces (`sescam-common`, `com.sescam.radiodiagnostics.common.patients`):** `PatientService` (`registerPatient`, `getPatientById`, `getPatientByCip`, `searchPatients`, `updatePatient`, `dischargePatient`), `EhrGateway` (`syncPatientRecord`). `AccessControlService` (`common.security`) defines `canManagePatients`.
- **DTOs & Types:** `PatientDTO`, `PatientSummaryDTO`, `CreatePatientRequestDTO`, `UpdatePatientRequestDTO`, `DischargePatientRequestDTO`, `PatientSearchCriteriaDTO`, `EhrSyncResultDTO`, `PatientStatus`, `Gender`, `PreferredNotificationChannel`, `AddressDTO`, `EmergencyContactDTO`.
- **Errors:** `PatientNotFoundException` (404), `DuplicatePatientException` (409), `InvalidPatientDataException` (400), `ConcurrentPatientUpdateException` (409), `UnauthorizedPatientAccessException` (403), `EhrSyncException` (502).
- **REST (`sescam-server-app`):**
  - `POST /api/v1/patients`: Register new patient.
  - `GET /api/v1/patients`: Search patients by identifier/CIP/name.
  - `GET /api/v1/patients/{id}`: Consult patient details.
  - `PUT /api/v1/patients/{id}`: Update patient demographic and contact data.
  - `POST /api/v1/patients/{id}/discharge`: Discharge patient.
  - Headers: `X-Actor-Id`, `X-Actor-Role`.
- **Client view (`sescam-client-app`):** `/patients` (search and worklist), `/patients/new` (registration form), `/patients/{id}` (detail view and update form), `/patients/{id}/discharge` (discharge dialog). REST client `PatientApiClient`.

## Analysis and Design Diagrams
- Analysis: [class diagram](../diagrams/UC-AAD-01/uc-aad-01-class-analysis.puml), [communication – normal flow](../diagrams/UC-AAD-01/uc-aad-01-comm-normal.puml), [communication – alternative flows](../diagrams/UC-AAD-01/uc-aad-01-comm-alt.puml).
- Design: [class diagram](../diagrams/UC-AAD-01/uc-aad-01-class-design.puml), [sequence diagram](../diagrams/UC-AAD-01/uc-aad-01-sequence-design.puml).

## Design Decisions
- **Optimistic Locking:** Every patient entity maintains a `version` attribute incremented on each update. If an update request sends an outdated expected version, `ConcurrentPatientUpdateException` (409) is thrown (A4).
- **Duplicate Prevention:** Before creating a new patient, the system validates that neither CIP nor DNI/NIE already exists in the patient registry; violations throw `DuplicatePatientException` (409) and log an audit event (A1).
- **Audit Traceability (NFR-11):** All registration, update, discharge, rejection, and authorization failure events are written directly to `AuditService` (`AuditEventDTO`).
- **External EHR Decoupling:** C6 defines an outbound `EhrGateway` port. Until the live HL7/FHIR connector exists in C9, `NoOpEhrGateway` marks synchronization as `SYNCED` or simulates failure when requested without failing the local transaction silently.

## Acceptance Criteria
- [x] Admissions Staff can register, consult, update, and discharge a patient.
- [x] Mandatory fields, formats, and duplicate warnings are enforced.
- [x] Successful and rejected operations are auditable.
- [x] Synchronization outcomes are visible and never represented as silent success.
- [x] System Administrator configuration privileges remain distinct from Admissions Staff operational privileges.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../DECISIONS.md) and [DECISIONS18-11.md](../../DECISIONS18-11.md) (ADR-013, ADR-014, ADR-015, ADR-016, ADR-017).

## Effort Estimation
- **Estimated effort without AI:** 8 hours
- **Estimated effort with AI (Copilot):** 4 hours
- **Estimated effort with AI + 25% safety margin:** 5 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
