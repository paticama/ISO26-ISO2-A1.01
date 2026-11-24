# UC-PHY-05 – Log Procedure Execution Details and Clinical/Operational Incidents (C7 v1.0.0)

## Use Case
- **UC:** `UC-PHY-05`
- **RF:** `FR-PHY-09`, `FR-PHY-10`, `FR-PHY-11`, `FR-PHY-17`
- **Phase:** Elaboration
- **Iteration:** 2
- **Module:** `sescam-diagnostics` (C7, v1.0.0)
- **Priority:** High (ISO/IEC/IEEE 29148:2018)
- **Dependencies:** FR-PHY-09 depends on FR-PHY-01 (UC-PHY-01) and FR-AAD-12 (UC-AAD-05).
- **UML Relationships:**
  - Extended by: Record Adverse Contrast Reaction & Clinical Complications [Extension Point: ClinicalComplication]
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#23](https://github.com/paticama/ISO26-ISO2-A1.01/issues/23)
- **Actor:** Physician
- **Secondary Actors:** Departmental RIS/PACS, Administrative / Admissions System
- **Description:** Allows the physician to record radiological procedure execution details and document clinical or operational incidents occurring during the examination.
- **Preconditions:**
  1. The physician is authenticated via SSO.
  2. The diagnostic procedure has been booked into a slot via UC-AAD-05 (FR-AAD-12).
- **Postconditions:**
  1. Procedure execution details and technical resource utilization logs are persisted in the system.
  2. Any documented clinical or operational incidents are registered for clinical auditing or rescheduling.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** The physician selects the active or recently completed diagnostic procedure.
  2. **[Client]** The physician enters execution details: actual start and end times, room/equipment utilized, contrast medium dose administered, and confirmation of informed consent verification.
  3. **[Client -> Server]** The physician submits the execution log.
  4. **[Server]** The system persists the execution record and updates the procedure status to `Executed / Pending Report`.
  5. **[Client]** Confirms execution recording.
- **Alternative / Exception Flows:**
  - **2a. Document clinical incident or adverse contrast reaction [Client -> Server]:** If an anomaly occurs (e.g., adverse contrast reaction, anaphylactoid event, equipment malfunction), the physician logs an incident, specifying type, severity, and narrative. The server stores the incident audit record.
  - **2b. Procedure aborted due to incident [Server]:** If the incident prevents examination completion, the server marks the procedure as `Incomplete / Incident` and notifies Admissions for rescheduling.

## RADIT Disciplines
- [x] Requirements: Specify execution logs (contrast lot, equipment, consent) and incident tracking.
- [x] Analysis: [activity diagram](../diagrams/UC-PHY-05/uc-phy-05-activity.puml) (main flow + 2a, 2b), [analysis class diagram](../diagrams/UC-PHY-05/uc-phy-05-class-analysis.puml) (boundary/control/entity, entity per module), [domain class diagram](../diagrams/puml/phy-class-domain.puml) and communication diagrams ([normal](../diagrams/puml/comm-phy-05-normal.puml), [2a](../diagrams/puml/comm-phy-05-alt2a.puml), [2b](../diagrams/puml/comm-phy-05-alt2b.puml)).
- [x] Design: contracts in `sescam-common` (see below); server [design class diagram](../diagrams/UC-PHY-05/uc-phy-05-class-design.puml) and [sequence diagram](../diagrams/UC-PHY-05/uc-phy-05-sequence-design.puml); client [design class diagram](../diagrams/UC-PHY-05/uc-phy-05-class-design-client.puml) and [sequence diagram](../diagrams/UC-PHY-05/uc-phy-05-sequence-design-client.puml).
- [x] Implementation: server slice (`sescam-diagnostics` 1.0.0: `ProcedureExecutionServiceImpl`, `ExecutionRules`, `ProcedureExecution`, `ClinicalIncident`; `ExecutionRestController` in server-app) and client slice (`ExecutionViewController`, `ExecutionForm`, `ExecutionApiClient`, `executions/new.html`). Stand-ins until the modules exist: bookings of C5 (`InMemoryScheduledProcedureGateway`) and the subscribers of the execution event in C4/C5.
- [x] Testing: JUnit 5 + Mockito. `sescam-diagnostics` 23 tests (rules per field, service per flow: main, 2a, 2b, abort without reason/incident, consent and CT dose missing, denied role, unknown procedure, second record, audit failure), `sescam-server-app` `ExecutionRestControllerTest` 5 (MockMvc), `sescam-client-app` 20 (form, REST client, view per flow), `sescam-common` `DiagnosticsContractsTest` 5. JaCoCo ≥ 80% in the four modules.

## Contracts and Interfaces
- `com.sescam.radiodiagnostics.common.diagnostics.ProcedureExecutionService` (C7; package follows the repository convention instead of `es.uclm.esi.sescam.diagnostics.api`): `listProceduresAwaitingExecution(actorId, actorRole)`, `recordExecution(ExecutionLogDTO) : ExecutionRecordDTO`
- DTOs and enums in `common.diagnostics`: `ExecutionLogDTO` (execution + incidents + abort in one request), `IncidentDTO`, `ExecutionRecordDTO`, `ClinicalIncidentDTO`, `ScheduledProcedureDTO`, `ProcedureExecutionEvent`, `ExecutionStatus`, `IncidentType`, `IncidentSeverity`
- Exceptions: `InvalidExecutionDataException` (400, with invalid fields), `UnauthorizedExecutionAccessException` (403), `ProcedureNotFoundException` (404), `ExecutionAlreadyRecordedException` (409)
- Outbound ports: `ScheduledProcedureGateway` (booked procedures, to be implemented by C5), `ProcedureEventPublisher` (C4 order status, C5 rescheduling), `AccessControlService.canLogProcedureExecution` (physician, radiologist, technician), `AuditService`
- REST API (server-app): `GET /api/v1/executions/pending`, `POST /api/v1/executions` (201; 400, 403, 404, 409). Client view: `/executions/new`.
- Decision: `ClinicalIncident` (C7, during an examination) is a different entity from `PatientIncident` (C8, PAT-05). Open decision #1 of `PLAN18-11.md` §9, to confirm at the team meeting.

## Acceptance Criteria
- [x] Execution details logged with timestamp and room utilization.
- [x] Execution metrics captured: contrast batch and volume, radiation dose (DLP, CTDIvol, mandatory for CT), radiotracer activity (mandatory for nuclear medicine). *(issue #23)*
- [x] Clinical incidents and adverse reactions documented with severity grading. *(issue #23)*
- [ ] Incident report linked to patient clinical history and flagged for audit. *(issue #23) Linked by `patientId` and audited (`CLINICAL_INCIDENT_LOGGED`); showing it in the history is PAT-02 (It3).*
- [ ] Aborted procedures trigger rescheduling notification to Admissions. *`ProcedureExecutionEvent` with `requiresRescheduling()` is published; nothing consumes it until C5 subscribes.*

## Decisions (DECISIONS.md)
- Complies with **ADR-003** and **ADR-008**.

## Effort Estimation
- **Estimated effort without AI:** 5 hours
- **Estimated effort with AI (Copilot):** 2 hours
- **Estimated effort with AI + 25% safety margin:** 2.5 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
