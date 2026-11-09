# UC-AAD-05 — Book Requested Examination

## Phase Elaboration

- **Module:** AAD (Admissions, Appointments and Diagnostics)
- **Minutes:** Neutral
- **GitHub Issue:** Neutral
- **Primary actor:** Admissions Staff
- **Supporting actor:** System Administrator
- **Related decisions:** [CU00 initial structure decisions](../../feature/CU00-initial-structure/DECISIONS.md)

## Goal and scope

Admissions Staff books an eligible examination request into an available published slot. System Administrator configures examination catalog, booking constraints, and permissions; the administrator does not perform routine booking.

## Client–Server flow

1. The client displays eligible requests and available published slots.
2. Admissions Staff selects a request and slot and confirms the booking.
3. The client sends a typed command with request, slot, and patient references.
4. The server rechecks eligibility, authorization, capacity, conflicts, and slot state.
5. The server commits the appointment and updates the request and availability atomically.
6. The client shows the appointment confirmation or a specific booking error.

## Main flow

1. Admissions Staff searches for a requested examination.
2. The server confirms that the request is valid, active, and not already booked.
3. The client presents only slots compatible with service, location, duration, and policy.
4. Staff selects a slot and confirms.
5. The server locks the relevant slot, validates all rules again, creates the appointment, and removes or updates the waitlist entry.
6. The server returns the appointment number, date, time zone, location, and next operational status.

## Alternate and error flows

- The request is missing, cancelled, or already booked: `REQUEST_NOT_BOOKABLE`.
- The slot was taken or closed: `SLOT_UNAVAILABLE`.
- Patient or service data is inactive: `REFERENCE_INACTIVE`.
- A conflicting appointment exists: `APPOINTMENT_CONFLICT`.
- The booking violates capacity or lead-time policy: `BOOKING_POLICY_VIOLATION`.
- Duplicate retries with the same idempotency key return the original result.
- Authorization, validation, or service failures return `FORBIDDEN`, `VALIDATION_ERROR`, or `SERVICE_UNAVAILABLE`.

## RADIT

### Requirements

- Show eligible requests and compatible published slots.
- Book atomically with conflict and capacity protection.
- Update request, slot, waitlist, and worklist state consistently.
- Return a durable confirmation and explicit failure reason.

### Analysis

Booking is a cross-aggregate transaction. Availability shown by the client is advisory; the server must revalidate under a lock or equivalent concurrency control. Retry safety requires an idempotency key.

### Design

The client uses `BookExaminationCommand`, `BookableRequestQuery`, and `AvailableSlotQuery`. The server coordinates request, patient, schedule, appointment, waitlist, and worklist repositories in one transactional boundary.

### Implementation

Implement eligibility queries, compatible-slot calculation, transaction locking, idempotency records, appointment creation, and downstream state events. Provide a stable confirmation model.

### Testing

Test successful booking, double booking, capacity races, stale slots, cancelled requests, incompatible services, retries, rollback, and resulting waitlist/worklist state.

### Deployment

Deploy appointment constraints and idempotency storage, migrate existing requests safely, and run a controlled booking smoke test against a published schedule.

## Contracts and interfaces

### Client contract

- `findBookableRequests(query) -> BookableRequest[]`
- `findAvailableSlots(query) -> AvailableSlot[]`
- `bookRequestedExamination(command) -> AppointmentConfirmation`

### Server contract

- Revalidate all references and policies at commit time.
- Make booking, capacity reservation, and state updates atomic.
- Return the same result for a safe idempotent retry.

### Interfaces

Versioned HTTP/JSON routes live under `/api/aad/bookings`. Booking requests include an idempotency key and correlation identifier. Error envelopes identify the failed business rule without leaking storage details.

## Acceptance criteria

- Admissions Staff can book an eligible request into a compatible published slot.
- Two simultaneous bookings cannot consume the same slot beyond capacity.
- A stale or closed slot is rejected explicitly.
- A repeated request with the same idempotency key does not create a second appointment.
- Request, waitlist, appointment, and worklist states are consistent after success.
- A failed booking leaves no partial reservation or status change.
- System Administrator booking policy configuration is not exposed as an Admissions Staff action.

## Effort estimation

| Work item | Estimate |
|---|---:|
| Requirements and transaction analysis | 5 h |
| Booking and concurrency design | 7 h |
| Client booking flow | 8 h |
| Server transaction and integration implementation | 14 h |
| Automated and integration testing | 12 h |
| Deployment and smoke verification | 4 h |
| **Total** | **50 h** |

