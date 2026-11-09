# UC-AAD-04 — Manage Daily Worklist

## Phase Elaboration

- **Module:** AAD (Admissions, Appointments and Diagnostics)
- **Minutes:** Neutral
- **GitHub Issue:** Neutral
- **Primary actor:** Admissions Staff
- **Supporting actor:** System Administrator
- **Related decisions:** [CU00 initial structure decisions](../../feature/CU00-initial-structure/DECISIONS.md)

## Goal and scope

Admissions Staff views and manages the daily operational worklist: arrivals, check-in state, missing information, cancellations, and hand-off status. System Administrator configures statuses and permissions but does not manage the daily queue.

## Client–Server flow

1. The client requests the worklist for a date, location, and actor scope.
2. The server builds the list from appointments, waitlist decisions, and operational status.
3. Admissions Staff opens an item and submits a status transition or note.
4. The server checks authorization, transition rules, and current version.
5. The server commits the transition and returns the updated item.
6. The client refreshes affected counters and displays explicit errors.

## Main flow

1. Admissions Staff selects the operational day and location.
2. The client displays appointments grouped by status and sorted by configured operational order.
3. Staff marks an arrival, identifies missing information, records a hand-off, or marks a cancellation.
4. The server verifies that the transition is allowed and that required fields are present.
5. The server records actor, timestamp, reason where required, and the resulting status.
6. The worklist and summary counters refresh from the committed state.

## Alternate and error flows

- An appointment outside the actor scope returns `FORBIDDEN`.
- An invalid transition returns `WORKLIST_TRANSITION_INVALID`.
- A required reason or missing data returns `REASON_REQUIRED` or `WORKLIST_DATA_REQUIRED`.
- A stale item version returns `CONFLICT`.
- A cancelled or completed appointment cannot be moved backward without an explicitly configured recovery permission.
- A service failure returns `SERVICE_UNAVAILABLE` and leaves the prior status intact.

## RADIT

### Requirements

- Show a date- and location-scoped daily worklist.
- Support explicit operational status transitions and required reasons.
- Provide filtering, sorting, counters, and refresh after mutations.
- Preserve an auditable timeline for each item.

### Analysis

The worklist is a read model over appointments and operational events. Status transitions are a state machine, not free-form field edits. The server remains the source of truth while the client may optimistically refresh only after confirmation.

### Design

The client uses `DailyWorklistQuery` and `WorklistTransitionCommand`. The server exposes a worklist projection, transition policy, event log, and counter summary interface.

### Implementation

Implement scoped queries, status-state validation, required reason rules, version checks, event persistence, and client filters. Keep read projection rebuilds safe and observable.

### Testing

Test daily boundaries, location scope, every allowed and rejected transition, required fields, concurrent updates, counter accuracy, projection refresh, and audit chronology.

### Deployment

Deploy status configuration and event schema, build the daily projection, and verify counts against source appointments for representative operating days.

## Contracts and interfaces

### Client contract

- `getDailyWorklist(query) -> DailyWorklist`
- `transitionWorklistItem(id, command, version) -> WorklistItem`
- `getWorklistSummary(query) -> WorklistSummary`
- `getWorklistTimeline(id) -> WorklistEvent[]`

### Server contract

- Enforce scope, state transitions, required reasons, and optimistic concurrency.
- Return committed status and updated counters.
- Keep event history append-only.

### Interfaces

Versioned HTTP/JSON routes live under `/api/aad/worklist`. The summary interface is derived from the same committed projection as the item list and includes a correlation identifier.

## Acceptance criteria

- Admissions Staff sees only the worklist scope granted to them.
- Every supported transition succeeds only from an allowed prior state.
- Invalid transitions and missing reasons return explicit stable errors.
- Counters match the committed worklist after a successful transition.
- Concurrent updates cannot silently overwrite a newer state.
- Timeline entries identify actor, time, prior state, new state, and reason.
- System Administrator configuration remains separate from daily operational actions.

## Effort estimation

| Work item | Estimate |
|---|---:|
| Requirements and state analysis | 4 h |
| Projection and state-machine design | 6 h |
| Client worklist implementation | 9 h |
| Server and event implementation | 12 h |
| Automated and integration testing | 10 h |
| Deployment and projection verification | 4 h |
| **Total** | **45 h** |

