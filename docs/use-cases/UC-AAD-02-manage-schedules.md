# UC-AAD-02 — Manage Schedules

## Phase Elaboration

- **Module:** AAD (Admissions, Appointments and Diagnostics)
- **Minutes:** Neutral
- **GitHub Issue:** Neutral
- **Primary actor:** Admissions Staff
- **Supporting actor:** System Administrator
- **Related decisions:** [CU00 initial structure decisions](../../feature/CU00-initial-structure/DECISIONS.md)

## Goal and scope

Admissions Staff creates and maintains examination schedules, availability windows, capacity, and booking rules. System Administrator defines the services, locations, permissions, and exceptional configuration available to the scheduling process; the administrator does not replace operational schedule ownership.

## Client–Server flow

1. The client displays schedules filtered by service, location, date, and status.
2. Admissions Staff submits a schedule command.
3. The client performs format checks and sends a typed request.
4. The server authorizes the actor, validates time zones, capacity, overlaps, and dependencies.
5. The server commits the change atomically and returns the schedule version.
6. The client refreshes affected views and reports any explicit business error.

## Main flow

1. Admissions Staff selects a service, location, date range, operating hours, and capacity.
2. The server validates that the service and location are active and that the interval is valid.
3. The server rejects overlaps or capacity values outside configured limits.
4. The schedule is created or updated with version, status, audit metadata, and booking constraints.
5. Admissions Staff publishes the schedule; only published slots are available to booking flows.
6. A permitted closure marks future availability unavailable while preserving existing appointments for review.

## Alternate and error flows

- Overlapping intervals return `SCHEDULE_OVERLAP`.
- Invalid or ambiguous time-zone data returns `SCHEDULE_TIME_INVALID`.
- Capacity below existing bookings returns `CAPACITY_BELOW_BOOKINGS`.
- Inactive service or location returns `REFERENCE_INACTIVE`.
- A stale schedule version returns `CONFLICT`.
- Publication with no usable slots returns `SCHEDULE_EMPTY`.
- Permission failures return `FORBIDDEN`; persistence failures return `SERVICE_UNAVAILABLE`.

## RADIT

### Requirements

- Create, edit, publish, close, and search schedules.
- Validate operating intervals, capacity, service, location, and time zone.
- Prevent overlaps and preserve historical schedule versions.
- Keep Admissions Staff operations separate from System Administrator configuration.

### Analysis

Schedules are versioned operational resources. Availability is derived from published intervals minus reservations and closures. All overlap and capacity checks are server-side and transactional.

### Design

The client uses `ScheduleQuery`, `CreateScheduleCommand`, `UpdateScheduleCommand`, `PublishScheduleCommand`, and `CloseScheduleCommand`. The server provides schedule, availability, reference-data, and audit interfaces with optimistic concurrency.

### Implementation

Implement interval validation, overlap queries, publication state transitions, capacity guards, version checks, and client calendar/list views. Store all timestamps with an explicit time zone and retain prior versions.

### Testing

Cover interval boundaries, daylight-saving transitions, overlap detection, capacity changes, publication, closure, stale updates, permissions, and concurrent booking interaction.

### Deployment

Deploy schedule tables, overlap indexes, migration checks, and monitoring for rejected transitions. Confirm time-zone configuration and rollback before opening schedule management.

## Contracts and interfaces

### Client contract

- `listSchedules(query) -> ScheduleSummary[]`
- `createSchedule(command) -> Schedule`
- `updateSchedule(id, command, version) -> Schedule`
- `publishSchedule(id, version) -> Schedule`
- `closeSchedule(id, reason, version) -> Schedule`

### Server contract

- Validate all temporal and capacity rules transactionally.
- Expose only schedules within actor scope.
- Return the committed schedule version and stable error envelope.

### Interfaces

Versioned HTTP/JSON routes live under `/api/aad/schedules`. The availability interface is read-only and derives slots from published schedules. Mutations require an idempotency key and entity version.

## Acceptance criteria

- Admissions Staff can create and publish a valid schedule.
- Overlapping or invalid intervals cannot be persisted.
- A schedule cannot publish without at least one valid available slot.
- Capacity cannot be reduced below existing bookings.
- System Administrator configuration is distinct from Admissions Staff schedule operations.
- Concurrent edits produce `CONFLICT` rather than silently overwriting changes.
- Closed schedules remain auditable and explain why future availability changed.

## Effort estimation

| Work item | Estimate |
|---|---:|
| Requirements and analysis | 4 h |
| Temporal and domain design | 6 h |
| Client calendar and forms | 9 h |
| Server and persistence implementation | 12 h |
| Automated and integration testing | 9 h |
| Deployment and migration verification | 4 h |
| **Total** | **44 h** |

