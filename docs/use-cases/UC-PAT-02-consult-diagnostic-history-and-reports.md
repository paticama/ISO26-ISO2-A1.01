# UC-PAT-02 – Consult My Diagnostic History and Reports (sescam-diagnostics v0.3.0)

## Use Case
- **UC:** `UC-PAT-02`
- **RF:** `FR-PAT-11`, `FR-PAT-12`, `FR-PAT-13`
- **Phase:** Construction
- **Iteration:** 3
- **Module:** `sescam-diagnostics` (v0.3.0)
- **Priority:** Medium by team decision; FR-PAT-11 to FR-PAT-13 were aligned to Medium so the use case matches the rule that a use case takes the highest priority among its functional requirements.
- **UML Relationships:**
  - Related Use Cases: UC-PHY-05 registers the executed procedures listed here; UC-PHY-04 produces the signed reports and addenda; UC-PAT-03 sends the report-availability notice that links to this use case.
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#106](https://github.com/paticama/ISO26-ISO2-A1.01/issues/106)
- **Actor:** Patient
- **Secondary Actors:** Hospital EHR (HCE) (read-only source of synchronized reports)
- **Description:** Enables a patient to consult the history of their completed diagnostic procedures, check whether the report of each procedure is pending or available, and read the finalized report.
- **Preconditions:**
  1. The patient is authenticated via corporate Single Sign-On (SSO).
  2. The patient is registered in the system (UC-AAD-01).
- **Postconditions:**
  1. The patient has consulted only their own procedures and reports.
  2. Each consultation of clinical data is recorded in the audit log.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** The patient opens the *My Diagnostic History* section of the patient portal.
  2. **[Server]** The system retrieves the completed procedures that belong to the authenticated patient, with execution date, examination type, modality, centre, and report status (`Pending` or `Available`).
  3. **[Client]** The system displays the history in reverse chronological order, with filters by date range and modality.
  4. **[Client]** The patient selects a procedure.
  5. **[Server]** The system returns the procedure details (execution date, examination type, ordering physician, report status) and, if the report is available, the finalized report with its signature date and any addenda. The consultation is audited.
  6. **[Client]** The system displays the details and the report.
- **Alternative / Exception Flows:**
  - **A1. No completed procedures [Server -> Client]:** The system informs the patient that there are no records yet.
  - **A2. Report pending [Server -> Client]:** The procedure is shown with status `Pending`; no draft content is ever displayed.
  - **A3. Report with addenda [Server -> Client]:** The latest signed version is shown, marked as amended, together with the list of addenda and their dates.
  - **A4. EHR unavailable [Server -> Client]:** The system shows the data stored locally and warns that external records may be incomplete.
  - **A5. Procedure not owned by the patient [Server -> Client]:** Access is denied and the attempt is audited.

## RADIT Disciplines
- [x] Requirements: Specify history consultation, report-status checking, and finalized-report display.
- [ ] Architecture: Define read access from the patient portal to the reporting module and the EHR adapter.
- [ ] Design: Define history and report DTOs and read-only query interfaces.
- [ ] Implementation: Implement the patient history service and portal views.
- [ ] Testing: Tests for ownership, draft-report concealment, addenda display, and audit of consultations.

## Contracts and Interfaces
- `Client:` Patient portal (web and mobile) for history list, filters, and report view.
- `Server:` Patient diagnostic-history service.
- `Reporting interface:` Read-only access to finalized reports and addenda produced in UC-PHY-04.
- `EHR interface:` Read-only retrieval of synchronized reports.
- `Audit interface:` Records every consultation of clinical data.
- `Errors:` `RECORD_NOT_FOUND`, `ACCESS_DENIED`, `REPORT_NOT_AVAILABLE`, and `EHR_UNAVAILABLE` are returned explicitly; no error is treated as success.

## Acceptance Criteria
- [ ] The patient sees only their own completed procedures; any attempt to access another patient's record is denied and audited.
- [ ] Each procedure shows its report status as `Pending` or `Available`.
- [ ] Draft reports are never visible to the patient.
- [ ] Finalized reports show their signature date and any addenda.
- [ ] Every consultation of a report is recorded in the audit log.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../feature/CU00-initial-structure/DECISIONS.md).
- Operational assumption: only reports signed in UC-PHY-04 are released to the patient.

## Effort Estimation
- **Estimated effort without AI:** 8 hours
- **Estimated effort with AI (Copilot):** 4.5 hours
- **Estimated effort with AI + 25% safety margin:** 5.63 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
