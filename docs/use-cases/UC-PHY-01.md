# UC-PHY-01 – Order and Prescribe Diagnostic Procedure (C4 v1.0.0)

## Use Case
- **UC:** `UC-PHY-01`
- **RF:** `FR-PHY-01`, `FR-PHY-02`, `FR-PHY-03`
- **Phase:** Elaboration
- **Iteration:** 2
- **Module:** `sescam-orders` (C4, v1.0.0; v1.0.1 audits the order before storing it; v1.0.2 records `ORDER_CREATION_FAILED` if storing fails after the audit)
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
- [x] Analysis: [activity diagram](../diagrams/UC-PHY-01/uc-phy-01-activity.puml) (main flow + 4a, 6a, 6b), [analysis class diagram](../diagrams/UC-PHY-01/uc-phy-01-class-analysis.puml) (boundary/control/entity, entity per module), [domain class diagram](../diagrams/puml/phy-class-domain.puml) and communication diagrams ([normal](../diagrams/puml/comm-phy-01-normal.puml), [4a](../diagrams/puml/comm-phy-01-alt4a.puml), [6a](../diagrams/puml/comm-phy-01-alt6a.puml), [6b](../diagrams/puml/comm-phy-01-alt6b.puml)).
- [x] Design: interfaces and DTOs in `sescam-common` (see Contracts below); server [design class diagram](../diagrams/UC-PHY-01/uc-phy-01-class-design.puml) and [sequence diagram](../diagrams/UC-PHY-01/uc-phy-01-sequence-design.puml); client [design class diagram](../diagrams/UC-PHY-01/uc-phy-01-class-design-client.puml) and [sequence diagram](../diagrams/UC-PHY-01/uc-phy-01-sequence-design-client.puml).
- [x] Implementation: server slice (`sescam-orders` 1.0.0, `OrderRestController`, `OrderExceptionHandler`, `OrderWiringConfig`) and client slice (`OrderViewController`, `OrderForm`, `OrderApiClient`, `orders/new.html`). Stand-ins until the modules exist: exam catalogue (C2), EHR safety gateway (C9), order subscriber (C5).
- [x] Testing: JUnit 5 + Mockito. `sescam-orders` 26 tests (service per flow: main, 4a, 6a, 6b alert, 6b override, denied role, unknown exam, audit failure), `sescam-server-app` `OrderRestControllerTest` 7 (MockMvc), `sescam-client-app` 22 (form, REST client, view per flow). JaCoCo ≥ 80% in the three modules.

## Contracts and Interfaces
- `com.sescam.radiodiagnostics.common.orders.OrderService` (C4; was `OrderEntryService`, renamed to the module interface of `PLAN18-11.md` §3) with `createOrder(CreateOrderDTO) : OrderResponseDTO`
- DTOs and enums in `common.orders`: `CreateOrderDTO`, `OrderResponseDTO`, `ConsentAndPreparationDTO`, `SafetyConstraintDTO`, `OrderCreatedEvent`, `PriorityLevel`, `OrderStatus`, `ConstraintType`
- Exceptions: `InvalidOrderDataException` (400), `UnauthorizedOrderAccessException` (403), `SafetyAlertException` (422, A6b)
- Outbound ports: `PatientSafetyGateway` (EHR constraints, implemented by C9), `OrderEventPublisher` (order → admissions), `AccessControlService.canOrderProcedures`, `AuditService`
- Needed from C2 (`common.catalogue`, pending owner approval): `ExamTypeService`, `ExamTypeDTO`, `ModalityType`, `ExamTypeNotFoundException`
- REST API (server-app): `GET /api/v1/orders/exam-types`, `POST /api/v1/orders` (201; 400, 403, 404, 422)
- Analysis and design diagrams: [`docs/diagrams/UC-PHY-01/`](../diagrams/UC-PHY-01/)

## Acceptance Criteria
- [ ] Specification approved in accordance with team template.
- [x] Patient EHR safety contraindications and contrast consent verified (`SafetyRules`, 6a/6b tests; EHR read through `PatientSafetyGateway`, no-op until C9 provides it).
- [ ] Validated orders recorded in `Pending Scheduling` status and routed to Admissions. *Recorded: done. Routed: `OrderCreatedEvent` is published, but nothing consumes it until C5 (AAD-05) subscribes.*
- [x] 100% passage of automated unit tests.

## Decisions (DECISIONS.md)
- Complies with **ADR-001** (Unified Process), **ADR-003** (Interface-Driven Decoupling), and **ADR-007** (SSO/EHR Standards).

## Effort Estimation
- **Estimated effort without AI:** 8 hours
- **Estimated effort with AI (Copilot):** 3 hours
- **Estimated effort with AI + 25% safety margin:** 3.75 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
