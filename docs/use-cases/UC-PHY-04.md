# UC-PHY-04 – Author, Edit, and Review Diagnostic Reports and Medical Images (sescam-diagnostics v0.2.0)

## Use Case
- **UC:** `UC-PHY-04`
- **RF:** `FR-PHY-12`, `FR-PHY-13`, `FR-PHY-14`, `FR-PHY-15`, `FR-PHY-16`
- **Phase:** Elaboration
- **Iteration:** 2
- **Module:** `sescam-diagnostics` (v0.2.0)
- **Priority:** High (ISO/IEC/IEEE 29148:2018)
- **Dependencies:** FR-PHY-16 depends on FR-PHY-15 and FR-ADM-13 (UC-ADM-03).
- **UML Relationships:**
  - `<<include>>` View Medical Image Series in DICOM/PACS Viewer
  - Extended by: Issue Diagnostic Report Addendum [Extension Point: PostSignatureRevision]
  - Extended by: UC-PHY-02 (Order Periodic Follow-up) [Extension Point: FollowUpRecommendation]
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#22](https://github.com/paticama/ISO26-ISO2-A1.01/issues/22)
- **Actor:** Physician
- **Secondary Actors:** Departmental RIS/PACS Platform, Hospital EHR (HCE), Patient User (Notification)
- **Description:** Enables the physician to review acquired medical images using a DICOM-compliant viewer and author, edit, review, and digitally sign the corresponding diagnostic report.
- **Preconditions:**
  1. The physician is authenticated via SSO.
  2. The diagnostic procedure has already been executed, and its execution data is recorded in UC-PHY-05 (FR-PHY-09), which precedes UC-PHY-04 within Iteration 2; its medical images are archived in the departmental RIS/PACS.
  3. The audit service in UC-ADM-03 (FR-ADM-13) is available for report-revision history.
- **Postconditions:**
  1. The finalized diagnostic report is persisted, linked to the DICOM study, and synchronized with the patient's EHR via HL7/FHIR.
  2. The patient receives an automated notification regarding diagnostic report availability.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** The physician selects a completed diagnostic procedure from the clinical reporting worklist.
  2. **[Server]** The system queries the RIS/PACS platform via DICOM protocols (`C-FIND` / `WADO-RS`) and returns image metadata and study access tokens.
  3. **[Client]** The system loads and renders the medical images inside the integrated DICOM-compliant viewer.
  4. **[Client]** The physician inspects the images and opens the diagnostic reporting editor.
  5. **[Client]** The physician authors findings and diagnostic conclusions (or loads an existing draft).
  6. **[Client -> Server]** The physician validates and submits the finalized diagnostic report.
  7. **[Server]** The server saves the signed report, synchronizes the document with the hospital EHR using HL7 FHIR (`DiagnosticReport`), and emits an automated notification to the Patient user.
  8. **[Client]** Displays confirmation of report finalization and EHR synchronization.
- **Alternative / Exception Flows:**
  - **2a. PACS connection failure [Server -> Client]:** If DICOM images cannot be retrieved, the server logs a communication incident and alerts the client. The client displays a connection failure message.
  - **5a. Save as draft [Client -> Server]:** The physician saves the report as draft. The server updates status to `Draft`; no EHR synchronization or patient notification is dispatched until final validation.

## RADIT Disciplines
- [x] Requirements: Specify DICOM study retrieval, viewer rendering, reporting editor, HL7/FHIR sync, and patient notification.
- [ ] Architecture: Hexagonal adapters for DICOM and HL7/FHIR integration.
- [ ] Design: Diagnostic reporting service interfaces and DTOs.
- [ ] Implementation: Draft saving, report signing, and FHIR resource builder.
- [ ] Testing: Tests for report lifecycle (Draft -> Finalized) and notification triggering.

## Contracts and Interfaces
- `es.uclm.esi.sescam.reporting.api.DiagnosticReportingService`
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
- **Estimated effort with AI + 25% safety margin:** 5 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
