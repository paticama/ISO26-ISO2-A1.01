# UC-AAD-01 — Manage Patient

## Phase Elaboration

- **Module:** AAD (Admissions, Appointments and Diagnostics)
- **Minutes:** Neutral
- **GitHub Issue:** Neutral
- **Primary actor:** Admissions Staff
- **Supporting actor:** System Administrator
- **Related decisions:** [CU00 initial structure decisions](../../feature/CU00-initial-structure/DECISIONS.md)

## Goal and scope

Admissions Staff creates, retrieves, updates, and deactivates patient records while preserving identity, contact, and administrative data. System Administrator configures the permissions and reference data required by the process; the administrator does not perform ordinary patient registration on behalf of Admissions Staff.

The use case covers validation, duplicate detection, persistence, auditability, and clear error reporting. Clinical notes and diagnostic results are outside this use case.

## Client–Server flow

1. The client presents the patient search or registration form.
2. Admissions Staff submits a query or patient command.
3. The client validates required fields and sends a typed request over the authenticated API.
4. The server authorizes the actor, validates business rules, checks for duplicates, and reads or writes the patient repository.
5. The server returns a typed success or explicit error response.
6. The client shows the result and preserves actionable validation messages without exposing internal implementation details.

## Main flow

1. Admissions Staff selects **New patient** or searches by an allowed identifier.
2. The client requires the minimum identity fields and normalizes safe display input.
3. The server validates identifier format, date of birth, contact data, and uniqueness.
4. The server creates the patient with an immutable identifier, status `active`, timestamps, and audit metadata.
5. Admissions Staff reviews the returned summary and may update permitted administrative fields.
6. For a deactivation request, the server verifies that no active appointment or worklist dependency prevents the change, then records the reason and audit event.

## Alternate and error flows

- A duplicate identifier returns `PATIENT_DUPLICATE` and identifies the existing record without disclosing unrelated data.
- Missing or malformed fields return `VALIDATION_ERROR` with field-level messages.
- A patient referenced by an update is absent or inactive when an active record is required; return `PATIENT_NOT_FOUND` or `PATIENT_INACTIVE`.
- A conflicting concurrent update returns `CONFLICT` and requires the client to reload before retrying.
- An unauthorized actor returns `FORBIDDEN`; an unauthenticated request returns `UNAUTHENTICATED`.
- Repository or network failure returns `SERVICE_UNAVAILABLE`; no partial success is shown.

## RADIT

### Requirements

- Search by the configured patient identifier and permitted demographic fields.
- Create and update patient administrative data with field-level validation.
- Preserve immutable identity fields and an audit trail.
- Deactivate only when dependent AAD records allow it.

### Analysis

Patient identity is a shared aggregate. Commands must be idempotent where a request identifier is supplied, and reads must apply actor-specific visibility. Duplicate detection is performed server-side because client validation cannot guarantee uniqueness.

### Design

The client uses `PatientQuery`, `CreatePatientCommand`, `UpdatePatientCommand`, and `DeactivatePatientCommand`. The server exposes patient endpoints backed by a patient repository and audit service. Error codes are stable API values; human-readable messages remain localized at the client.

### Implementation

Implement typed request/response models, server-side validation, authorization policies, repository transactions, optimistic concurrency, and client field mapping. Record create, update, and deactivate events with actor and timestamp.

### Testing

Test valid create, search, update, and deactivate flows; duplicate and malformed inputs; authorization; concurrent updates; dependency blocking; idempotency; and repository failure handling. Verify that no sensitive fields are returned outside the actor's permission.

### Deployment

Deploy the API and client together with the patient schema migration, indexes for configured search fields, and audit retention settings. Validate migration rollback and health checks before enabling the module.

## Contracts and interfaces

### Client contract

- `searchPatients(query) -> PatientSummary[]`
- `createPatient(command) -> Patient`
- `updatePatient(id, command, version) -> Patient`
- `deactivatePatient(id, reason, version) -> DeactivationReceipt`

### Server contract

- Enforce authentication, authorization, validation, uniqueness, concurrency, and transaction boundaries.
- Return stable error codes and correlation identifiers.
- Never return a success response for a transaction that was not committed.

### Interfaces

The HTTP/JSON interface uses versioned routes under `/api/aad/patients`. Requests carry an idempotency key for creates and an entity version for updates. Responses include correlation metadata and either the typed payload or an explicit error envelope.

## Acceptance criteria

- Admissions Staff can create a valid patient and retrieve the persisted record.
- A duplicate identifier is rejected with `PATIENT_DUPLICATE`.
- Every invalid field is reported with a deterministic field-level error.
- System Administrator permissions can be configured without granting ordinary registration by default.
- A stale version cannot overwrite a newer patient record.
- Deactivation is blocked when a dependent record requires an active patient.
- All successful mutations have an auditable actor, timestamp, and correlation identifier.

## Effort estimation

| Work item | Estimate |
|---|---:|
| Requirements and analysis | 4 h |
| API and domain design | 5 h |
| Client implementation | 7 h |
| Server and persistence implementation | 10 h |
| Automated and integration testing | 8 h |
| Deployment and migration verification | 3 h |
| **Total** | **37 h** |

