# UC-PAT-04 – Consult My Follow-ups and Clinical Guidelines (Patient v0.5.0)

## Use Case
- **UC:** `UC-PAT-04`
- **RF:** `FR-PAT-16`, `FR-PAT-17`, `FR-PAT-18`, `FR-PAT-19`
- **Phase:** Late Construction
- **Iteration:** 5
- **Module:** `sescam-patient-portal` (v0.5.0)
- **Priority:** Low (ISO/IEC/IEEE 29148:2018 / Inherited from FR-PAT-16 and FR-PAT-17)
- **UML Relationships:**
  - Related Use Cases: UC-PHY-02 creates the periodic surveillance plans shown here; UC-ADM-01 maintains the clinical protocols and guidelines; once a follow-up is booked (UC-AAD-05) it is also shown in UC-PAT-01.
- **Dependencies:** FR-PAT-16 depends on FR-PAT-01 (UC-PAT-01) and FR-PHY-05 (UC-PHY-02).
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#101](https://github.com/paticama/ISO26-ISO2-A1.01/issues/101)
- **Actor:** Patient
- **Secondary Actors:** None
- **Description:** Enables a patient to consult their planned follow-up examinations and periodic checkups, and the patient-facing clinical guidelines associated with each surveillance protocol.
- **Preconditions:**
  1. The patient is authenticated via corporate Single Sign-On (SSO).
  2. The patient is registered in the system (UC-AAD-01).
  3. Periodic surveillance plans and future scheduling requests are available from UC-PHY-02 (FR-PHY-05).
- **Postconditions:**
  1. The patient has consulted only their own follow-ups and the guidelines linked to them.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** The patient opens the *My Follow-ups* section of the patient portal.
  2. **[Server]** The system retrieves the surveillance plans of the authenticated patient: protocol (e.g., pulmonary nodule surveillance, oncological restaging, screening), examination type, recurrence interval, next due date, and status (`Planned`, `Pending Booking`, `Booked`, `Completed`).
  3. **[Client]** The system displays the follow-ups ordered by next due date.
  4. **[Client]** The patient selects a follow-up.
  5. **[Server]** The system returns the follow-up details and the list of published clinical guidelines associated with its protocol.
  6. **[Client]** The system displays the details and the guidelines.
  7. **[Client]** The patient selects a guideline.
  8. **[Server]** The system returns the guideline content with its version and publication date.
  9. **[Client]** The system displays the guideline.
- **Alternative / Exception Flows:**
  - **A1. No follow-ups [Server -> Client]:** The system informs the patient that no periodic follow-up is planned.
  - **A2. Follow-up already booked [Client]:** The system shows the booked appointment and links to it in UC-PAT-01.
  - **A3. No guideline associated [Server -> Client]:** The system informs the patient that no guideline is available for this protocol.
  - **A4. Guideline withdrawn or updated [Server]:** Only the current published version is shown.
  - **A5. Follow-up not owned by the patient [Server -> Client]:** Access is denied and the attempt is audited.

## RADIT Disciplines
- [x] Requirements: Specify follow-up consultation and guideline display.
- [ ] Architecture: Define read access from the patient portal to the order-entry module and the protocol catalogue.
- [ ] Design: Define follow-up and guideline DTOs and read-only query interfaces.
- [ ] Implementation: Implement the patient follow-up service and portal views.
- [ ] Testing: Tests for ownership, status display, booked-follow-up linking, and guideline versioning.

## Contracts and Interfaces
- `Client:` Patient portal (web and mobile) for follow-up list, details, and guidelines.
- `Server:` Patient follow-up service.
- `Surveillance interface:` Read-only access to surveillance plans created in UC-PHY-02.
- `Protocol catalogue interface:` Read-only access to published guidelines (UC-ADM-01).
- `Errors:` `FOLLOW_UP_NOT_FOUND`, `GUIDELINE_NOT_FOUND`, and `ACCESS_DENIED` are returned explicitly; no error is treated as success.

## Acceptance Criteria
- [ ] The patient sees only their own follow-ups; any attempt to access another patient's follow-up is denied and audited.
- [ ] Each follow-up shows protocol, examination type, interval, next due date, and status.
- [ ] Booked follow-ups link to the corresponding appointment.
- [ ] Only published guidelines associated with the follow-up's protocol are shown, in their current version.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../feature/CU00-initial-structure/DECISIONS.md).
- Operational assumption: patient-facing guidelines are informational content approved by the service; they do not replace clinical advice.

## Effort Estimation
- **Estimated effort without AI:** 6 hours
- **Estimated effort with AI (Copilot):** 3.5 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
