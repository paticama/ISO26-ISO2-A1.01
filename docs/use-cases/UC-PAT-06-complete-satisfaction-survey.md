# UC-PAT-06 – Complete Satisfaction Survey (sescam-communication v0.3.0)

## Use Case
- **UC:** `UC-PAT-06`
- **RF:** `FR-PAT-15`
- **Phase:** Late Construction
- **Iteration:** 4
- **Module:** `sescam-communication` (v0.3.0)
- **Priority:** Low (ISO/IEC/IEEE 29148:2018 / Inherited from FR-PAT-15)
- **UML Relationships:**
  - Related Use Cases: UC-PHY-05 marks the procedure as executed, which makes the survey available; UC-AAD-06 and UC-ADM-05 consume the aggregated results.
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#104](https://github.com/paticama/ISO26-ISO2-A1.01/issues/104)
- **Actor:** Patient
- **Secondary Actors:** None
- **Description:** Enables a patient to answer a satisfaction survey about a procedure they attended, so that the service can evaluate and improve the quality of care.
- **Preconditions:**
  1. The patient is authenticated via corporate Single Sign-On (SSO).
  2. The patient has at least one executed procedure whose survey has not been answered and is within the configured answer window.
- **Postconditions:**
  1. The answers are stored linked to the procedure, and the survey is marked as `Answered`.
  2. The answers are available to the statistics use cases only in aggregated form.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** The patient opens the *Surveys* section of the patient portal.
  2. **[Server]** The system lists the executed procedures of the patient with a pending survey.
  3. **[Client]** The patient selects a procedure.
  4. **[Server]** The system returns the configured questionnaire (rating questions and an optional comment).
  5. **[Client]** The patient answers the questions; the client checks the required answers and submits the survey.
  6. **[Server]** The server validates the answers, stores them linked to the procedure, and marks the survey as `Answered`.
  7. **[Client]** The system displays a thank-you confirmation.
- **Alternative / Exception Flows:**
  - **A1. No pending surveys [Server -> Client]:** The system informs the patient that there are no surveys to answer.
  - **A2. Required answers missing [Client]:** Submission is blocked and the unanswered questions are highlighted.
  - **A3. Survey already answered [Server -> Client]:** The second submission is rejected and the patient is informed.
  - **A4. Answer window expired [Server -> Client]:** The survey is no longer available and the patient is informed.

## RADIT Disciplines
- [x] Requirements: Specify survey availability, questionnaire, validation, and single submission.
- [ ] Architecture: Define the boundary between survey storage and the analytics modules.
- [ ] Design: Define questionnaire and answer DTOs and the aggregation contract.
- [ ] Implementation: Implement the patient survey service and portal views.
- [ ] Testing: Tests for availability window, required answers, duplicate prevention, and aggregated-only access.

## Contracts and Interfaces
- `Client:` Patient portal (web and mobile) for survey list and questionnaire.
- `Server:` Patient survey service.
- `Procedure interface:` Reads executed procedures (UC-PHY-05).
- `Analytics interface:` Exposes aggregated results to UC-AAD-06 and UC-ADM-05.
- `Errors:` `SURVEY_NOT_AVAILABLE`, `SURVEY_ALREADY_SUBMITTED`, and `INVALID_SURVEY_ANSWERS` are returned explicitly; no error is treated as success.

## Acceptance Criteria
- [ ] Surveys are available only for the patient's own executed procedures and within the answer window.
- [ ] Required answers are enforced before submission.
- [ ] Each survey can be submitted only once.
- [ ] Statistics use cases receive only aggregated results, without identifying the patient.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../feature/CU00-initial-structure/DECISIONS.md).
- Operational assumption: the questionnaire and the answer window are configurable parameters (UC-ADM-01).

## Effort Estimation
- **Estimated effort without AI:** 6 hours
- **Estimated effort with AI (Copilot):** 3.5 hours
- **Estimated effort with AI + 25% safety margin:** 4.38 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
