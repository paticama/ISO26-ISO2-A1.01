# SESCAM Radiodiagnostics Project Plan

## Purpose and source

This document is the working plan for agents and team members contributing to the SESCAM Radiodiagnostics project for Software Engineering II. It consolidates the project-planning, requirements, architecture, configuration-management, and quality-assurance decisions in **Theoretical, Assignment Iteration 0**. The module structure approved on 13/11 replaces the per-actor modules (`sescam-patient-portal`, `sescam-order-entry`, `sescam-execution-reporting`, `sescam-administrative`, `sescam-system-admin`) still found in the source document, the Wiki, and some issues; if they conflict, this plan prevails. Dates and effort estimates below are the approved planning baseline from that document, except where this plan states otherwise; update this plan through the change-control process when the team approves a change.

Detailed use-case specifications, acceptance criteria, contracts, flows, and per-use-case estimates are maintained under [`docs/use-cases/`](docs/use-cases/). Each use case is tracked by one GitHub issue. The use-case diagrams are in Appendix A of the source document, which is not stored in the repository.

## Project baseline and objectives

- **Product:** a client-server system supporting radiodiagnostics workflows for SESCAM.
- **Lifecycle:** Unified Process (UP): Inception, Elaboration, Construction, and Transition.
- **Requirements baseline:** 77 functional requirements (FRs) and 23 use cases, approved at the end of Inception.
- **Scope:** patient, physician, admissions, and system-administration workflows; integration with corporate SSO, the EHR/HCE, patient-administration systems, and RIS/PACS.
- **Architectural baseline:** due at the end of Elaboration; includes the `sescam-parent` root Maven POM, `sescam-common` interfaces, and Elaboration use cases.
- **Product baseline:** due at Transition; target `sescam-parent` version is `1.0.0`, the version of the whole build.

The FRs are grouped by subsystem: Patient (19), Physician (20), Administrative/Admissions (12), and System Administrator (26). The 23 use cases are end-to-end actor goals, not one-use-case-per-requirement: a use case may realize several FRs, and an iteration may deliver several use cases. Each use case belongs to exactly one iteration.

## Prioritization and delivery strategy

Use the High/Medium/Low scale with the following criteria:

| Criterion | High | Medium / Low |
|---|---|---|
| Architectural significance and technical risk | Exercises a core boundary, validates a critical domain model, or removes significant technical uncertainty | Uses established CRUD or reporting mechanics |
| Clinical and stakeholder value | Essential to healthcare workflow or system viability | Secondary, optional, or postponable capability |
| Dependency and precedence | A prerequisite is delivered first and has equal or higher priority | Depends on an already established capability |

Priority determines the planned phase: **High → Elaboration**, **Medium → Construction**, and **Low → Iteration 4 (Late Construction)**. “Late Construction” is not a separate UP phase. Elaboration is deliberately used to exercise the architectural boundaries: SSO and role-based views, booking transactions, diagnostic reporting and DICOM images, EHR/admission/RIS-PACS integrations, audit logging, and procedure-execution data.

## Schedule and use-case assignments

Dates use day/month format. Iterations are planned as one-week increments, except the Inception window and the three-day Transition window shown below.

| Iteration | Dates | UP phase | Planned work |
|---|---|---|---|
| 0 | 13/10–13/11 | Inception | Requirements, use cases, and planning; establish the 77-FR/23-use-case baseline and approve the SCM and SQA plans |
| 1 | 14/11–20/11 | Elaboration | UC-ADM-01, UC-ADM-02, UC-ADM-03, UC-ADM-04, UC-AAD-01 |
| 2 | 21/11–27/11 | Elaboration | UC-PHY-01, UC-AAD-02, UC-AAD-05, UC-PHY-05, UC-PHY-04, UC-PAT-01 |
| 3 | 28/11–04/12 | Construction | UC-AAD-03, UC-AAD-04, UC-PHY-03, UC-PAT-02, UC-PAT-03, UC-PHY-02, UC-PAT-05 |
| 4 | 05/12–11/12 | Construction | UC-PAT-06, UC-PAT-04, UC-PHY-06, UC-AAD-06, UC-ADM-05 |
| N | 12/12–14/12 | Transition | Complete final integration and build of all modules under `sescam-parent`, clean up code, test, and deliver the final product |

### Use-case catalogue and module allocation

| ID | Use case | Maven module | Priority | Iteration |
|---|---|---|---|---:|
| UC-PAT-01 | Manage My Appointments | `sescam-admissions` | High | 2 |
| UC-PAT-02 | Consult My Diagnostic History and Reports | `sescam-diagnostics` | Medium | 3 |
| UC-PAT-03 | Notify Patient | `sescam-communication` | Medium | 3 |
| UC-PAT-04 | Consult My Follow-ups and Clinical Guidelines | `sescam-orders` | Low | 4 |
| UC-PAT-05 | Submit Incident Report | `sescam-communication` | Medium | 3 |
| UC-PAT-06 | Complete Satisfaction Survey | `sescam-communication` | Low | 4 |
| UC-PHY-01 | Order and Prescribe Diagnostic Procedure | `sescam-orders` | High | 2 |
| UC-PHY-02 | Order Periodic Follow-up Surveillance Procedure | `sescam-orders` | Medium | 3 |
| UC-PHY-03 | Manage Orders and Consult Clinical Execution Schedules | `sescam-orders` | Medium | 3 |
| UC-PHY-04 | Author, Edit, and Review Diagnostic Reports and Medical Images | `sescam-diagnostics` | High | 2 |
| UC-PHY-05 | Log Procedure Execution Details and Clinical/Operational Incidents | `sescam-diagnostics` | High | 2 |
| UC-PHY-06 | Consult Operational Metrics on Orders | `sescam-analytics` | Low | 4 |
| UC-AAD-01 | Manage Patient | `sescam-admissions` | High | 1 |
| UC-AAD-02 | Manage Schedules | `sescam-admissions` | High | 2 |
| UC-AAD-03 | Manage Waitlist and Priority | `sescam-admissions` | Medium | 3 |
| UC-AAD-04 | Manage Daily Worklist | `sescam-admissions` | Medium | 3 |
| UC-AAD-05 | Book Requested Examination | `sescam-admissions` | High | 2 |
| UC-AAD-06 | Generate Operational Reports | `sescam-analytics` | Low | 4 |
| UC-ADM-01 | Manage System Configurations and Parameters | `sescam-catalogue` | High | 1 |
| UC-ADM-02 | Manage User Roles and Access Control | `sescam-security` | High | 1 |
| UC-ADM-03 | Audit and Compliance Management | `sescam-security` | High | 1 |
| UC-ADM-04 | Manage External Integrations and Interoperability | `sescam-integration` | High | 1 |
| UC-ADM-05 | Generate Statistics and Usage Analytics | `sescam-analytics` | Low | 4 |

## Actors and external systems

- **Patient:** manages and consults appointments, diagnostic history, and follow-ups.
- **Physician:** inherits patient capabilities and prescribes procedures, records their execution, and authors diagnostic reports.
- **Admissions staff:** manages patients, schedules, waitlists, bookings, worklists, and operational reports.
- **System administrator:** manages configuration, access control, audit, integrations, and global statistics.
- **Timer:** triggers patient reminders and notifications.
- **External systems:** corporate SSO; hospital EHR/HCE; patient admission/administration system; RIS/PACS; and a messaging gateway for e-mail, SMS, and mobile push. The messaging gateway is a team assumption from the source plan.

## Architecture and component responsibilities

The system is a client-server application. Each component is a Maven submodule; server-side domain modules collaborate through explicit team-designed interfaces. Keep modules cohesive and loosely coupled, and place shared interfaces, DTOs and exceptions in `sescam-common`.

| Layer | Module | Responsibility |
|---|---|---|
| Build | `sescam-parent` | Root aggregator POM; its version is the version of the whole build |
| Contracts | `sescam-common` | Interfaces, DTOs and exceptions shared by client and server |
| Domain (server) | `sescam-security` | Login, users, roles, permissions, and audit log |
| Domain (server) | `sescam-catalogue` | Examination types, protocols, guidelines, policies, triage, parameters, and room schedules |
| Domain (server) | `sescam-admissions` | Patients, schedules and slots, waitlist, bookings, daily worklist, and patient appointments |
| Domain (server) | `sescam-orders` | Prescriptions, periodic follow-ups, and order status |
| Domain (server) | `sescam-diagnostics` | Procedure execution, clinical incidents, reports, and images |
| Domain (server) | `sescam-communication` | Notifications, reminders, patient incidents, and surveys |
| Domain (server) | `sescam-integration` | External adapters for EHR (HL7/FHIR), RIS/PACS (DICOM), admission system, and SSO |
| Domain (server) | `sescam-analytics` | Read-only statistics and reports |
| Application | `sescam-server-app` | Server startup, API, configuration, and database |
| Application | `sescam-client-app` | Single client application with role-specific views |

## Effort, capacity, and budget

Estimates are tentative. AI-assisted estimates include human review. Costs use effort buffered by 25% at €12.50/hour: 126.88 h with AI and 204 h × 1.25 = 255 h without AI.

| Iteration | Without AI (h) | With AI (h) | With AI + 25% (h) |
|---|---:|---:|---:|
| 1 | 48 | 24 | 30 |
| 2 | 61 | 29 | 36.25 |
| 3 | 57 | 29.5 | 36.88 |
| 4 | 38 | 19 | 23.75 |
| **Iterations 1–4 total** | **204** | **101.5** | **126.88** |

The team has six members and assumes up to 7 hours per member per week (42 team-hours/week); actual capacity varies with other course work. Iteration 0 took approximately 80 hours, versus an initial 160-hour theoretical estimate. The planning rate is **€12.50/hour**, based on the €1,000 Iteration 0 cost and actual hours.

| Cost item | With AI (25% buffered) | Without AI (25% buffered) |
|---|---:|---:|
| Iteration 0 | €1,000 | €1,000 |
| Iterations 1–4 | €1,586 | €3,187.50 |
| Transition (N) | €2,000 | €2,000 |
| Tools and licences | €180 | €0 |
| **Total** | **€4,766** | **€6,187.50** |

The €2,000 Transition cost is fixed by the assignment statement, not derived from hours × rate. The €180 AI-license estimate assumes €10/month per user for six users; the project spans three billing months (October, November and December). The team has GitHub Copilot through its academic access; this line estimates a comparable paid licence.

## Release and version plan

Follow SemVer 2.0.0. For domain modules, increment a module's minor version once per delivered use case; increment the patch for fixes that do not change interfaces. `sescam-common`'s minor version increases when its interfaces change, at most once per iteration. `sescam-parent`, `sescam-server-app`, and `sescam-client-app` carry the overall build version. Tags are repository-wide and use `<module>-vX.Y.Z` (for example, `sescam-security-v0.2.0`). At each iteration release and the Transition release, merge `develop` into `main` through a reviewed PR and create the version tags shown in the table on `main`; tags are created only at these releases. The final product target is `1.0.0` for `sescam-parent`, the version of the whole build, tagged `sescam-parent-v1.0.0`.

| Module | Iteration 1 (20/11) | Iteration 2 (27/11) | Iteration 3 (04/12) | Iteration 4 (11/12) | Transition (14/12) |
|---|---|---|---|---|---|
| `sescam-parent` | `sescam-parent-v0.1.0` (build version) | `sescam-parent-v0.2.0` (build version) | `sescam-parent-v0.3.0` (build version) | `sescam-parent-v0.4.0` (build version) | `sescam-parent-v1.0.0` (build version) |
| `sescam-server-app` | `sescam-server-app-v0.1.0` (build version) | `sescam-server-app-v0.2.0` (build version) | `sescam-server-app-v0.3.0` (build version) | `sescam-server-app-v0.4.0` (build version) | `sescam-server-app-v1.0.0` (build version) |
| `sescam-client-app` | `sescam-client-app-v0.1.0` (build version) | `sescam-client-app-v0.2.0` (build version) | `sescam-client-app-v0.3.0` (build version) | `sescam-client-app-v0.4.0` (build version) | `sescam-client-app-v1.0.0` (build version) |
| `sescam-security` | `sescam-security-v0.2.0` (UC-ADM-02, UC-ADM-03) | — | — | — | — |
| `sescam-catalogue` | `sescam-catalogue-v0.1.0` (UC-ADM-01) | — | — | — | — |
| `sescam-integration` | `sescam-integration-v0.1.0` (UC-ADM-04) | — | — | — | — |
| `sescam-admissions` | `sescam-admissions-v0.1.0` (UC-AAD-01) | `sescam-admissions-v0.4.0` (UC-AAD-02, UC-AAD-05, UC-PAT-01) | `sescam-admissions-v0.6.0` (UC-AAD-03, UC-AAD-04) | — | — |
| `sescam-orders` | — | `sescam-orders-v0.1.0` (UC-PHY-01) | `sescam-orders-v0.3.0` (UC-PHY-03, UC-PHY-02) | `sescam-orders-v0.4.0` (UC-PAT-04) | — |
| `sescam-diagnostics` | — | `sescam-diagnostics-v0.2.0` (UC-PHY-05, UC-PHY-04) | `sescam-diagnostics-v0.3.0` (UC-PAT-02) | — | — |
| `sescam-communication` | — | — | `sescam-communication-v0.2.0` (UC-PAT-03, UC-PAT-05) | `sescam-communication-v0.3.0` (UC-PAT-06) | — |
| `sescam-analytics` | — | — | — | `sescam-analytics-v0.3.0` (UC-PHY-06, UC-AAD-06, UC-ADM-05) | — |

The table shows each module's version at the end of each iteration; the version reached by each individual use case is stated in its specification in [`docs/use-cases/`](docs/use-cases/). `sescam-common` is tagged `sescam-common-vX.Y.Z` at an iteration release only when its interfaces changed in that iteration; its versions are not planned in advance.

## Team responsibilities

| Responsibility | Member |
|---|---|
| Coordinator; planning and repository administration | Patricia Camacho Rivallo |
| Secretary; decisions and meeting minutes | Åsta Emilie Bergum |
| System Administrator subsystem lead | Antonio Natán Fechete Carpea |
| Patient subsystem lead | Alba de la Mata Domínguez |
| Physician subsystem and Wiki lead | Victoria Rodríguez Álamo |
| Administrative subsystem lead | Rafael Camilo Rubio Quintero |

Subsystem leads own their modules, interfaces, and releases. The coordinator maintains this plan. The secretary records decisions and meeting minutes. A PR must be approved by a team member other than its author; AI cannot approve PRs.

## Configuration management and change control

- Track requirements, use-case specifications, code, POMs, meeting minutes, `DECISIONS.md`, Wiki pages, and AI configuration as configuration items.
- Use Git Flow (`main`, `develop`, feature, and hotfix branches); protect `main` and `develop`. Do not commit directly to `main`; integrate through peer-reviewed pull requests.
- Create one branch per use case from `develop`, named `feature/UC-<ACTOR>-<NN>-<short-name>`; create hotfix branches from `main`, named `hotfix/<short-name>`.
- Feature PRs target `develop`; hotfix PRs target `main` and are then merged back into `develop`. PRs reference the issue (for example, `Closes #N`).
- At each iteration release and the Transition release, merge `develop` into `main` through a reviewed PR and create the version tags shown in the Release and version plan on `main`; tags are created only at these releases.
- Build the executable system with Maven. Use Semantic Versioning and tags as specified above.
- Raise each use case as a GitHub issue using the project template and track it on the Kanban board. Keep requirements, issues, specifications, decisions, and meeting minutes traceable.
- Propose and review requirement or use-case changes at weekly team meetings; approve them there and record the decision in the minutes.
- Perform a configuration audit at each iteration release. Review the SCM plan after Iterations 2 and 4; maintain it through the coordinator and review it at the end of each UP phase.
- External systems are outside the team's control and outside the SCM plan.

### SCM milestones

| Date | Milestone |
|---|---|
| 13/11 | Iteration 0 ends; approve 77-FR/23-use-case requirements baseline and SCM/SQA plans |
| Every week | Review and approve change requests at the team meeting; record decisions in minutes |
| 20/11 | Iteration 1 release of security, catalogue, integration, and admissions; configuration audit |
| 27/11 | Iteration 2 release of admissions, orders, and diagnostics; approve architectural baseline (root POM, `sescam-common` interfaces, Elaboration use cases); audit and SCM-plan review |
| 04/12 | Iteration 3 release of admissions, orders, diagnostics, and communication; configuration audit |
| 11/12 | Iteration 4 release of orders, communication, and analytics; configuration audit and SCM-plan review |
| 12/12–14/12 | Transition release: final integration and build of all modules under `sescam-parent`; merge `develop` into `main` through a reviewed PR, create the Transition version tags, perform the final configuration audit, and deliver on 14/12 |

## Quality assurance and acceptance targets

Apply Quality by Design: define quality through the requirements from the start, not only at final testing. The source plan uses ISO/IEC 25010:2023 for the quality model and ISO/IEC 25040 for evaluation. Quality weights are Security 25%, Safety 30%, Functional suitability 20%, Compatibility 10%, Maintainability 10%, and Flexibility 5%.

| Characteristic | Measure | Target |
|---|---|---:|
| Security | Clinical-data access is recorded in the audit log | 100% |
| Security | Functions protected by roles | 100% |
| Safety | Examination safety constraints are checked | 100% |
| Functional suitability | Acceptance tests pass during an iteration / at release | ≥80% / 100% |
| Compatibility | Integration tests pass | ≥80% |
| Maintainability | Unit-test line coverage, measured with JaCoCo (Maven plugin) | ≥80% |
| Flexibility | Supported client device categories (desktop, mobile, web) via one responsive client | 3 out of 3 |

All PRs receive human peer review. Review use cases before team approval. Before release, the module owner verifies acceptance criteria, passing tests, and reviewed code. Evaluate at the end of each UP stage except Inception using the ISO/IEC 25040 sequence: establish requirements, specify and design the evaluation, execute it, and conclude it. Use the PDCA cycle (Plan, Do, Check, Act) for improvement. The coordinator maintains the SQA plan; subsystem leads are responsible for module quality; all human members review and approve PRs.

### Product constraints to preserve

- Healthcare interoperability uses DICOM, HL7, and FHIR; support progressive integration with SSO, EHR/HCE, patient-administration systems, and RIS/PACS.
- Authenticate through corporate SSO and enforce role-based access to functions and data.
- Check examination safety constraints, contrast-medium requirements, and informed-consent protocols.
- Record every clinical-data access and every diagnostic-report or order-status change with user, action, timestamp, and affected patient. Audit records must not be modifiable or deletable through the system; the retention period remains to be agreed with SESCAM.
- External-channel notifications must not disclose diagnostic findings or other clinical content; they may announce appointments, preparation instructions, or report availability and direct patients to the authenticated portal.
- Use a modular, multi-tier, client-server design with explicit interfaces and individual Maven submodules; follow SOLID principles.
- Support heterogeneous clients: Windows/Linux/macOS desktop, Android/iOS/iPadOS mobile, and web access with DICOM-compliant image viewing. A single responsive web client in `sescam-client-app` covers all three device categories.
- Maintain at least 80% unit-test line coverage per component; use JaCoCo (Maven plugin) to measure and verify it before merging each PR.
- Core development is Java, with Eclipse and Visual Paradigm as primary development environments. Use the repository Wiki and AI assistance as specified by the course.
- Human engineers direct decisions and audit AI-generated deliverables; human team members retain accountability for the system.

## Tools and assumptions

**Resources:** Git, GitHub, Maven, JUnit, JaCoCo (Maven plugin), GitHub Copilot, PlantUML, and Visual Paradigm; the plan assumes free or academic access. GitHub Copilot is used for implementation and coding assistance; its model is assigned automatically rather than selecting one fixed model.

**Planning assumptions:** six team members; approximately 7 hours per person per week; one-week iterations; GitHub Copilot use; no dependency on another team's work. The client is SESCAM. Estimates are tentative and should be revisited as actual effort becomes available.

## Working rules for contributors and agents

- Create one feature branch per use case from `develop`, using `feature/UC-<ACTOR>-<NN>-<short-name>`.
- Link the use-case issue in the PR (for example, `Closes #N`).
- Run `mvn verify` and check JaCoCo coverage is at least 80% before opening a PR.
- Do not change `sescam-common` interfaces without approval from the affected subsystem leads.
- Never commit to `main` or `develop`.
- Create version tags on `main` only at the iteration and Transition releases shown in the Release and version plan.
- External notifications must not contain clinical content.
- AI cannot approve PRs.
