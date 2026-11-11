# UC-SYS-04 — Manage External Integrations & Interoperability

## Use Case

- **UC:** `UC-SYS-04`
- **Requirement:** `FR-ADM-16`
- **Phase:** Inception
- **Module:** Administrators (`v0.4.0`)
- **Minutes:** [Minutes of meetings](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings) (details TBD)
- **GitHub Issue:** [#59](https://github.com/paticama/ISO26-ISO2-A1.01/issues/59)
- **Primary actor:** System Administrator
- **System:** Administrator Server
- **Goal:** Manage integrations with external hospital systems.

## Goal and scope

The System Administrator configures and maintains the system's interoperability connections with external hospital systems. The use case covers integration definitions, connection settings, safe credential references, validation, activation, and configuration history. It does not define the clinical semantics or routine operational use of exchanged data.

## Preconditions

1. The administrator is authenticated and authorized to manage integrations.
2. The target integration type is supported and required organizational details are available.

## Postconditions

- A valid integration configuration is stored with a clear enabled, disabled, or pending state.
- Configuration changes are auditable, and secrets are not exposed in client responses or logs.
- Failed validation or activation does not silently replace a working configuration.

## Client–Server flow

1. The client requests the supported integration catalog and current integration statuses.
2. The server returns authorized configuration summaries, omitting secret values.
3. The administrator selects an integration and creates or updates its endpoint, protocol options, and credential reference.
4. The client submits the change with the current configuration version.
5. The server validates supported settings, endpoint syntax, credential reference, and security policy.
6. The server stores the versioned configuration; where requested, it performs a controlled connectivity check or marks the configuration pending activation.
7. The server returns status and a diagnostic summary without disclosing sensitive data.

## Main flow

1. The administrator opens the integration management view.
2. The system lists configured external hospital-system integrations and their administrative status.
3. The administrator selects an integration and enters or updates its supported connection settings.
4. The server validates the configuration and verifies that any referenced secret is available to the integration service.
5. The administrator requests a connectivity test or saves the configuration for later activation.
6. The server performs the permitted check, records the result and configuration version, and reports success, failure, or pending activation.
7. The administrator enables the integration when validation succeeds; the server records the state transition and audit event.

## Alternate and error flows

- Unsupported integration type or option: return `INTEGRATION_UNSUPPORTED`.
- Invalid endpoint or protocol settings: return `INTEGRATION_CONFIGURATION_INVALID`.
- Referenced credential is missing or inaccessible: return `INTEGRATION_CREDENTIAL_UNAVAILABLE` without revealing its value.
- Connectivity check fails: retain the previous active configuration or leave the new configuration disabled; return `INTEGRATION_CONNECTION_FAILED` with a safe diagnostic.
- A concurrent edit makes the submitted version stale: return `CONFLICT`.
- The actor lacks permission: return `FORBIDDEN`.
- Remote service or local integration service is unavailable: report a failed or indeterminate check, never a successful connection.

## RADIT

### Requirements

- Manage supported external hospital-system integration configurations.
- Validate settings and provide a safe connectivity-check outcome before activation when supported.
- Protect credentials and distinguish configured, tested, enabled, disabled, and failed states.
- Preserve configuration versions and audit changes without interrupting a working integration on invalid updates.

### Analysis

Integration endpoints are external dependencies and can fail independently of the platform. Configuration changes therefore need explicit lifecycle state and safe diagnostics. Connectivity testing confirms reachability only; it must not be presented as proof of end-to-end clinical correctness.

### Design

Use `IntegrationCatalogQuery`, `IntegrationConfiguration`, `UpdateIntegrationCommand`, `TestIntegrationCommand`, and `IntegrationStatus`. Keep secret material in a protected secret store and store only a reference in the integration configuration. A health-check adapter reports a bounded, non-sensitive result.

### Implementation

Implement supported-type validation, versioned configuration, secret-reference handling, authorization, controlled test execution, state transitions, audit events, and safe error mapping. Ensure configuration changes cannot leak tokens or patient data into diagnostics.

### Testing

Test supported and unsupported settings, secret access, valid and failed connectivity checks, stale updates, activation state, authorization, rollback/preservation of active configuration, and redaction in logs and responses.

### Deployment

Deploy integration adapters, protected secret references, network settings, and monitoring. Validate connectivity in a non-production environment first and document disablement and rollback procedures.

## Contracts and interfaces

### Client contract

- `listIntegrations(query) -> IntegrationSummary[]`
- `getIntegrationConfiguration(integrationId) -> IntegrationConfiguration`
- `updateIntegration(integrationId, command, version) -> IntegrationStatus`
- `testIntegration(integrationId, command) -> IntegrationTestResult`
- `setIntegrationEnabled(integrationId, enabled, version) -> IntegrationStatus`

### Server contract

- Enforce supported integration policy and administrator scope.
- Never return secret values or sensitive remote response content.
- Clearly separate configuration persistence from connection-test and activation outcomes.
- Preserve or restore the last known working configuration when an update fails validation or activation.

## Acceptance criteria

- A System Administrator can manage supported integrations with external hospital systems.
- Invalid settings and unavailable credentials are rejected with actionable, non-sensitive errors.
- Secret values are never returned or written to ordinary logs.
- Connectivity test results are distinct from activation state and do not imply end-to-end data correctness.
- Failed updates do not silently disable or overwrite a working integration.
- Each configuration change and state transition is versioned and auditable.

## Decisions (DECISIONS.md)

- No specific decision is identified in issue [#59](https://github.com/paticama/ISO26-ISO2-A1.01/issues/59); record applicable decisions here when established.
