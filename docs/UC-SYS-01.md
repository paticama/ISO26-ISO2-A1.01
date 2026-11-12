# UC-ADM-01 — Manage System Configurations and Parameters

## Use Case

- **UC:** `UC-ADM-01`
- **Requirements:** `FR-ADM-01`, `FR-ADM-02`, `FR-ADM-03`, `FR-ADM-04`, `FR-ADM-05`, `FR-ADM-06`
- **Phase:** Elaboration (Iteration 1)
- **Module:** `sescam-catalogue` (`v0.1.0`) — Module ADM (Administration)
- **Minutes:** [Minutes of meetings](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings) (Meeting #4 minutes)
- **GitHub Issue:** [#44](https://github.com/paticama/ISO26-ISO2-A1.01/issues/44)
- **Primary actor:** System Administrator
- **Secondary actors:** None
- **System:** SESCAM Radiodiagnostics (`sescam-catalogue`)
- **Goal:** Allow a System Administrator to inspect, create, and update system-wide configuration parameters (examination types, protocols, schedules, appointment policies, and priorities) safely and consistently.

## UML Relationships

- **`<<include>>` Record Audit Event:** Every confirmed configuration change is recorded so it is attributable to the System Administrator and available for audit review.
- **Related use cases (context only):** `UC-ADM-02` manages user roles and access control (governed as a precondition); `UC-ADM-03` queries the resulting audit records; `UC-ADM-04` manages external system integrations.

## Goal and scope

The System Administrator can inspect, create, and update configuration parameters that govern system behavior. The use case covers validation, authorization, persistence, audit history, and activation of supported settings. It does not cover user-role management (`UC-ADM-02`), external integration setup (`UC-ADM-04`), or operational scheduling.

## Preconditions

1. The System Administrator is authenticated (via Corporate SSO).
2. The System Administrator has permission to manage system configuration (`UC-ADM-02`).
3. The configuration service (`sescam-catalogue`) is available.

## Postconditions

- A valid change is persisted and its effective or pending value is visible to authorized users.
- The change is attributed to the System Administrator and recorded in the configuration and audit history.
- Invalid, conflicting, or unauthorized changes do not modify the active configuration.

## Client–Server flow

1. The client requests the configuration categories and parameters visible to the System Administrator.
2. The server checks authorization and returns values, descriptions, constraints, and current versions without exposing protected secrets.
3. The System Administrator selects a parameter and submits a proposed value.
4. The client performs basic format validation and sends a typed update command with the current version and a reason when required.
5. The server rechecks authorization, validates type, range, dependencies, and version, then persists the change and triggers `Record Audit Event` atomically.
6. The server returns the committed value, version, and activation status; the client displays the result.

## Main flow

1. The System Administrator opens the system configuration view.
2. The client displays permitted settings grouped by category, including current value and applicable constraints.
3. The System Administrator chooses a setting and enters a new value.
4. The server validates the value and any dependent settings.
5. The server saves the new version and includes `Record Audit Event` to log the actor, timestamp, previous and new values (redacting secrets), and change reason where applicable.
6. The setting is activated immediately when safe, or marked pending activation when a controlled reload or restart is required.
7. The client confirms the outcome and exposes the change history to authorized System Administrators.

## Alternate and error flows

- **Invalid type or range:** A value has the wrong type or violates a configured bound -> return `CONFIGURATION_INVALID` with field-level details and leave active values unchanged.
- **Dependency conflict:** A change conflicts with a dependent setting -> return `CONFIGURATION_DEPENDENCY_INVALID` and leave active values unchanged.
- **Concurrent modification:** Another System Administrator changed the value since it was loaded -> return `CONFLICT` and require a refresh/review before saving.
- **Unauthorized or unauthenticated access:** The actor lacks permission -> return `FORBIDDEN`; unauthenticated requests -> return `UNAUTHENTICATED`.
- **Deferred activation:** A setting requires restart or controlled activation -> save as pending and clearly report that it is not yet effective.
- **Persistence or activation failure:** Persistence or activation fails -> return `SERVICE_UNAVAILABLE` or `CONFIGURATION_ACTIVATION_FAILED`; do not report an uncommitted value as active.

## RADIT

### Requirements

- Allow authorized System Administrators to inspect and manage supported configuration parameters.
- Validate data type, allowed range, and dependencies on the server.
- Preserve version history and record configuration mutations for audit.
- Protect secrets and distinguish saved, pending, and active settings.

### Analysis

Configuration affects shared system behavior and must be treated as versioned, controlled data. Client-side checks are advisory; the server is the authority for validation and authorization. A failed update must not leave a partially applied configuration. The configuration catalogue, permitted value ranges, and reload/restart rules are refined during analysis.

### Design

Use `ConfigurationQuery`, `UpdateConfigurationCommand`, `ConfigurationEntry`, and `ConfigurationHistoryEntry`. The server coordinates a configuration repository, policy validator, activation mechanism, and audit service (`Record Audit Event`). Updates carry an expected version for optimistic concurrency.

### Implementation

Implement typed configuration schemas, server-side validation, access policies, transactional writes, secret redaction, audit records, activation state, and administrator-facing error mapping.

### Testing

Test valid changes, invalid types and bounds, dependency checks, permissions, concurrent edits, secret redaction, activation failures, history accuracy, and rollback/no-partial-update behavior.

### Deployment

Deploy schema changes and configuration migration with a documented rollback. Verify initial values, access policy, audit capture, and health checks before enabling administrator edits.

## Contracts and interfaces

### Client contract

- `listConfigurations(query) -> ConfigurationEntry[]`
- `updateConfiguration(key, command, version) -> ConfigurationEntry`
- `getConfigurationHistory(key, query) -> ConfigurationHistoryPage`

### Server contract

- Enforce authorization and validation independently of the client.
- Commit configuration and audit history consistently.
- Never expose secret values in read responses or audit displays.
- Return the committed version and activation state using stable error codes (`CONFIGURATION_INVALID`, `CONFIGURATION_DEPENDENCY_INVALID`, `CONFLICT`, `FORBIDDEN`, `UNAUTHENTICATED`, `SERVICE_UNAVAILABLE`, `CONFIGURATION_ACTIVATION_FAILED`).

## Acceptance criteria

- A System Administrator can view and update permitted configuration parameters.
- Invalid values and invalid dependencies are rejected without changing the active configuration.
- Concurrent updates cannot silently overwrite a newer value.
- Each successful mutation is attributable to the System Administrator and has a retrievable audit/history entry.
- Secret values are not disclosed in responses or audit history.
- Pending activation is clearly distinguished from an effective configuration.

## Decisions (DECISIONS.md)

- Refer to `DECISIONS.md` and issue [#44](https://github.com/paticama/ISO26-ISO2-A1.01/issues/44) for architectural and configuration-policy decisions.
