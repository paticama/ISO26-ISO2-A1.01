# UC-PHY-05 – Log Procedure Execution Details and Clinical/Operational Incidents (Physicians v0.2.0)

## Use Case
- **UC:** `UC-PHY-05`
- **FR:** `FR-PHY-18`, `FR-PHY-19`
- **Phase:** Construction
- **Module:** `sescam-execution-reporting` (v0.2.0)
- **Minutes:** [Meeting #3 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings)
- **GitHub Issue:** [#23](https://github.com/paticama/ISO26-ISO2-A1.01/issues/23)
- **Actor:** Physician User (Generalization: Inherits from base Patient User)
- **Secondary Actors:** Departmental RIS/PACS, Administrative / Admissions System
- **Description:** Allows the physician to record radiological procedure execution details and document clinical or operational incidents occurring during the examination.
- **Preconditions:**
  1. The physician is authenticated via SSO and selects an active scheduled procedure from the daily clinical worklist.
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
  - **2a. Document clinical or operational incident [Client -> Server]:** If an anomaly occurs (e.g., adverse contrast reaction, patient non-compliance, equipment malfunction), the physician logs an incident, specifying type, severity, and narrative. The server stores the incident audit record.
  - **2b. Procedure aborted due to incident [Server]:** If the incident prevents examination completion, the server marks the procedure as `Incomplete / Incident` and notifies Admissions for rescheduling.

## RADIT Disciplines
- [x] Requirements: Specify execution logs (contrast lot, equipment, consent) and incident tracking.
- [ ] Architecture: Incident logging audit model.
- [ ] Design: Execution service contracts.
- [ ] Implementation: Incident logging endpoints and rescheduling event dispatch.
- [ ] Testing: Tests for aborted procedures and rescheduling alerts.

## Contracts and Interfaces
- `es.uclm.esi.sescam.reporting.api.ProcedureExecutionService`
- `ExecutionLogDTO`, `IncidentDTO`

## Acceptance Criteria
- [ ] Execution details logged with timestamp and room utilization.
- [ ] Aborted procedures trigger rescheduling notification to Admissions.

## Decisions (DECISIONS.md)
- Complies with **ADR-003** and **ADR-008**.

## Effort Estimation
- **Estimated effort without AI:** 5 hours
- **Estimated effort with AI (Copilot):** 2 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
