# UC-ADM-02 – Manage User Roles & Access Control (Administrators v0.2.0)

## Use Case
- **UC:** `UC-ADM-02`
- **RF:** `FR-ADM-07`, `FR-ADM-08`, `FR-ADM-09`, `FR-ADM-10`
- **Phase:** Elaboration
- **Module:** `sescam-system-admin` (v0.2.0)
- **Priority:** High
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#50](https://github.com/paticama/ISO26-ISO2-A1.01/issues/50)
- **Actor:** System administrator
- **Description:** Enables an authorized system administrator to assign and maintain user roles so access to system capabilities follows the permissions associated with each role.
- **Preconditions:**
  1. The administrator is authenticated and authorized to manage roles and permissions.
  2. The target user account exists.
- **Postconditions:**
  1. A valid, confirmed role assignment is saved and its effective result is shown.
  2. Invalid, unauthorized, or failed changes preserve the existing assignment.
- **Main Success Scenario:**
  1. The administrator locates a user account.
  2. The system displays the user's current role assignments and relevant account status.
  3. The administrator selects or changes a role assignment.
  4. The system validates the requested change against available roles and access-control rules.
  5. The administrator reviews and confirms the change.
  6. The system saves the assignment and confirms the effective result.
- **Alternative / Exception Flows:**
  - **A1. Unauthorized administrator:** The system rejects the request without changing the assignment.
  - **A2. Invalid role or access rule:** The system explains the issue and preserves the existing assignment.
  - **A3. Save failure:** The system reports the failure and does not claim that access changed.
  - **A4. Sensitive assignment:** Changes to an administrator's own privileges or other designated sensitive assignments require the applicable safeguards.

## RADIT Disciplines
- [x] Requirements
- [x] Analysis
- [ ] Design
- [ ] Implementation
- [ ] Testing

## Contracts and Interfaces
- **System:** Administrator Server.
- **Inputs:** Target user account, requested role assignment or change, and authenticated administrator identity.
- **Outputs:** Current role assignment, validation and save result, and confirmation of resulting access.

## Acceptance Criteria
- [ ] An authorized administrator can view a user's current role assignments.
- [ ] Available roles can be assigned or changed according to access-control rules.
- [ ] Invalid, disallowed, or unauthorized changes do not alter the existing assignment.
- [ ] Sensitive changes receive the applicable safeguards.
- [ ] Successful role changes are confirmed, attributable, and available for audit.

## Decisions
- See the [Decisions wiki](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Decisions).

## Effort Estimation
- **Estimated effort without AI:** 10 hours
- **Estimated effort with AI (Copilot):** 5 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
