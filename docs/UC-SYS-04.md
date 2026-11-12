# UC-ADM-04 — Manage External Integrations and Interoperability

## Use Case

- **UC:** `UC-ADM-04`
- **Requirements:** `FR-ADM-16`, `FR-ADM-17`, `FR-ADM-18`, `FR-ADM-19`, `FR-ADM-20`, `FR-ADM-21`
- **Phase:** Elaboration (Iteration 1)
- **Module:** `sescam-integration` (`v0.1.0`) — Module ADM (Administration)
- **Minutes:** [Minutes of meetings](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings) (Meeting #4 minutes)
- **GitHub Issue:** [#59](https://github.com/paticama/ISO26-ISO2-A1.01/issues/59)
- **Primary actor:** System Administrator
- **Secondary actors:** Corporate SSO (`<<system>>`), Hospital EHR (HCE) (`<<system>>`), Patient Admission System (`<<system>>`), RIS/PACS (`<<system>>`)[cite: 1, 2]
- **System:** SESCAM Radiodiagnostics (`sescam-integration`)
- **Goal:** Allow a System Administrator to configure and monitor integrations with external hospital systems (`Corporate SSO`, `Hospital EHR (HCE)`, `Patient Admission System`, and `RIS/PACS`) while making connection and synchronization status visible[cite: 1, 2].

## UML Relationships

- **`<<include>>` Record Audit Event:** Every confirmed integration configuration change or state transition is recorded so it is attributable to the System Administrator and available for audit review.
- **`<<extend>>` Check Integration Connection:** Optional; attaches at the extension point `AfterSettingsValidation` (Main Flow step 5) when the integration supports a connection check, and can also be triggered by the System Administrator to retry after a failure.
- **Related use cases (context only):** `UC-ADM-01` manages system-wide configuration; `UC-ADM-02` governs integration-management permissions; integration changes are reviewed in `UC-ADM-03`.

## Goal and scope

The System Administrator configures and maintains the system's interoperability connections with external hospital systems (`Corporate SSO`, `Hospital EHR (HCE)`, `Patient Admission System`, and `RIS/PACS` using standard protocols such as DICOM, HL7, and FHIR)[cite: 1, 2]. The use case covers integration definitions, connection settings, safe credential references, validation, optional connectivity verification, activation, and configuration history. It does not define the detailed clinical data mapping, implementation of individual external connectors, or routine operational use of exchanged data.

## Preconditions

1. The System Administrator is authenticated (via Corporate SSO) and authorized to manage integrations (`UC-ADM-02`).
2. The target external system (`Corporate SSO`, `Hospital EHR (HCE)`, `Patient Admission System`, or `RIS/PACS`) and its supported interface are known to the deployment[cite: 1, 2].
3. The integration service (`sescam-integration`) is available.

## Postconditions

- A valid integration configuration is stored with a clear enabled, disabled, or pending state.
- Configuration changes and state transitions are attributable to the System Administrator and recorded in the audit history (`Record Audit Event`), without exposing secrets in client responses or logs.
- Failed validation, connection checks, or activation attempts do not silently replace a working configuration.

## Client–Server flow

1. The client requests the supported integration catalog and current integration statuses.
2. The server returns authorized configuration summaries and health indicators, omitting secret values.
3. The System Administrator selects an integration (`Corporate SSO`, `Hospital EHR (HCE)`, `Patient Admission System`, or `RIS/PACS`) and creates or updates its endpoint, protocol options, and credential reference[cite: 1, 2].
4. The client submits the change with the current configuration version.
5. The server validates supported settings, endpoint syntax, credential reference, and security policy.
6. The server stores the versioned configuration; where requested and supported (`<<extend>> Check Integration Connection`), it performs a controlled connectivity check against the target external system or marks the configuration pending activation.
7. The server commits the configuration and triggers `Record Audit Event` atomically, returning status and a diagnostic summary without disclosing sensitive data.

## Main flow

1. The System Administrator opens the integration management view.
2. The system lists configured external hospital-system integrations and their administrative and synchronization status.
3. The System Administrator selects an integration and enters or updates its supported connection settings using the approved secure configuration mechanism.
4. The server validates the configuration and verifies that any referenced secret is available to the integration service.
5. *[Extension point: `AfterSettingsValidation`]* Where supported and requested, the System Administrator or system triggers `Check Integration Connection` (`<<extend>>`) to verify reachability with the external system, or saves the configuration for later activation.
6. The server records the check result and configuration version, and reports success, failure, or pending activation.
7. The System Administrator reviews and confirms enabling the integration when validation succeeds; the server persists the state transition and includes `Record Audit Event` to log the change.

## Alternate and error flows

- **Unsupported integration:** Unsupported integration type or option -> return `INTEGRATION_UNSUPPORTED`.
- **Invalid settings:** Invalid endpoint or protocol settings -> return `INTEGRATION_CONFIGURATION_INVALID` and preserve the last known working configuration where possible.
- **Missing or inaccessible credential:** Referenced credential is missing or inaccessible -> return `INTEGRATION_CREDENTIAL_UNAVAILABLE` without revealing its value.
- **Connectivity check failure:** Connectivity check against `Corporate SSO`, `Hospital EHR (HCE)`, `Patient Admission System`, or `RIS/PACS` fails -> retain the previous active configuration or leave the new configuration disabled, return `INTEGRATION_CONNECTION_FAILED` with a safe diagnostic, and allow the System Administrator to retry.
- **Concurrent modification:** A concurrent edit makes the submitted version stale -> return `CONFLICT` and require a refresh.
- **Unauthorized or unauthenticated access:** The actor lacks permission -> return `FORBIDDEN`; unauthenticated requests -> return `UNAUTHENTICATED`.
- **Service unavailability:** Remote external system or local integration service (`sescam-integration`) is unavailable -> reflect the unavailable condition in the integration status and report a failed or indeterminate check, never a successful connection.

## RADIT

### Requirements

- Manage supported external hospital-system integration configurations (`Corporate SSO`, `Hospital EHR (HCE)`, `Patient Admission System`, `RIS/PACS`)[cite: 1, 2].
- Validate settings and provide a safe connectivity-check outcome before activation when supported.
- Protect credentials and distinguish configured, tested, enabled, disabled, and failed states.
- Preserve configuration versions and audit changes without interrupting a working integration on invalid updates.

### Analysis

Integration endpoints (`Corporate SSO`, `Hospital EHR (HCE)`, `Patient Admission System`, and `RIS/PACS`) are external dependencies and can fail independently of the platform[cite: 1, 2]. Configuration changes therefore need explicit lifecycle state and safe diagnostics. Connectivity testing confirms reachability only; it must not be presented as proof of end-to-end clinical correctness. Supported interface standards (DICOM, HL7, FHIR), retry behavior, health indicators, and credential-management mechanisms are refined during analysis[cite: 1].

### Design

Use `IntegrationCatalogQuery`, `IntegrationConfiguration`, `UpdateIntegrationCommand`, `TestIntegrationCommand`, and `IntegrationStatus`. Keep secret material in a protected secret store and store only a reference in the integration configuration. A health-check adapter reports a bounded, non-sensitive result, and an audit service (`Record Audit Event`) logs committed mutations.

### Implementation

Implement supported-type validation, versioned configuration, secret-reference handling, authorization, controlled test execution, state transitions, audit events, and safe error mapping. Ensure configuration changes cannot leak tokens, credentials, or patient data into diagnostics or ordinary logs.

### Testing

Test supported and unsupported settings, secret access, valid and failed connectivity checks across external systems, stale updates, activation state, authorization, rollback/preservation of active configuration, and redaction in logs and responses.

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

- Enforce supported integration policy and System Administrator scope.
- Never return secret values or sensitive remote response content in responses, diagnostics, or logs.
- Clearly separate configuration persistence from connection-test and activation outcomes.
- Preserve or restore the last known working configuration when an update fails validation or activation, returning stable error codes (`INTEGRATION_UNSUPPORTED`, `INTEGRATION_CONFIGURATION_INVALID`, `INTEGRATION_CREDENTIAL_UNAVAILABLE`, `INTEGRATION_CONNECTION_FAILED`, `CONFLICT`, `FORBIDDEN`, `UNAUTHENTICATED`).

## Acceptance criteria

- A System Administrator can view configured integrations and manage supported settings for external hospital systems (`Corporate SSO`, `Hospital EHR (HCE)`, `Patient Admission System`, `RIS/PACS`)[cite: 1, 2].
- Invalid settings and unavailable credentials are rejected with actionable, non-sensitive errors without silently replacing a working configuration.
- Integration credentials are protected in display, storage, logs, and diagnostics according to security requirements.
- Connectivity test results are distinct from activation state and do not imply end-to-end data correctness.
- Connection failures are visible and distinguishable from successful operation.
- Each configuration change and state transition is versioned and attributable via `Record Audit Event`.

## Decisions (DECISIONS.md)

- Refer to `DECISIONS.md` and issue [#59](https://github.com/paticama/ISO26-ISO2-A1.01/issues/59) for architectural, protocol (DICOM/HL7/FHIR), and credential-management decisions[cite: 1].
