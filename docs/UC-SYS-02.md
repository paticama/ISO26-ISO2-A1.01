# UC-SYS-02 — Manage User Roles & Access Control

## Use Case

- **UC:** `UC-SYS-02`
- **Requirement:** `FR-ADM-07`
- **Phase:** Inception
- **Module:** Administrators (`v0.2.0`)
- **Minutes:** [Minutes of meetings](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings) (details TBD)
- **GitHub Issue:** [#50](https://github.com/paticama/ISO26-ISO2-A1.01/issues/50)
- **Primary actor:** System Administrator
- **System:** Administrator Server
- **Goal:** Manage user roles and security permissions.

## Goal and scope

The System Administrator assigns and revokes roles and manages the permission definitions associated with those roles. The use case covers role assignment, access-policy validation, and auditability. It does not include authentication-provider configuration or routine clinical and admissions work.

## Preconditions

1. The administrator is authenticated and authorized to manage roles and permissions.
2. The target user exists and is eligible for access management.

## Postconditions

- A valid role or permission change is persisted and takes effect according to the system's authorization policy.
- The change is recorded with its actor, target, time, and reason where required.
- Invalid changes leave the prior access state unchanged.

## Client–Server flow

1. The client requests users, roles, and permission definitions within the administrator's scope.
2. The server authorizes the request and returns the permitted data without exposing credentials or unrelated sensitive information.
3. The administrator selects a user and assigns or revokes a role, or edits a role's permitted capabilities.
4. The client submits a typed command with the current version and any required justification.
5. The server validates the target, role, permissions, policy constraints, and expected version.
6. The server commits the change and an audit event atomically, then returns the updated access summary.

## Main flow

1. The administrator opens access management and searches for a user or role.
2. The system displays the user's current roles or a role's capabilities.
3. The administrator selects an allowed role assignment or permission change.
4. The server verifies that the administrator may grant the requested access and that the change does not violate mandatory security constraints.
5. The server persists the update, records the previous and new access state, and invalidates or refreshes relevant authorization state as required.
6. The client confirms the updated access state.

## Alternate and error flows

- The user or role does not exist or is inactive: return `USER_NOT_FOUND` or `ROLE_NOT_FOUND`.
- The requested assignment violates policy or would grant prohibited privilege: return `ACCESS_POLICY_VIOLATION`.
- The administrator attempts to remove their last effective administrative access: reject with `LAST_ADMIN_REQUIRED` unless an approved recovery process applies.
- A stale version is submitted: return `CONFLICT` and require a refresh.
- The actor lacks the required management permission: return `FORBIDDEN`.
- Persistence or authorization-state refresh fails: return a stable service error and do not claim the change is effective unless committed.

## RADIT

### Requirements

- Assign and revoke roles for eligible users.
- Manage role definitions and associated security permissions within authorized scope.
- Enforce least privilege and prevent invalid or self-locking administrative changes.
- Maintain an auditable record of access changes.

### Analysis

Role and permission administration is security-sensitive. The server must make authorization decisions and validate privilege boundaries; the client is not trusted to enforce them. Role changes should be versioned to avoid lost updates and should become effective consistently across authorization checks.

### Design

Use `UserAccessQuery`, `RoleQuery`, `AssignRoleCommand`, `RevokeRoleCommand`, and `UpdateRolePermissionsCommand`. A policy service validates grants and revocations; an audit service records committed changes.

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

- Enforce authentication, scope, least privilege, policy constraints, and concurrency.
- Apply committed changes consistently to subsequent authorization decisions.
- Record actor, target, timestamp, change, and justification where required.

## Acceptance criteria

- A System Administrator can assign and revoke user roles and manage security permissions within their scope.
- The server rejects unauthorized or policy-violating grants and revocations.
- Concurrent changes cannot silently overwrite a newer access state.
- Successful changes are reflected in authorization decisions and are auditable.
- A change that would remove the last effective administrative access is safely rejected or routed through an approved recovery path.

## Decisions (DECISIONS.md)

- No specific decision is identified in issue [#50](https://github.com/paticama/ISO26-ISO2-A1.01/issues/50); record applicable decisions here when established.
