# UC-ADM-02 — Manage User Roles and Access Control

## Use Case

- **UC:** `UC-ADM-02`
- **Requirements:** `FR-ADM-07`, `FR-ADM-08`, `FR-ADM-09`, `FR-ADM-10`
- **Phase:** Elaboration (Iteration 1)
- **Module:** `sescam-security` (`v0.1.0`) — Module ADM (Administration)
- **Minutes:** [Minutes of meetings](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings) (Meeting #4 minutes)
- **GitHub Issue:** [#50](https://github.com/paticama/ISO26-ISO2-A1.01/issues/50)
- **Primary actor:** System Administrator
- **Secondary actors:** None
- **System:** SESCAM Radiodiagnostics (`sescam-security`)
- **Goal:** Allow a System Administrator to assign and revoke user roles and maintain role-based access permissions safely and consistently.

## UML Relationships

- **`<<include>>` Record Audit Event:** Every confirmed role or permission assignment change is recorded so it is attributable to the System Administrator and available for audit review.
- **Related use cases (context only):** `UC-ADM-01` manages system-wide configuration; role assignments made here govern access preconditions across system use cases and are reviewed in `UC-ADM-03`.

## Goal and scope

The System Administrator assigns and revokes roles and manages the permission definitions associated with those roles[cite: 2]. The use case covers role assignment, access-policy validation, sensitive-change safeguards, and auditability. It does not include authentication-provider configuration (`UC-ADM-04`), audit-reporting workflows (`UC-ADM-03`), or routine clinical and admissions work.

## Preconditions

1. The System Administrator is authenticated (via Corporate SSO) and authorized to manage roles and permissions.
2. The target user account or role exists and is eligible for access management.
3. The security service (`sescam-security`) is available.

## Postconditions

- A valid role or permission change is persisted and takes effect according to the system's authorization policy.
- The change is attributed to the System Administrator and recorded in the audit history (`Record Audit Event`).
- Invalid, policy-violating, or unauthorized changes leave the prior access state unchanged.

## Client–Server flow

1. The client requests users, roles, and permission definitions within the System Administrator's scope.
2. The server authorizes the request and returns the permitted data without exposing credentials or unrelated sensitive information.
3. The System Administrator selects a user and assigns or revokes a role, or edits a role's permitted capabilities.
4. The client submits a typed command with the current version and any required justification.
5. The server validates the target, role, permissions, policy constraints, and expected version.
6. The server commits the change and triggers `Record Audit Event` atomically, then returns the updated access summary.

## Main flow

1. The System Administrator opens access management and searches for a user or role.
2. The system displays the user's current roles and account status, or a role's capabilities.
3. The System Administrator selects an allowed role assignment or permission change.
4. The server verifies that the System Administrator may grant the requested access and that the change does not violate mandatory security constraints or separation-of-duties rules.
5. The server persists the update, includes `Record Audit Event` to log the actor, timestamp, previous and new access state, and change reason where applicable, and invalidates or refreshes relevant authorization state as required.
6. The client confirms the updated access state.

## Alternate and error flows

- **Missing or inactive target:** The user or role does not exist or is inactive -> return `USER_NOT_FOUND` or `ROLE_NOT_FOUND` and leave existing assignments unchanged.
- **Policy or privilege violation:** The requested assignment violates access-control policy, separation-of-duties rules, or would grant prohibited privilege -> return `ACCESS_POLICY_VIOLATION`.
- **Self-lockout / sensitive privilege safeguard:** The System Administrator attempts to remove their last effective administrative access -> reject with `LAST_ADMIN_REQUIRED` unless an approved recovery process applies.
- **Concurrent modification:** A stale version is submitted -> return `CONFLICT` and require a refresh before saving.
- **Unauthorized or unauthenticated access:** The actor lacks the required management permission -> return `FORBIDDEN`; unauthenticated requests -> return `UNAUTHENTICATED`.
- **Persistence or refresh failure:** Persistence or authorization-state refresh fails -> return a stable service error and do not claim the change is effective unless committed.

## RADIT

### Requirements

- Assign and revoke roles for eligible users[cite: 2].
- Manage role definitions and associated security permissions within authorized scope[cite: 2].
- Enforce least privilege and prevent invalid or self-locking administrative changes.
- Maintain an auditable record of access changes.

### Analysis

Role and permission administration is security-sensitive. The server must make authorization decisions and validate privilege boundaries; the client is not trusted to enforce them. Role changes should be versioned to avoid lost updates and should become effective consistently across authorization checks. The role catalogue, granted permissions, and separation-of-duties rules are refined during analysis.

### Design

Use `UserAccessQuery`, `RoleQuery`, `AssignRoleCommand`, `RevokeRoleCommand`, and `UpdateRolePermissionsCommand`. A policy service validates grants and revocations; an audit service (`Record Audit Event`) records committed changes. Updates carry an expected version for optimistic concurrency.

### Implementation

Implement scoped queries, server-side authorization, least-privilege validation, optimistic concurrency, consistent permission-cache invalidation, and append-only change audit records.

### Testing

Test assignment and revocation, unauthorized changes, prohibited grants, last-administrator protection, inactive users/roles, stale updates, effective permission refresh, audit completeness, and failure handling.

### Deployment

Deploy role and permission migrations with a verified baseline administrator and a recovery procedure. Validate that existing user access is preserved or migrated intentionally before enabling management operations.

## Contracts and interfaces

### Client contract

- `getUserAccess(userId) -> UserAccessSummary`
- `listRoles(query) -> RolePage`
- `assignRole(userId, command, version) -> UserAccessSummary`
- `revokeRole(userId, command, version) -> UserAccessSummary`
- `updateRolePermissions(roleId, command, version) -> Role`

### Server contract

- Enforce authentication, scope, least privilege, policy constraints, and optimistic concurrency.
- Apply committed changes consistently to subsequent authorization decisions.
- Record actor, target, timestamp, previous and new access state, and justification where required via `Record Audit Event`.

## Acceptance criteria

- A System Administrator can assign and revoke user roles and manage security permissions within their scope[cite: 2].
- The server rejects unauthorized or policy-violating grants and revocations without altering existing assignments.
- Concurrent changes cannot silently overwrite a newer access state.
- Successful changes are reflected in authorization decisions and have a retrievable audit record.
- A change that would remove the last effective administrative access is safely rejected or routed through an approved recovery path.

## Decisions (DECISIONS.md)

- Refer to `DECISIONS.md` and issue [#50](https://github.com/paticama/ISO26-ISO2-A1.01/issues/50) for architectural and access-control policy decisions.
