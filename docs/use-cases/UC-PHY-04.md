# UC-PHY-04 – Author, Edit, and Review Diagnostic Reports and Medical Images (Physicians v0.1.0)

## Use Case
- **UC:** `UC-PHY-04`
- **RF:** `FR-PHY-12`, `FR-PHY-13`, `FR-PHY-14`, `FR-PHY-15`, `FR-PHY-16`
- **Phase:** Construction
- **Module:** Diagnostics module (`sescam-diagnostics` v0.1.0)
- **Minutes:** [Meeting #3 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings)
- **GitHub Issue:** [#22](https://github.com/paticama/ISO26-ISO2-A1.01/issues/22)
- **Actor:** Physician User (Generalization: Inherits from base Patient User)
- **Secondary Actors:** Departmental RIS/PACS Platform, Hospital EHR (HCE)
- **UML Relationships:** Extended by `UC-PHY-02 Order Periodic Follow-up Surveillance Procedure` (extension point: `FollowUpRecommendation`). Viewing DICOM images is an internal step; report addenda are an alternative flow.
- **Description:** Enables the physician to review acquired medical images using a DICOM-compliant viewer and author, edit, review, and digitally sign the corresponding diagnostic report.
- **Preconditions:**
  1. The physician is authenticated via SSO.
  2. The diagnostic procedure has been executed, and its medical images are archived in the departmental RIS/PACS.
- **Postconditions:**
  1. The finalized diagnostic report is persisted, linked to the DICOM study, and synchronized with the patient's EHR via HL7/FHIR.
  2. An availability event is published to notify the patient asynchronously.

### Extension Points
- **`FollowUpRecommendation`:** In Step 5, while authoring diagnostic conclusions, if clinical findings warrant periodic surveillance imaging, the physician may optionally trigger `UC-PHY-02 Order Periodic Follow-up Surveillance Procedure`.

- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** The physician selects a completed diagnostic procedure from the clinical reporting worklist.
  2. **[Server]** The system queries the RIS/PACS platform via DICOM protocols (`C-FIND` / `WADO-RS`) and returns image metadata and study access tokens.
  3. **[Client]** The system loads and renders the medical images inside the integrated DICOM-compliant viewer.
  4. **[Client]** The physician inspects the images and opens the diagnostic reporting editor.
  5. **[Client]** The physician authors findings and diagnostic conclusions (or loads an existing draft). *(Extension point: `FollowUpRecommendation` may be triggered here)*.
  6. **[Client -> Server]** The physician validates and submits the finalized diagnostic report with digital signature.
  7. **[Server]** The server saves the signed report, synchronizes the document with the hospital EHR using HL7 FHIR (`DiagnosticReport`), and emits a report availability event.
  8. **[Client]** Displays confirmation of report finalization and EHR synchronization.
- **Alternative / Exception Flows:**
  - **2a. PACS connection failure [Server -> Client]:** If DICOM images cannot be retrieved, the server logs a communication incident and alerts the client. The client displays a connection failure message.
  - **5a. Save as draft [Client -> Server]:** The physician saves the report as draft. The server updates status to `Draft`; no EHR synchronization or patient notification is dispatched until final validation.
  - **6a. Issue diagnostic report addendum [Client -> Server]:** If a finalized report requires modification, the physician authors an addendum preserving the original signature and logging an audit amendment.

## RADIT Disciplines
- [x] Requirements: Specify DICOM study retrieval, viewer rendering, reporting editor, HL7/FHIR sync, and patient notification.
- [ ] Architecture: Hexagonal adapters for DICOM and HL7/FHIR integration.
- [ ] Design: Diagnostic reporting service interfaces and DTOs.
- [ ] Implementation: Draft saving, report signing, and FHIR resource builder.
- [ ] Testing: Tests for report lifecycle (Draft -> Finalized) and notification triggering.

## Contracts and Interfaces
- `es.uclm.esi.sescam.diagnostics.api.DiagnosticReportingService`
- `ReportDTO`, `DicomStudyDTO`

## Acceptance Criteria
- [ ] DICOM medical images rendered in viewer.
- [ ] Finalized reports synchronized with hospital EHR via FHIR `DiagnosticReport`.
- [ ] Draft reports do not trigger patient notifications or EHR sync.

## Decisions (DECISIONS.md)
- Complies with **ADR-003**, **ADR-007** (DICOM, HL7, FHIR), and **ADR-008**.

## Effort Estimation
- **Estimated effort without AI:** 10 hours
- **Estimated effort with AI (Copilot):** 4 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
