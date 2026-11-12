# UC-PHY-01 – Order and Prescribe Diagnostic Procedure (Physicians v0.1.0)

## Use Case
- **UC:** `UC-PHY-01`
- **RF:** `FR-PHY-01`, `FR-PHY-02`, `FR-PHY-03`
- **Phase:** Elaboration
- **Module:** `sescam-order-entry` (v0.1.0)
- **Priority:** High (ISO/IEC/IEEE 29148:2018)
- **UML Relationships:**
  - `<<include>>` Verify Patient Safety Constraints & Contrast Allergies
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#19](https://github.com/paticama/ISO26-ISO2-A1.01/issues/19)
- **Actor:** Physician
- **Secondary Actors:** Electronic Health Record (EHR / HCE), Administrative / Admissions System
- **Description:** Enables a physician to prescribe a radiological examination for an assigned patient from the system's catalogue of procedure types.
- **Preconditions:**
  1. The physician is authenticated in the system via corporate Single Sign-On (SSO).
  2. The target patient is registered in the system and selected from the EHR.
- **Postconditions:**
  1. A new diagnostic procedure order is recorded in the system with `Pending Scheduling` status.
  2. The order is queued in the central repository for Admissions slot allocation.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Client]** The physician selects the option to order a new diagnostic procedure for an assigned patient.
  2. **[Server]** The system retrieves the active Catalogue of Examination Types (modality, room, duration, preparation guidelines, contrast requirements, informed consent protocols, safety constraints) and returns DTOs to the client.
  3. **[Client]** The system renders the catalogue; the physician selects the examination type.
  4. **[Client]** The physician enters clinical indication, clinical justification, and assigns priority level (*Urgent/Stat*, *Preferential*, or *Routine*).
  5. **[Client]** The client performs form validation and submits the prescription payload (`CreateOrderDTO`) to the server.
  6. **[Server]** **`<<include>>` Verify Patient Safety Constraints & Contrast Allergies:** The server validates safety constraints, contrast requirements, and informed consent protocols against the patient's EHR profile.
  7. **[Server]** The server persists the order with `Pending Scheduling` status, logs an audit entry, and publishes an order notification for Admissions worklist ingestion.
  8. **[Client]** The system displays order confirmation with the generated tracking identifier.
- **Alternative / Exception Flows:**
  - **4a. Missing mandatory clinical data [Client]:** If indication, justification, or priority level is omitted, the client blocks submission and highlights missing fields.
  - **6a. Contrast medium and informed consent required [Server]:** The system automatically attaches the informed consent document and patient preparation guidelines to the order.
  - **6b. Safety constraint detected [Server -> Client]:** If a contraindication is detected (e.g., MRI with ferromagnetic implant or contrast allergy), the server flags a safety alert. The client displays the warning, requiring explicit clinical override justification or allowing cancellation.

## RADIT Disciplines
- [x] Requirements: Elicit and specify prescription workflow with safety constraint validation.
- [ ] Architecture: Model domain class diagram & sequence diagrams for order creation.
- [ ] Design: Define Java interfaces (`OrderEntryService`) and DTOs (`CreateOrderDTO`).
- [ ] Implementation: Implement service contract and REST controller in OpenJDK 17/21.
- [ ] Testing: Unit tests (JUnit 5 + Mockito) validating safety constraint & contrast rules.

## Contracts and Interfaces
- `es.uclm.esi.sescam.diagnostics.api.OrderEntryService`
- `CreateOrderDTO`, `OrderResponseDTO`

## Acceptance Criteria
- [ ] Specification approved in accordance with team template.
- [ ] Patient EHR safety contraindications and contrast consent verified.
- [ ] Validated orders recorded in `Pending Scheduling` status and routed to Admissions.
- [ ] 100% passage of automated unit tests.

## Decisions (DECISIONS.md)
- Complies with **ADR-001** (Unified Process), **ADR-003** (Interface-Driven Decoupling), and **ADR-007** (SSO/EHR Standards).

## Effort Estimation
- **Estimated effort without AI:** 8 hours
- **Estimated effort with AI (Copilot):** 3 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
