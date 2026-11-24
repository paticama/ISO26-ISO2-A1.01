---
name: Use case
about: One parent issue per use case (realized in exactly one iteration), with a server and a client sub-issue
title: 'UC-XXX-NN – Use Case Name (Cn vX.Y.Z)'
---

<!--
Title: Cn = titular module (C1–C10, see PLAN18-11.md §3); vX.Y.Z = version this use case gives that module.
Create two sub-issues of this issue:
  'UC-XXX-NN – server (Cn vX.Y.Z)'  domain entity, service in the module, REST controller in sescam-server-app, JUnit 5 + Mockito tests
  'UC-XXX-NN – client (Cn vX.Y.Z)'  view in sescam-client-app, REST client, form validations
Branches: feature/<module>-<version>-<XXX-NN>-server and feature/<module>-<version>-<XXX-NN>-client, PR into develop.
-->

## Use Case
UC-XXX-NN · FR-XXX-NN, FR-XXX-NN · Phase · Iteration N · `sescam-module` (Cn, vX.Y.Z) · Minutes of meetings
- **Priority:** High | Medium | Low (realizes FR-XXX-NN, FR-XXX-NN)
- **Assigned to:** person who owns the whole RADIT cycle of this use case
- **UML Relationships:**
  - `<<include>>` / `<<extend>>` Use Case Name [Condition: …] — or: No `<<include>>` or `<<extend>>` relationships apply.
  - Related Use Cases: UC-XXX-NN (reason), UC-XXX-NN (reason).

### Minutes (linked to wiki)
[Meeting #N minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-n-…)

## Objective
One or two sentences: what the primary actor achieves with this use case and why it matters.

## RADIT Disciplines
- [ ] Requirements
- [ ] Analysis
- [ ] Design
- [ ] Implementation
- [ ] Testing

## Components involved
- Titular module: `sescam-module` (Cn) – version goes to vX.Y.Z
- Other modules used (only through their interfaces in `sescam-common`): …
- `sescam-common` (interfaces/DTOs), `sescam-server-app` (endpoint), `sescam-client-app` (view)

## Contracts and Interfaces
Actor:
Primary actor

System:
Service name in `sescam-module` (Cn, vX.Y.Z)

Detailed Specification:
[`docs/use-cases/UC-XXX-NN-use-case-name.md`](https://github.com/paticama/ISO26-ISO2-A1.01/blob/develop/docs/use-cases/UC-XXX-NN-use-case-name.md)

## Acceptance Criteria
- Verifiable condition 1.
- Verifiable condition 2.

## Definition of Done
- [ ] **Analysis:** interaction flows and behavioural diagrams; entities placed in their module.
- [ ] **Design:** interfaces and DTOs in `sescam-common`; design diagrams committed.
- [ ] **Implementation:** server and client sub-issues closed; module passes `mvn verify`; modules not built yet covered by no-op implementations.
- [ ] **Testing:** one test per acceptance criterion, at least one per alternative flow, unit and integration tests (JUnit 5 + Mockito), JaCoCo ≥ 80% for the module.
- [ ] **Security and safety:** role check and audit entry on every clinical-data access; examination safety constraints checked.
- [ ] **Close:** RADIT boxes ticked here and in the spec, actual hours logged, PR approved by another teammate, module version bumped.

## Decisions (DECISIONS.md)
Refer to DECISIONS.md and DECISIONS18-11.md (topics affected by this use case).

## Effort Estimation
- **Estimated effort without AI:** X hours
- **Estimated effort with AI (Copilot):** X hours
- **Estimated effort with AI + 25% safety margin:** X hours
- **Actual effort:** X hours (fill in when closing)
