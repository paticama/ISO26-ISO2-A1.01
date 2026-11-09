# UC-AAD-03 — Manage Waitlist and Priority

## Phase Elaboration

- **Module:** AAD (Admissions, Appointments and Diagnostics)
- **Minutes:** Neutral
- **GitHub Issue:** Neutral
- **Primary actor:** Admissions Staff
- **Supporting actor:** System Administrator
- **Related decisions:** [CU00 initial structure decisions](../../feature/CU00-initial-structure/DECISIONS.md)

## Goal and scope

Admissions Staff adds eligible requests to a waitlist, reviews their order, and applies authorized priority changes. System Administrator configures priority categories and policy limits; the administrator does not make routine queue decisions.

## Client–Server flow

1. The client requests a waitlist view for a service, location, and date range.
2. Admissions Staff adds or updates a request.
3. The client sends a typed command with the reason for any priority change.
4. The server validates eligibility, policy, permissions, and queue invariants.
5. The server assigns a deterministic position or priority score and records an audit event.
6. The client displays the resulting position and any next action.

## Main flow

1. Admissions Staff selects an eligible examination request.
2. The server verifies that the request has no active appointment and that its patient and service are valid.
3. The request is inserted using configured priority, clinical urgency classification, request time, and tie-breaker rules.
4. Admissions Staff may request a priority change with a mandatory reason.
5. The server recalculates the affected ordering atomically and returns the new positions.
6. When a slot is released, the client can show the next eligible request for booking.

## Alternate and error flows

- An already booked or cancelled request returns `WAITLIST_NOT_ELIGIBLE`.
- An unsupported priority category returns `PRIORITY_INVALID`.
- A missing reason for a manual change returns `REASON_REQUIRED`.
- A policy limit violation returns `PRIORITY_POLICY_VIOLATION`.
- A stale queue version returns `CONFLICT`.
- Unauthorized priority changes return `FORBIDDEN`.
- Failure while reordering leaves the prior order unchanged and returns `SERVICE_UNAVAILABLE`.

## RADIT

### Requirements

- Add, remove, search, and inspect waitlist entries.
- Apply deterministic ordering and configured priority rules.
- Require authorization and reasons for manual priority changes.
- Preserve queue history and explain every order change.

### Analysis

Queue order is a derived projection of eligible requests and policy inputs. Manual overrides are exceptional, bounded, and auditable. Reordering must be atomic so two staff members cannot produce divergent positions.

### Design

The client uses `WaitlistQuery`, `AddWaitlistEntryCommand`, `ChangePriorityCommand`, and `RemoveWaitlistEntryCommand`. The server owns eligibility, ordering, policy evaluation, and audit records.

### Implementation

Implement queue projections, deterministic tie-breakers, policy validation, transactional reorder operations, optimistic versions, and reason capture. Provide a read model for operational lists.

### Testing

Test deterministic ordering, ties, priority changes, ineligible requests, policy boundaries, concurrent updates, audit completeness, and recovery after failed persistence.

### Deployment

Deploy waitlist and audit schema changes, rebuild the queue projection, and verify that the projection matches source requests before enabling manual priority operations.

## Contracts and interfaces

### Client contract

- `getWaitlist(query) -> WaitlistPage`
- `addToWaitlist(command) -> WaitlistEntry`
- `changePriority(id, command, version) -> WaitlistEntry`
- `removeFromWaitlist(id, reason, version) -> RemovalReceipt`

### Server contract

- Own eligibility and ordering decisions.
- Require reasons and permissions for manual priority changes.
- Return the new queue version and audit correlation identifier.

### Interfaces

Versioned HTTP/JSON routes live under `/api/aad/waitlist`. A read-only queue projection interface supports worklists. Errors use stable codes with field and policy details where applicable.

## Acceptance criteria

- An eligible request is placed in a deterministic position.
- An ineligible request cannot enter or remain in the waitlist.
- Manual priority changes require the correct permission and a non-empty reason.
- Two concurrent changes cannot silently lose one another.
- Every queue-order change is auditable and reconstructable.
- The client shows explicit policy and validation errors.
- System Administrator policy configuration cannot be performed through the Admissions Staff operation.

## Effort estimation

| Work item | Estimate |
|---|---:|
| Requirements and policy analysis | 5 h |
| Queue and audit design | 6 h |
| Client waitlist and priority views | 8 h |
| Server and projection implementation | 13 h |
| Automated and integration testing | 10 h |
| Deployment and projection verification | 4 h |
| **Total** | **46 h** |

