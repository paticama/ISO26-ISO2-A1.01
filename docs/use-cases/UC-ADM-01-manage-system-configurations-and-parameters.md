# UC-ADM-01 – Manage System Configurations & Parameters (Administrators v0.1.0)

## Use Case
- **UC:** `UC-ADM-01`
- **RF:** `FR-ADM-01`, `FR-ADM-02`, `FR-ADM-03`, `FR-ADM-04`, `FR-ADM-05`, `FR-ADM-06`
- **Phase:** Elaboration
- **Iteration:** 1
- **Module:** `sescam-system-admin` (v0.1.0)
- **Priority:** High
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#44](https://github.com/paticama/ISO26-ISO2-A1.01/issues/44)
- **Actor:** System administrator
- **Description:** Enables an authorized system administrator to review and update system-wide configuration parameters and see whether changes take effect immediately or require a restart or reload.
- **Preconditions:**
  1. The administrator is authenticated.
  2. The administrator is authorized to manage system configuration.
- **Postconditions:**
  1. A valid, confirmed configuration change is saved and its activation status is reported.
  2. Invalid, unauthorized, or failed changes leave the saved configuration unchanged.
- **Main Success Scenario:**
  1. The administrator opens the system configuration area.
  2. The system displays available configuration parameters and their current values.
  3. The administrator selects a parameter and enters a new value.
  4. The system validates the value and explains any validation errors.
  5. The administrator reviews and confirms the change.
  6. The system saves the change and reports whether it took effect immediately or requires a restart or reload.
- **Alternative / Exception Flows:**
  - **A1. Unauthorized administrator:** The system denies the change and does not expose an unauthorized save action.
  - **A2. Invalid value:** The system identifies the affected parameter and leaves its saved value unchanged.
  - **A3. Save failure:** The system reports the failure and makes clear that the previous value remains active.
  - **A4. Concurrent change:** The system detects a newer value and prompts the administrator to reload or review the conflict rather than silently overwriting it.

## RADIT Disciplines
- [x] Requirements
- [x] Analysis
- [ ] Design
- [ ] Implementation
- [ ] Testing

## Contracts and Interfaces
- **System:** Administrator Server.
- **Inputs:** Configuration parameter identifier, proposed value, and authenticated administrator identity.
- **Outputs:** Current and updated values, validation or save result, and whether a change is active or pending.

## Acceptance Criteria
- [ ] An authorized administrator can view current system configuration parameters.
- [ ] Valid parameter changes can be saved and their activation status is communicated.
- [ ] Invalid values and unauthorized changes are rejected without modifying saved configuration.
- [ ] Concurrent changes are not silently overwritten.
- [ ] Changes are attributable to the administrator and available for audit.

## Decisions
- See the [Decisions wiki](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Decisions).

## Effort Estimation
- **Estimated effort without AI:** 8 hours
- **Estimated effort with AI (Copilot):** 4 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
