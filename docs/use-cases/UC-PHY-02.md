# UC-PHY-02 – Order Periodic Follow-up Surveillance Procedure (Physicians v0.3.0)

## Use Case
- **UC:** `UC-PHY-02`
- **RF:** `FR-PHY-04`, `FR-PHY-05`
- **Phase:** Construction
- **Iteration:** 4
- **Module:** `sescam-order-entry` (v0.3.0)
- **Priority:** Medium (ISO/IEC/IEEE 29148:2018)
- **UML Relationships:**
  - `<<include>>` Verify Patient Safety Constraints & Contrast Allergies
  - `<<extend>>` UC-PHY-04 (Diagnostic Reporting) [Extension Point: FollowUpRecommendation]
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#20](https://github.com/paticama/ISO26-ISO2-A1.01/issues/20)
- **Actor:** Physician
- **Secondary Actors:** Administrative / Admissions System
- **Description:** Enables a physician to prescribe recurring follow-up diagnostic procedures with defined recurrence intervals, automatically generating future scheduling requirements.
- **Preconditions:**
  1. The physician is authenticated via SSO and has selected an active patient.
  2. The selected procedure type in the catalogue is marked as eligible for periodic follow-up surveillance.
- **Postconditions:**
  1. A periodic surveillance plan is registered in the system.
  2. Future scheduling requirements are automatically computed and queued in the administrative waitlist according to defined recurrence intervals.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** The physician selects the option to prescribe a periodic follow-up surveillance procedure.
  2. **[Server]** The system queries the catalogue and returns only examination types flagged as eligible for periodic surveillance.
  3. **[Client]** The physician selects the examination type and specifies the clinical surveillance protocol (e.g., pulmonary nodule surveillance, oncological restaging, periodic screening).
  4. **[Client]** The physician enters clinical indication, justification, priority level, and selects the recurrence interval (3, 6, or 12 months).
  5. **[Client -> Server]** The physician submits the surveillance plan (`PeriodicOrderDTO`).
  6. **[Server]** The system stores the surveillance master plan, calculates future trigger dates, and queues automated scheduling tickets for Admissions.
  7. **[Client]** Displays confirmation of the periodic surveillance schedule roadmap.
- **Alternative / Exception Flows:**
  - **3a. Ineligible procedure selected [Client]:** If an examination type not eligible for periodic surveillance is selected, the client disables recurrence options and redirects to standard single order entry (`UC-PHY-01`).

## RADIT Disciplines
- [x] Requirements: Specify surveillance recurrence intervals (3, 6, 12 months) and eligibility filtering.
- [ ] Architecture: Model surveillance protocol entity relationships.
- [ ] Design: Define recurrence calculation contracts and DTOs.
- [ ] Implementation: Service logic generating automated scheduling tickets.
- [ ] Testing: Tests for recurrence date calculation and ineligible procedure validation.

## Contracts and Interfaces
- `es.uclm.esi.sescam.orderentry.api.SurveillanceService`
- `PeriodicOrderDTO`, `SurveillancePlanDTO`

## Acceptance Criteria
- [ ] Ineligible examination types cannot be configured for recurrence.
- [ ] Future scheduling requirements accurately computed and queued.

## Decisions (DECISIONS.md)
- Complies with **ADR-001** and **ADR-003**.

## Effort Estimation
- **Estimated effort without AI:** 6 hours
- **Estimated effort with AI (Copilot):** 2.5 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
