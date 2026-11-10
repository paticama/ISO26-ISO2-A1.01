# UC-PHY-03 – Manage Orders and Consult Clinical Execution Schedules (Physicians v0.3.0)

## Use Case
- **UC:** `UC-PHY-03`
- **RF:** `FR-PHY-06`, `FR-PHY-07`, `FR-PHY-08`
- **Phase:** Construction
- **Module:** `sescam-order-entry` (v0.3.0)
- **Priority:** Medium (ISO/IEC/IEEE 29148:2018)
- **UML Relationships:**
  - Extended by: Mandatory Order Cancellation Audit [Condition: If cancellation requested]
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#21](https://github.com/paticama/ISO26-ISO2-A1.01/issues/21)
- **Actor:** Physician
- **Secondary Actors:** Administrative / Admissions System
- **Description:** Allows the physician to track and manage the fulfillment status of their prescribed diagnostic orders and consult available clinical execution schedules.
- **Preconditions:**
  1. The physician is authenticated in the system via SSO.
- **Postconditions:**
  1. The physician consults updated order statuses and calendar schedule availability.
  2. If requested, an unexecuted pending order is updated or cancelled.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** The physician accesses the order tracking and schedule consultation view.
  2. **[Server]** The system retrieves and returns all diagnostic orders prescribed by the physician, indicating current statuses (`Ordered`, `Scheduled`, `Completed`, `Reported`, `Cancelled`).
  3. **[Client]** The physician applies filters (status, date range, patient) and selects a specific order to inspect its tracking history and assigned appointment slot.
  4. **[Client]** The physician requests to view clinical execution schedules.
  5. **[Server]** The system queries and returns calendar schedule availability across imaging modalities and technical examination rooms.
  6. **[Client]** Renders the schedule matrix and room availability calendars.
- **Alternative / Exception Flows:**
  - **3a. Modify or cancel pending order [Client -> Server]:** If a procedure has not yet been executed, the physician modifies its priority/clinical details or cancels the order. The server updates the record and alerts Admissions to free the scheduled slot.

## RADIT Disciplines
- [x] Requirements: Specify order status tracking, modality schedule queries, and order modification/cancellation.
- [ ] Architecture: Sequence diagram for status synchronization.
- [ ] Design: Query interfaces and DTOs for schedule calendars.
- [ ] Implementation: Filtering and cancellation service endpoints.
- [ ] Testing: Test unexecuted order cancellation and status transitions.

## Contracts and Interfaces
- `es.uclm.esi.sescam.orderentry.api.OrderTrackingService`
- `OrderFilterDTO`, `ExecutionScheduleDTO`

## Acceptance Criteria
- [ ] Executed orders cannot be cancelled or modified.
- [ ] Cancellation releases scheduled appointment slots in Admissions.

## Decisions (DECISIONS.md)
- Complies with **ADR-003** and **ADR-006**.

## Effort Estimation
- **Estimated effort without AI:** 6 hours
- **Estimated effort with AI (Copilot):** 2.5 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
