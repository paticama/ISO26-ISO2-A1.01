# DECISIONS.md

> **Update of 18/11/2026:** ADR-013 to ADR-018 (work by use case instead of by actor, module owners, 10 domain modules C1–C10, RADIT ownership, new versions, branches and delivery dates) are in [`DECISIONS18-11.md`](DECISIONS18-11.md), which prevails where the two disagree.

This is the project's consolidated Architecture Decision Log. It summarizes the development and design decisions in **Theoretical, Assignment Iteration 0**, with current module names aligned to the approved structure recorded in [`PLAN.md`](PLAN.md). The source document is not stored in the repository. Use-case-specific flows and acceptance criteria remain in [`docs/use-cases/`](docs/use-cases/); the project schedule, estimates, and release plan are in `PLAN.md`.

Some use-case specifications reference the ADR numbers below. A change to these decisions must be proposed and approved at a weekly team meeting, then recorded in the meeting minutes and this log.

## ADR-001: Unified Process lifecycle

**Decision:** Use the Unified Process (UP), with Inception, Elaboration, Construction, and Transition. Inception establishes the requirements, use-case, and planning baseline; Elaboration validates the architecture; Construction delivers the remaining use cases; Transition integrates, tests, cleans up, and delivers the product.

**Rationale:** An iterative lifecycle supports incremental delivery while using high-priority use cases to validate the system's architectural boundaries before completing lower-priority workflows.

**Consequences:** Each use case is assigned to exactly one iteration, while an iteration may deliver several use cases. The approved dates, iteration assignments, and milestones are maintained in `PLAN.md`.

## ADR-002: Requirements, use-case granularity, and prioritization

**Decision:** Maintain 77 functional requirements (FRs) and 23 end-to-end use cases. Do not equate one requirement with one use case or one iteration. A use case represents an observable clinical or operational goal, may realize several FRs, and groups related operations on the same business object. Track each use case as one GitHub issue with a detailed specification, and deliver it in one iteration. A use case takes the highest priority among its functional requirements.

Prioritize using architectural significance and technical risk, clinical/stakeholder value, and dependency/precedence. ISO/IEC/IEEE 29148:2018 is used as the reference for the High/Medium/Low scale. High-priority use cases are delivered in Elaboration, Medium in Construction, and Low in Iteration 4 (Late Construction).

**Rationale:** This preserves cohesive, valuable workflows and incremental delivery, rather than splitting requirements into small units that cannot deliver standalone value.

**Consequences:** The FR baseline is grouped into Patient (19), Physician (20), Administrative/Admissions (12), and System Administrator (26). The use-case catalogue and iteration assignments are in `PLAN.md`.

## ADR-003: Interface-driven decoupling

**Decision:** Use explicit, team-designed interfaces between client and server components. Keep components cohesive and loosely coupled, follow SOLID principles, and place shared interfaces, DTOs, and exceptions in `sescam-common`. Domain components depend only on `sescam-common`, never on each other; `sescam-server-app` wires the implementations. Interfaces to external systems are defined in `sescam-common`, implemented by `sescam-integration`, and used by domain components only through those interfaces.

**Rationale:** Explicit contracts allow modules and client/server components to evolve independently while preserving integration boundaries.

**Consequences:** Domain modules depend on shared contracts rather than one another's implementation details. Changes to `sescam-common` interfaces require approval from the affected subsystem leads.

## ADR-004: Client-server and Maven module boundaries

**Decision:** Build a multi-tier client-server system as a Maven multi-module project. Use the approved domain-oriented module structure; it supersedes the older per-actor module names still present in the source document, Wiki, or issues. See ADR-012 for the structure decision and rationale.

| Layer | Module | Responsibility |
|---|---|---|
| Build | `sescam-parent` | Root aggregator POM; its version is the version of the whole build |
| Contracts | `sescam-common` | Shared interfaces, DTOs, and exceptions |
| Domain (server) | `sescam-security` | Login, users, roles, permissions, and audit log |
| Domain (server) | `sescam-catalogue` | Examination types, protocols, guidelines, policies, triage, parameters, and room schedules |
| Domain (server) | `sescam-admissions` | Patients, schedules and slots, waitlist, bookings, daily worklist, and patient appointments |
| Domain (server) | `sescam-orders` | Prescriptions, periodic follow-ups, and order status |
| Domain (server) | `sescam-diagnostics` | Procedure execution, clinical incidents, reports, and images |
| Domain (server) | `sescam-communication` | Notifications and reminders, patient incidents, and surveys |
| Domain (server) | `sescam-integration` | Adapters for EHR (HL7/FHIR), RIS/PACS (DICOM), admission systems, and SSO |
| Domain (server) | `sescam-analytics` | Read-only statistics and reports |
| Application | `sescam-server-app` | Server startup, API, configuration, and database |
| Application | `sescam-client-app` | Single client application with role-specific views |

A component is created with its first use case, not before; until then, other components use a no-op implementation of its `sescam-common` interface.

**Rationale:** Domain-oriented modules give the system cohesive boundaries, make dependencies explicit, and support independent module releases.

**Consequences:** `sescam-parent` owns the root build version; the final whole-build target is `1.0.0`. Build and package the executable system with Maven.

## ADR-005: Actors and client access

**Decision:** Support four primary actors: Patient, Physician, Admissions Staff, and System Administrator. Physicians inherit patient capabilities and add procedure ordering, execution recording, and diagnostic-report authoring. The system Timer triggers notifications. External secondary actors are corporate SSO, the hospital EHR/HCE, the patient admission/administration system, RIS/PACS, and a messaging gateway.

Use one client application with views appropriate to each role. A single responsive web client in `sescam-client-app` covers desktop, mobile, and web device categories; web access includes DICOM-compliant image viewing.

**Rationale:** A shared client preserves a consistent user experience while role-specific views expose the appropriate workflows. The messaging gateway is a team assumption; it supports e-mail, SMS, and mobile push.

**Consequences:** Authenticate users through corporate SSO and authorize access by role. Support Windows, Linux, and macOS desktop access and Android, iOS, and iPadOS mobile access, as well as web access.

## ADR-006: Order, scheduling, and execution workflows

**Decision:** Model prescriptions, scheduling, bookings, execution, and reporting as related but distinct domain responsibilities. Physicians prescribe diagnostic procedures and may schedule periodic follow-ups; generated follow-ups create future scheduling requests. Admissions books physician-ordered examinations into available slots and manages rescheduling and cancellation. Procedure execution is recorded before reports and operational data depend on it.

**Rationale:** This reflects the clinical and operational sequence and keeps order management, admissions scheduling, and diagnostic execution/reporting within their respective domain modules.

**Consequences:** Preserve requirement dependencies and workflow state across module interfaces. Capture cancellation reasons and audit changes where required; slot release on cancellation is specified in UC-PAT-01, UC-PHY-03, and UC-AAD-05.

## ADR-007: Healthcare interoperability standards

**Decision:** Use the established external systems and standards: corporate Single Sign-On (SSO) for authentication; HL7 and FHIR for EHR/HCE data exchange; DICOM for RIS/PACS interoperability and medical images.

**Rationale:** The system must integrate progressively with hospital systems using healthcare interoperability standards.

**Consequences:** Keep external adapters in `sescam-integration`. Monitor integration endpoint health, and keep external systems outside the team's configuration-management scope.

## ADR-008: Clinical safety, privacy, and auditability

**Decision:** Enforce role-based access to system functions and data. Define and check examination safety constraints, contrast-medium requirements, and informed-consent protocols. Record clinical-data access and changes to diagnostic reports and order status with the user, action, timestamp, and affected patient. Audit records cannot be modified or deleted through the system. Notifications sent through external channels must not include diagnostic findings or other clinical content.

**Rationale:** Clinical data is sensitive, and errors in examination safety or unauthorized or untraceable changes can affect patient care and compliance.

**Consequences:** Audit access and relevant changes consistently. Retain audit records for the period agreed with SESCAM; that retention period remains to be determined. External notifications may announce appointments, preparation instructions, or report availability and direct patients to the authenticated portal.

## ADR-009: Quality by Design and verification targets

**Decision:** Define quality from the start using ISO/IEC 25010:2023 and evaluate it using ISO/IEC 25040. Prioritize Security (25%), Safety (30%), Functional Suitability (20%), Compatibility (10%), Maintainability (10%), and Flexibility (5%). Evaluate at the end of each UP stage except Inception.

| Characteristic | Measure | Target |
|---|---|---:|
| Security | Clinical-data access logged | 100% |
| Security | Functions protected by roles | 100% |
| Safety | Examination safety constraints checked | 100% |
| Functional suitability | Acceptance tests pass during an iteration / at release | ≥80% / 100% |
| Compatibility | Integration tests pass | ≥80% |
| Maintainability | Unit-test line coverage per component | ≥80% |
| Flexibility | Desktop, mobile, and web access | 3 out of 3 |

**Rationale:** Quality is treated as meeting requirements and being reliable, not as a final-stage check.

**Consequences:** Human reviewers inspect every PR. Before release, the module owner checks acceptance criteria, tests, and review status. Use JUnit for automated tests and JaCoCo (Maven plugin) to measure and verify unit-test line coverage before merging a PR. Use the PDCA (Plan, Do, Check, Act) cycle for improvement.

## ADR-010: Configuration, releases, and human/AI responsibilities

**Decision:** Use Git Flow with protected `main` and `develop` branches. Create one feature branch per use case from `develop`, named `feature/UC-<ACTOR>-<NN>-<short-name>`; create hotfix branches from `main`, named `hotfix/<short-name>`. Feature PRs target `develop`; hotfix PRs target `main` and are merged back into `develop`. At each iteration release, merge `develop` into `main` through a reviewed PR.

Follow SemVer 2.0.0: increment a module's minor version once per delivered use case and its patch for fixes that do not change interfaces. Use repository-wide unique tags in the format `<module>-vX.Y.Z`; create the release-table version tags on `main` only at iteration releases. `sescam-common`'s minor version increases when its interfaces change, at most once per iteration. `sescam-parent`, `sescam-server-app`, and `sescam-client-app` carry the overall build version: `0.1.0`, `0.2.0`, `0.3.0`, and `0.4.0` after Iterations 1 to 4, and `1.0.0` in Iteration N. `sescam-parent` carries the version of the whole build, with a final target and tag of `1.0.0` and `sescam-parent-v1.0.0`.

Use GitHub issues and the Kanban board to track use cases. Reference the issue in its PR and record approved requirement/use-case changes and other decisions in meeting minutes. Configuration items include code, POMs, requirements, use-case specifications, meeting minutes, this decision log, Wiki pages, and AI configuration. Conduct configuration audits at iteration releases; review the SCM plan at the end of UP phases and after Iterations 2 and 4.

**Rationale:** Protected branches, peer review, release tags, and traceability preserve reproducible builds and an auditable development history.

**Consequences:** Never commit directly to `main`; all PRs are reviewed by a human team member who is not the author. Engineers design the interfaces and AI implements the contracts; no AI-generated code is accepted without human verification. Humans direct design decisions, audit AI-generated deliverables, and retain accountability. GitHub Copilot assists with implementation; its model is assigned automatically rather than fixed by the team. Core development is Java, with Eclipse and Visual Paradigm as the primary development environments. The project uses Maven, GitHub, PlantUML, and the repository Wiki.

## ADR-011: Priority adjustments by team decision

**Decision:**

- **UC-ADM-04:** `High (Raised by team decision: SSO, EHR and RIS/PACS integration is the highest technical risk and a dependency of every High use case, Criteria 1 and 3)`. It is delivered in Iteration 1.
- **UC-PAT-02:** `Medium by team decision; FR-PAT-11 to FR-PAT-13 were aligned to Medium so the use case matches the rule that a use case takes the highest priority among its functional requirements.` It is delivered in Iteration 3.

**Rationale:** These adjustments apply the approved prioritization criteria and highest-FR-priority rule to the integration dependencies and patient report-history requirements.

**Consequences:** UC-ADM-04 and UC-PAT-02 retain the priorities and iteration assignments recorded in their specifications and `PLAN.md`.

## ADR-012: Domain-oriented module structure

**Decision:** Replace the per-actor modules (`sescam-patient-portal`, `sescam-order-entry`, `sescam-execution-reporting`, `sescam-administrative`, `sescam-system-admin`) with the domain-oriented structure in ADR-004; `sescam-analytics` keeps its name.

**Rationale:** The domain components follow the core processes named in the course specification: order entry, appointment scheduling, procedure execution and diagnostic reporting, plus analytics. They are supported by catalogue, security, communication, and integration components. Patient management belongs to `sescam-admissions` because hospital Admission, Discharge, Transfer owns patient registration and discharge; schedules, waitlist, bookings, and worklist are part of the same workflow. `sescam-integration` isolates the highest-risk external protocols (DICOM, HL7, FHIR) and lets the rest of the system be tested against simulated external systems. `sescam-common` holds all interfaces in one place: “The Engineer designs the Interfaces; the AI implements the Contracts.”

**Consequences:** The use-case to module mapping and versions in `PLAN.md` follow this structure; old module names in the source document, Wiki, or issues are obsolete.

## Planning assumptions and references

- The team consists of six students, with an estimated maximum of seven work hours per person per week; actual capacity varies with other course work.
- Iteration 0 took approximately 80 hours; its initial theoretical estimate was 160 hours. The planning rate is €12.50/hour.
- Use-case effort estimates are tentative; AI-assisted estimates include human review. Iterations 1–4 total 204 hours without AI and 101.5 hours with AI, or 126.88 hours with the 25% AI contingency. Cost comparisons and the Transition cost fixed by the assignment are maintained in `PLAN.md`.
- The team has academic GitHub Copilot access; the plan estimates a comparable licence at €10 per user per month.
- The client is SESCAM. The team does not depend on another team's work.
- Each subsystem lead is responsible for their module, interfaces, and releases; the coordinator maintains the project and quality plans; the secretary records decisions and meeting minutes.
- The requirements baseline contains 77 FRs and 23 use cases. Their complete scope, priorities, schedule, module allocations, versions, and acceptance criteria are maintained in `PLAN.md` and the individual specifications under `docs/use-cases/`.
