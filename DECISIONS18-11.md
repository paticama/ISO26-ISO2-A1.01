# DECISIONS – update of 18/11

> **Status: APPROVED** at the team meeting of 18/11/2026.
> **Parent document:** [`DECISIONS.md`](DECISIONS.md) (ADR-001 to ADR-012). This file adds ADR-013 to ADR-018 and says which parts of the earlier ADRs they change. Where the two disagree, **this file prevails**. Everything else in `DECISIONS.md` stays valid.
> The plan that applies these decisions is [`PLAN18-11.md`](PLAN18-11.md).

## Changes to earlier ADRs

| Earlier ADR | What changes | New ADR |
|-------------|--------------|---------|
| ADR-003 (Consequences), "Planning assumptions" | "Subsystem leads" no longer exist. Approval of `sescam-common` changes and module quality go to the **module owner** of each affected module | ADR-013 |
| ADR-004 (module table) | 10 domain modules instead of 8: new `sescam-patients` and `sescam-schedules`; room schedules move from catalogue to schedules | ADR-014 |
| ADR-012 (Rationale) | Patients no longer live in `sescam-admissions` | ADR-014 |
| ADR-010 (versions) | Domain modules start at 1.0.0 with their first use case; `sescam-common` goes up once per iteration like the apps | ADR-016 |
| ADR-010 (branches) | `feature/UC-<ACTOR>-<NN>-<short-name>` is replaced by one branch per slice with module and version in the name; new `release/itN` branches | ADR-017 |

ADR-001, 002, 005, 006, 007, 008 and 011 do not change.

## ADR-013: Work is assigned by use case, not by actor

**Decision:** We do not split work by actor and there are no "subsystem leads" per actor. Each person gets one or more use cases per iteration, chosen so that everyone works with several actors and modules, and nobody implements use cases of the actor they used to lead (Antonio ADM, Rafa AAD, Victoria PHY, Alba PAT).

Each module has a **module owner** as a support role: reviews the PRs that touch the module, checks that changes to its interfaces in `sescam-common` do not break other modules, and tags its releases. The owner does not design the interfaces of other people's use cases.

| Module owner | Modules |
|--------------|---------|
| Antonio | C1 security, C2 catalogue, C9 integration |
| Åsta | C3 schedules, C6 patients |
| Victoria | C4 orders, C7 diagnostics |
| Rafa | C5 admissions |
| Alba | C8 communication, C10 analytics |
| Patricia | `sescam-common`, `sescam-server-app`, `sescam-client-app` |

Coordinator (Patricia), secretary (Åsta) and Wiki (Victoria) roles stay as before.

**Rationale:** The professor rejected the split by actor at the 18/11 review. One use case touches several actors (e.g. an appointment is booked by admissions, consulted by the patient and attended by the physician), so splitting by actor scatters the same entity across people and modules. Each student also has to show they can analyse, design, implement and test.

**Consequences:** The allocation of use cases to people is in `PLAN18-11.md` §5. The SCM and SQA plans, the wiki and the existing issues must drop "subsystem lead" wording.

## ADR-014: Ten domain modules (C1–C10)

**Decision:** Use these domain modules. Each entity belongs to exactly one module; the others reach it only through that module's interface in `sescam-common`.

| Id | Module | Responsibility |
|----|--------|----------------|
| C1 | `sescam-security` | Accounts, login, roles (RBAC), auditing |
| C2 | `sescam-catalogue` | Examination types, protocols, durations, policies, triage, parameters |
| C3 | `sescam-schedules` | Room agendas, technician shifts, equipment availability |
| C4 | `sescam-orders` | Prescriptions, periodic follow-up orders |
| C5 | `sescam-admissions` | Booking, slot allocation, waitlists, daily worklist |
| C6 | `sescam-patients` | Patient demographic data and clinical history records |
| C7 | `sescam-diagnostics` | Procedure execution, reports, DICOM viewer integration, radiation dose |
| C8 | `sescam-communication` | Notifications (SMS/email), patient incidents, surveys |
| C9 | `sescam-integration` | External connectors (SSO, EHR, RIS/PACS, messaging) |
| C10 | `sescam-analytics` | Statistics, dashboards, metrics (read only) |

`sescam-parent`, `sescam-common`, `sescam-server-app` and `sescam-client-app` stay as in ADR-004.

Modules call each other only through interfaces in `sescam-common`. The clinical hand-offs (order → booking → execution → notification) may use events, e.g. orders publishes "order created" instead of calling admissions. Modules not built yet are replaced by no-op implementations (as in ADR-004).

**Rationale:** Patients and schedules are separate business areas. Taking them out of admissions removes the dependency cycle between orders and admissions (orders needs patients, which now live in `patients`) and stops admissions from carrying a third of the hours.

**Consequences:** UC-AAD-01 moves to `sescam-patients` and UC-AAD-02 to `sescam-schedules`. Room and modality operating hours (FR-ADM-04) now belong to `schedules` (to be confirmed, see "Still open"). `modulos.txt`, the component diagram, the wiki and the specs must be updated.

## ADR-015: Full RADIT ownership, server/client slices and Definition of Done

**Decision:** The person assigned to a use case owns its whole RADIT cycle:

- **R & A:** interaction flows and behavioural diagrams.
- **D:** public interfaces and DTOs in `sescam-common`.
- **I:** server slice (entity, service in its module, REST controller in `sescam-server-app`) and client slice (view in `sescam-client-app`, REST client, form validations).
- **T:** unit and integration tests with JUnit 5 + Mockito.

Peer review is mandatory: nobody merges into `develop` directly; another teammate checks that the PR builds cleanly and approves it.

Every issue carries the Definition of Done in `PLAN18-11.md` §7 (one test per acceptance criterion, at least one per alternative flow, JaCoCo ≥ 80%, role check and audit on clinical data, PR approved, version bumped, actual hours logged).

**Rationale:** Splitting by discipline (one person designs, another codes, another tests) creates bottlenecks and goes against UP, where every iteration delivers working software. The client-server architecture means every use case has a server and a client part.

**Consequences:** Completes ADR-009 and ADR-010; their review and coverage rules stay.

## ADR-016: Module versions

**Decision:** Every use case raises the version of **one** module, its titular module. A domain module gets **1.0.0** with its first use case and goes up one minor version with each later one (e.g. orders: PHY-01 1.0.0, PHY-02 1.1.0, PHY-03 1.2.0, PAT-04 1.3.0). Patch versions for fixes stay as in ADR-010.

`sescam-parent`, `sescam-common`, `sescam-server-app` and `sescam-client-app` go up once per iteration: 0.1.0 (It1), 0.2.0 (It2), 0.3.0 (It3), 0.4.0 (It4), 1.0.0 (N). Tag format stays `<module>-vX.Y.Z`.

**Rationale:** A use case delivered with tests is a complete, usable piece of its module. One module per use case keeps the version history easy to trace from the issue.

**Consequences:** Replaces the version table of `PLAN.md`; the new one is in `PLAN18-11.md` §5. The version in each use-case spec must be updated.

## ADR-017: Issues and branches per slice

**Decision:** Each use case has 1 parent issue and 2 sub-issues, with module and version in the title:

- `UC-PHY-01 – Order and prescribe diagnostic procedure (C4 v1.0.0)` (specification, pre/postconditions, acceptance criteria)
- `UC-PHY-01 – server (C4 v1.0.0)`
- `UC-PHY-01 – client (C4 v1.0.0)`

One feature branch per slice: `feature/<module>-<version>-<UC>-server|client` (e.g. `feature/orders-1.0.0-PHY-01-server`) → PR into `develop`. One release branch per iteration, `release/itN`, merged into `main` with the tags. Hotfix branches stay as in ADR-010. No direct commits to `main` or `develop`.

**Rationale:** The course asks for module and version in the branch name. Separate server and client slices give smaller PRs that are easier to review.

**Consequences:** Replaces the branch rule in ADR-010 and `PLAN.md`. The issue template gets a "Components involved" field.

## ADR-018: Delivery schedule and weekly cadence

**Decision:** Every iteration ends with a release merged into `main`, tags and an updated wiki. **11/12** feature freeze (no new use cases after It4), **14/12** final release `sescam-parent` 1.0.0, **15/12** only final check and submission. If we fall behind, we cut Low-priority use cases of It4 instead of pushing work into Transition.

Each week follows a fixed cadence (planning and analysis at the weekend, design Mon–Tue, team meeting Wed, implementation and tests Wed–Thu, release on Fri; see `PLAN18-11.md` §6). The week never stretches: an unfinished use case moves to the next iteration by a decision recorded in the minutes.

**Rationale:** The course grades the GitHub evidence continuously, so the repository must show progress every week, and nothing should be left for the last day.

**Consequences:** Dates of the iterations and the use case → iteration mapping do not change (ADR-001, ADR-002).

## Still open (analysis workshop)

1. Incidents: one entity shared by PAT-05 and PHY-05, or two entities whose categories live in catalogue.
2. Worklist: stored, or computed from confirmed bookings on each request.
3. Room and modality operating hours (FR-ADM-04, part of ADM-01): they now belong to `schedules`, so ADM-01 also touches that module. Confirm.

When decided, add them here as new ADRs and in the minutes.
