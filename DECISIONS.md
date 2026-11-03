# Project decisions

This document records the decisions and constraints that currently govern the
project. It distinguishes course requirements, explicit team choices, and
material that is still proposed or unresolved. A statement in the requirements
draft is not automatically an approved architectural decision: its status is
identified below wherever that distinction matters.

## Status and source conventions

- **Required by the course specification** means the course PDF makes the
  direction or process mandatory.
- **Selected project decision** means the project has explicitly chosen the
  option, including choices made by the user.
- **Requirement in the requirements draft** means the requirement is recorded
  in `Requirements-and-Use-Cases.md`; it remains subject to that document's
  review and approval process and is not, by itself, an additional design
  decision in this file.
- **Proposed / pending** means the draft identifies an option or plan for
  discussion, or necessary project-specific information has not been supplied.
  It must not be treated as approved until the team records that approval.

Source shorthand used below:

- **Course specification**: `enunciadoPracticaENGv1.15.3.pdf`, cited by page.
- **Requirements draft**: `Requirements-and-Use-Cases.md`, cited by section or
  requirement ID.
- **User choice**: an explicit project choice supplied by the user.

## Decision 1 — Use the Unified Process as the development lifecycle

**Status:** Required by the course specification.

**Decision.** The project follows the Unified Software Development Process
(UP). Development is use-case driven, iterative and incremental, and
architecture-centric. These three properties jointly govern planning,
decomposition, implementation, and evolution of the system; none is a reason
to postpone architecture or stakeholder validation until the end.

**Planning heuristic, not a design law.** For initial planning, the course
specification advises the team to use `1 functional requirement : 1 use case`
and `1 use case : 1 iteration` as simplifying 1:1 mappings. They provide a
starting structure for relating requirements, use cases, and iteration plans.
They do **not** require that the final design contain one distinct use case per
requirement, that each use case can never be refined or combined in delivery,
or that a single iteration can address only one use case. In particular, the
1:1 mappings must not be applied mechanically in a way that turns UP into a
linear pseudo-waterfall. The design must still produce cohesive, loosely
coupled components and allow architecture and components to evolve through
iterations.

The identifiers and 1:1 examples in `Requirements-and-Use-Cases.md` §1.1 are
the current draft's traceability convention. Whether its iteration grouping
and sequence are adopted is a separate planning decision; §7 is explicitly a
proposal for discussion.

**Iteration 0 and Iteration N baseline.** The course's simplified planning
baseline includes an initial Iteration 0 in Inception for initial project
planning and a concluding Iteration N in Transition for wrap-up, including
system integration, integration testing, deployment packaging, and user
manual preparation. The PDF gives initial-budgeting assumptions of 1,000 in
human labor for Iteration 0 and 2,000 in human labor for Iteration N. These
are the PDF's baseline assumptions only: the figures are not a project
approved budget, do not specify a currency, and exclude tooling and model
licensing fees. The PDF recommends estimating the empirical Iteration 0 cost
in detail.

**Rationale and consequences.** Use cases provide the behavioral thread for
the work; iterations provide incremental delivery; and the architecture
provides the constraints under which increments are built. Planning must
connect these three views without treating their initial 1:1 mapping as an
immutable system decomposition. Estimates and release milestones must be
derived from the team's actual approved scope and resources rather than
inferred from the heuristic.

**Sources:** Course specification, pp. 3, 7–8; `Requirements-and-Use-Cases.md`,
§§1.1, 1.3, 7, and 8.

## Decision 2 — Keep engineering decisions and accountability human-led

**Status:** Required by the course specification; GitHub Copilot is the
selected assistive tool under Decision 6.

**Decision.** The project explicitly adopts a **human-in-the-loop
(man-in-the-loop)** working model. Human engineers retain decision authority
and accountability throughout the lifecycle. AI, including GitHub Copilot,
is an assistive instrument: it may propose alternatives and help execute
bounded implementation work, but it does not own the requirements, design,
approval, or delivery outcome.

The following work remains under human direction and responsibility:

- eliciting stakeholder needs and reconciling the system against those needs;
- prioritizing requirements and use cases;
- analyzing and designing use cases;
- planning scope, iterations, estimates, milestones, and workload;
- making and justifying architectural, interface, and technology decisions;
- designing unit and integration tests and their acceptance intent;
- reviewing, verifying, auditing, and approving implementation and test
  results; and
- accepting responsibility for the delivered system and its engineering
  artifacts.

The AI may assist with bounded boilerplate implementation, coding that
conforms to an approved contract and architectural context, proposing design
alternatives for human consideration, auditing student-designed interfaces,
and executing a test suite. These activities do not transfer the underlying
engineering decisions or accountability to the AI. AI output is a candidate
artifact, not evidence that a requirement has been satisfied or permission
to change an approved contract.

**Project workflow.** Each AI-assisted implementation task follows this
sequence:

1. A human authors or reviews and approves the requirement and its use case,
   including the intended behavior and relevant stakeholder constraints.
2. A human designs and approves the module/interface contract and supplies
   the applicable architecture, dependency, and scope constraints.
3. AI assistance is limited to the assigned scope and must conform to that
   contract and context. If the task cannot be completed within them, the
   unresolved decision is returned to a human rather than silently invented.
4. A human reviews the generated changes and test results, audits them against
   the contract and stakeholder requirements, and decides whether to approve,
   revise, or reject them.
5. The human engineer remains accountable for the approved result, regardless
   of how much of its implementation was generated or executed by AI.

This workflow makes the specification's role separation operational for this
project; it does not assert that the PDF mandates a particular prompt format,
agent product, or approval automation. Project schedules must account for
human review, verification, and approval latency. Agent availability or
parallel execution does not remove the human review-capacity constraint.

**Rationale and consequences.** AI can accelerate implementation, but that
increases rather than removes the need for inspection, quality auditing, and
reconciliation with stakeholder requirements. Work is not considered
complete solely because an agent produced code or a test command finished.

**Sources:** Course specification, pp. 3–4; `Requirements-and-Use-Cases.md`,
§§1.3 and 8.

## Decision 3 — Humans design and own explicit interfaces and contracts

**Status:** Required design direction in the course specification and
explicitly adopted as the project's interface-oriented working principle.

**Decision.** The project is **oriented to interfaces**. Human engineers
design, justify, and own the interfaces/contracts that define collaboration
between modules. Module implementations must conform to those contracts, and
inter-module collaboration takes place across explicit interface boundaries,
not through dependencies on another module's internal implementation.

For AI-assisted work, the governing rule is the specification's formulation:
**“The Engineer designs the Interfaces; the AI implements the Contracts.”**
AI may audit an interface designed by a student, but this permission is not
authority to replace or expand the approved interface. If an implementation
appears to require a contract change, a human must decide and record that
change before implementation proceeds on the changed basis.

The architecture should minimize coupling and maintain cohesive modules.
Layered or multi-tier organization is appropriate where it supports those
goals. The course PDF gives Model–View–Controller as an example; it does not
make MVC mandatory. Its comparison of interface-driven and concrete
class-oriented programming is guidance to understand the distinction, not a
decision that every type must be an interface or that a particular class
hierarchy must be used. Interface and architectural trade-offs must be
justified.

**Scope and non-decisions.** This principle does not select specific
interfaces, Java packages, classes, methods, data-transfer objects, endpoint
shapes, dependency-injection mechanisms, or a particular layered pattern.
Those are design decisions to be made from approved requirements and
documented contracts. It also does not prescribe one module per functional
requirement or one module per use case.

**Rationale and consequences.** Explicit boundaries make independent,
incremental component work possible and constrain AI-generated changes to a
reviewable contract. They require the team to design interfaces deliberately,
manage dependencies explicitly, and justify changes that cross or alter a
boundary rather than allowing implementation details to become accidental
contracts.

**Sources:** Course specification, pp. 7–8; `Requirements-and-Use-Cases.md`,
§§1.4 and 6 (NFR-10, currently a draft requirement).

## Decision 4 — Use Java and a root multi-module Maven project

**Status:** Java and Maven are explicit project choices; Java and Maven
component structure are also specified by the course.

**Decision.** The project uses Java for core development and Maven for its
build and component organization. The repository build is organized around a
root multi-module Maven project. Each approved cohesive software component is
represented by an individual Maven submodule. Dependencies between
submodules are declared explicitly, and components collaborate through the
human-designed interfaces in Decision 3.

The design follows the course's SOLID guidance and favors cohesive,
loosely-coupled components that can be developed incrementally and
consolidated in the target build. The course cautions against a monolithic,
all-at-once development approach. Maven's multi-module structure is the
mechanism specified for managing component builds; it is not permission to
derive a component boundary mechanically from a requirement identifier.

**Scope and non-decisions.** This decision does not establish a
`1 functional requirement = 1 Maven submodule` rule. Requirements, use cases,
iterations, and software components are distinct concepts. It does not
approve any specific server or client module names, the requirement-to-module
allocation shown in the draft matrix, a Java or Maven version, a framework,
or a testing library.

`Requirements-and-Use-Cases.md` §5 contains a proposed traceability matrix. It
refers to ADR-005 for existing server modules and labels some server and
client modules as new proposals. Those labels and mappings are inputs for
review, not ratification by this decision. The exact module list, boundaries,
and ownership remain pending; this document does not infer a module count
from the draft.

**Rationale and consequences.** A root multi-module build provides a common
build structure while keeping component boundaries visible. The team must
settle and justify module boundaries, contract ownership, and dependency
direction before treating the draft mapping as implementation authority.

**Sources:** User choice; Course specification, pp. 7–8; `Requirements-and-Use-Cases.md`,
§§1.4, 5, and 6 (NFR-10, currently a draft requirement).

## Decision 5 — Use a client-server system boundary

**Status:** Required by the course specification and recorded as NFR-01 in the
requirements draft.

**Decision.** The system has a client-server architecture with the following
boundary:

- The **server** owns business logic, persistence, and integrations with
  external hospital systems.
- **Clients** interact with the server through its versioned REST API.
- Clients do not access the server database directly and do not communicate
  directly with external hospital systems as a substitute for the server
  boundary.
- The target client categories in the requirements draft are web, desktop,
  and mobile. The draft further names target operating systems and a
  DICOM-compliant viewer; those details remain requirements to be validated,
  not a selection of client implementation technologies in this decision.

This boundary applies consistently to patient, physician, administrative,
and system-administration clients. A client-side screen or input check does
not move business logic, persistence, or an external-system integration out
of the server.

**Scope and non-decisions.** This decision does not choose REST endpoints,
resource schemas, serialization formats, API framework, client frameworks,
deployment topology, or a specific server technology. The requirements draft
also records TLS 1.3, SSO-issued tokens and role checks, audit logging,
concurrency, availability, interoperability, performance, accessibility,
and test coverage (NFR-03 through NFR-10). Those statements must be reviewed
and managed as requirements; listing them here does not turn them into
additional architecture choices or resolve draft placeholders such as
availability and response-time targets.

**Rationale and consequences.** The single API boundary keeps persistence
and external integrations under server ownership and gives heterogeneous
clients one stated means of interaction. Client and server work therefore
depends on explicit API contracts, and project planning must include both
sides of the interaction without creating direct client-to-database or
client-to-hospital-system paths.

**Sources:** Course specification, p. 1; `Requirements-and-Use-Cases.md`,
§§1.4 and 6 (NFR-01 and NFR-02; NFR-03 through NFR-10 remain draft
requirements).

## Decision 6 — Use GitHub Copilot as an assistive development tool

**Status:** Selected by the user; consistent with the course specification.

**Decision.** The project uses GitHub Copilot to assist development, subject
to the human-in-the-loop and interface-contract decisions above. Copilot is
not the project decision-maker and does not change the team's responsibility
for requirements, architecture, approved technology, review, or delivery.

The course specification identifies Visual Studio Code with the GitHub
Copilot Chat extension as its recommended reference environment, while
allowing alternative toolchains. The user's selection is Copilot; it does
not make Visual Studio Code mandatory. The choice of editor, model,
extensions, and any additional agent harness is not settled by this decision
unless separately agreed.

**Rationale and consequences.** The tool selection permits AI-assisted
implementation within the project's engineering controls. Generated
alternatives and code must remain bounded by human-approved requirements,
contracts, and architecture, and must undergo human review as described in
Decision 2.

**Sources:** User choice; Course specification, pp. 3–4.

## Decision 7 — Maintain traceability through meeting records and GitHub

**Status:** Required by the course specification.

**Decision.** The project's decision and delivery workflow uses the following
records and links:

- Formal written minutes are kept for every collaborative meeting.
- Each decision and each agreed deliverable maps to a GitHub Issue.
- Operational progress is tracked on a GitHub Projects Kanban board.
- Planning deliverables, supporting project records, meeting minutes,
  use-case specifications, analysis models, Markdown descriptors, Visual
  Paradigm project files, and effort-tracking spreadsheets are linked from
  the repository wiki.
- Issues generated for project work follow the specification's structured
  content expectations: objectives, relevant RADIT engineering disciplines,
  involved contracts and interfaces, acceptance criteria (definition of
  done), and effort estimates in hours comparing execution with and without
  agent assistance.

These are process requirements, not claims that a particular meeting has
occurred, that an issue or milestone already exists, or that a URL has been
approved. No issue numbers, meeting dates, or links are invented here.

**Rationale and consequences.** These links keep decisions, commitments,
planning artifacts, and delivery progress traceable to one another. When a
decision or deliverable is agreed, it must be reflected in the relevant
meeting record and issue workflow; progress and supporting artifacts must
remain discoverable through the specified project records and wiki.

**Sources:** Course specification, pp. 5–6; `Requirements-and-Use-Cases.md`,
§8.

## Decision 8 — Treat project estimates and releases as planning work, not
known facts

**Status:** Planning obligations are required by the course specification;
project-specific values are pending.

The course requires the team to derive a total project cost, a project
schedule, a definitive completion date, a structured software release
calendar, and workload allocation. The analysis must consider the effects of
AI assistance on operating costs and schedule, including tooling and model
licensing costs and the time needed for human review, verification, and
approval. The PDF defines a human consultant work week as 40 billable hours.
Its observation that agents may operate without the same labor-hour
constraints does not remove the review and approval bottleneck.

The course's Iteration 0 and Iteration N cost figures in Decision 1 are only
initial budgeting assumptions. They do not establish this project's total
budget. The current source documents do not provide approved project-wide
costs, a complete schedule, a definitive deadline, or confirmed resource
allocations. No such values can be inferred from this file.

The specification also expects component evolution to be planned through
explicit target releases and SemVer, with changelogs and release contents
charted as part of incremental development. This records a planning
expectation; it does not choose version numbers, release dates, component
names, or the iteration in which a particular component is released.

**Sources:** Course specification, pp. 3, 5, and 7–8; `Requirements-and-Use-Cases.md`,
§§7–8.

## Pending decisions and draft material

The following points remain open. Their presence in the draft is not approval:

1. **Module boundaries and allocation.** `Requirements-and-Use-Cases.md` §5
   proposes a requirement-to-server-module matrix and identifies additional
   client modules and some server modules as new proposals. The team must
   review the exact module list, boundaries, interface ownership, dependency
   direction, and requirement allocation before implementation relies on
   them. This document does not assert a module count or ratify any proposed
   name.
2. **Iteration plan.** The outline in §7 is explicitly proposed for
   discussion. Confirm or revise its grouping, ordering, and scope against
   the dependencies and architecture. The UP 1:1 mappings are initial
   planning heuristics, not an automatic approval of that outline.
3. **Project estimates and staffing.** The project-specific budget, schedule,
   release calendar, completion date, workload allocation, and comparison of
   AI-assisted and non-AI-assisted effort remain to be established. The
   course's initial Iteration 0 and N assumptions do not fill in those
   project-specific values.
4. **Requirements-draft review.** `Requirements-and-Use-Cases.md` is a
   consolidated draft marked for review. Its priorities, dependency graph,
   client details, NFRs, suggested targets, and technical allocations must
   be treated according to their stated draft or open status. For example,
   NFR-06 and NFR-08 explicitly leave targets to be agreed; examples such as
   99.5% availability and under two seconds are not approved targets merely
   because they appear in the draft.
5. **Architecture details.** Specific Java packages, classes, interfaces,
   API resources, frameworks, database choices, client technologies,
   deployment arrangements, and detailed contract contents are not selected
   by this document. They require separate, justified decisions based on
   reviewed requirements and human-designed interfaces.
6. **Project records.** Meeting minutes, GitHub Issues, a Projects Kanban
   board, wiki links, release records, and effort estimates must be created
   and maintained through the prescribed workflow. This document records
   those obligations without fabricating records or links.
