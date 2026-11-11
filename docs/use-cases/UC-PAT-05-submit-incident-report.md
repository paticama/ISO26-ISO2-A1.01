# UC-PAT-05 – Submit Incident Report (Patient v0.4.0)

## Use Case
- **UC:** `UC-PAT-05`
- **RF:** `FR-PAT-14`
- **Phase:** Construction
- **Iteration:** 4
- **Module:** `sescam-patient-portal` (v0.4.0)
- **Priority:** Medium (ISO/IEC/IEEE 29148:2018 / Inherited from FR-PAT-14)
- **UML Relationships:**
  - Related Use Cases: UC-PHY-05 records clinical and operational incidents from the physician side using the same incident categories; UC-PAT-01 and UC-PAT-02 provide the appointments and procedures an incident can refer to.
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#103](https://github.com/paticama/ISO26-ISO2-A1.01/issues/103)
- **Actor:** Patient
- **Secondary Actors:** Admissions Staff (reviews submitted incidents — operational assumption)
- **Description:** Enables a patient to report an incident related to the radiology service (scheduling, waiting time, facilities, care received, or a clinical concern), optionally linked to one of their appointments or procedures, and to follow its status.
- **Preconditions:**
  1. The patient is authenticated via corporate Single Sign-On (SSO).
- **Postconditions:**
  1. The incident is stored with status `Received`, a tracking identifier, and the authenticated patient as author.
  2. The incident is available to the reviewing staff.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** The patient opens the *Report an Incident* section of the patient portal.
  2. **[Server]** The system returns the incident categories and the patient's recent appointments and procedures.
  3. **[Client]** The patient selects a category, optionally links an appointment or procedure, and enters the date and a description.
  4. **[Client]** The client checks the mandatory fields and submits the report.
  5. **[Server]** The server validates the data and ownership of the linked item, stores the incident with status `Received`, assigns a tracking identifier, and records an audit entry.
  6. **[Client]** The system displays the confirmation with the tracking identifier.
- **Alternative / Exception Flows:**
  - **A1. Missing mandatory data [Client]:** Submission is blocked and the missing fields are highlighted.
  - **A2. Clinical concern category selected [Client]:** The system reminds the patient that the portal is not an emergency channel and shows the service and emergency contact numbers before allowing submission.
  - **A3. Linked item not owned by the patient [Server -> Client]:** The submission is rejected and the attempt is audited.
  - **A4. Consult previous incidents [Client -> Server]:** The patient opens *My Incidents* and the system lists their incidents with status (`Received`, `In Review`, `Closed`) and staff response.

## RADIT Disciplines
- [x] Requirements: Specify incident submission, categories, linking, and status tracking.
- [ ] Architecture: Define the shared incident registry between the patient portal and the execution-reporting module.
- [ ] Design: Define incident DTOs, categories, and states.
- [ ] Implementation: Implement the patient incident service and portal views.
- [ ] Testing: Tests for validation, ownership of linked items, emergency warning, and status tracking.

## Contracts and Interfaces
- `Client:` Patient portal (web and mobile) for incident form and incident list.
- `Server:` Patient incident service.
- `Incident registry interface:` Stores incidents shared with UC-PHY-05 categories.
- `Audit interface:` Records author, timestamp, and outcome.
- `Errors:` `INVALID_INCIDENT_DATA` and `ACCESS_DENIED` are returned explicitly; no error is treated as success.

## Acceptance Criteria
- [ ] The patient can submit an incident with category, date, and description.
- [ ] The incident can be linked only to the patient's own appointments or procedures.
- [ ] Selecting a clinical concern shows the emergency warning before submission.
- [ ] The patient receives a tracking identifier and can follow the incident status.
- [ ] Every submitted incident is attributed to the authenticated patient and audited.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../feature/CU00-initial-structure/DECISIONS.md).
- Operational assumption (pending ratification): patient incidents are reviewed by Admissions Staff, who forward clinical concerns to the responsible physician.

## Effort Estimation
- **Estimated effort without AI:** 5 hours
- **Estimated effort with AI (Copilot):** 3 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
