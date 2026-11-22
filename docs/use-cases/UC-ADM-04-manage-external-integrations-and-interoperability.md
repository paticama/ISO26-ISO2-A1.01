# UC-ADM-04 – Manage External Integrations & Interoperability (C9 v1.0.0)

## Use Case
- **UC:** `UC-ADM-04`
- **RF:** `FR-ADM-16`, `FR-ADM-17`, `FR-ADM-18`, `FR-ADM-19`, `FR-ADM-20`, `FR-ADM-21`
- **Phase:** Elaboration
- **Iteration:** 1
- **Module:** `sescam-integration` (C9, v1.0.0)
- **Priority:** High (Raised by team decision: SSO, EHR and RIS/PACS integration is the highest technical risk and a dependency of every High use case, Criteria 1 and 3)
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#59](https://github.com/paticama/ISO26-ISO2-A1.01/issues/59)
- **Actor:** System administrator
- **Description:** Enables an authorized system administrator to configure and monitor external hospital integrations while making connection and synchronization status visible.
- **Preconditions:**
  1. The administrator is authenticated and authorized to manage integrations.
  2. The external system and its supported interface are known to the deployment.
- **Postconditions:**
  1. Valid integration settings and their connection or synchronization status are reported.
  2. Failed validation or connection checks do not silently replace a working configuration.
- **Main Success Scenario:**
  1. The administrator opens the integrations area.
  2. The system displays configured integrations and their current status.
  3. The administrator selects an integration to configure or review.
  4. The administrator enters or updates endpoint and connection settings through the approved secure configuration mechanism.
  5. The system validates the settings and performs a connection check where supported.
  6. The administrator reviews and confirms the change.
  7. The system saves the configuration and reports connection or synchronization status.
- **Alternative / Exception Flows:**
  - **A1. Unauthorized administrator:** The system rejects the operation without exposing or changing protected settings.
  - **A2. Invalid configuration or failed connection check:** The system explains the failure and preserves the last known working configuration where possible.
  - **A3. External system unavailable:** The integration status reflects the condition and allows retry or diagnostic review.
  - **A4. Secret handling:** Credentials are protected from unintended display and are excluded from ordinary logs and error messages.

## RADIT Disciplines
- [x] Requirements
- [x] Analysis
- [x] Design
- [x] Implementation
- [x] Testing

## Contracts and Interfaces
- **System:** sescam-integration.
- **Interfaces (`sescam-common`, `com.sescam.radiodiagnostics.common.integration`):** `IntegrationService` (`listIntegrations`, `getIntegration`, `validateSettings`, `updateIntegration`, `checkConnection`). `AccessControlService` (`common.security`) gets one new method, `canManageIntegrations`, which by default is the same as `canManageConfiguration` until C1 defines its own permission.
- **DTOs:** `IntegrationSettingsDTO` (the credential is write only and masked in `toString`), `UpdateIntegrationRequestDTO`, `IntegrationSummaryDTO` (only says whether a credential is stored), `SettingsCheckResultDTO`, `ConnectionCheckResultDTO`, `UpdateIntegrationResponseDTO`, `IntegrationType` (SSO, EHR, RIS_PACS, MESSAGING), `IntegrationStatus` (NOT_CONFIGURED, NOT_CHECKED, CONNECTED, CONNECTION_FAILED, UNAVAILABLE).
- **Errors:** `UnauthorizedIntegrationAccessException` (A1, HTTP 403), `InvalidIntegrationSettingsException` (A2, 400), `IntegrationConnectionFailedException` (A2/A3, 502; the status tells "refused" from "unreachable"), `ConcurrentIntegrationChangeException` (409), `IntegrationNotFoundException` (404), `IntegrationSaveException` (500).
- **REST (`sescam-server-app`):** `GET /api/v1/integrations`, `GET /api/v1/integrations/{id}`, `POST /api/v1/integrations/{id}/validate` (step 5, saves nothing), `PUT /api/v1/integrations/{id}` (steps 6-7), `POST /api/v1/integrations/{id}/check` (A3 retry), headers `X-Actor-Id`, `X-Actor-Role`.
- **Client view (`sescam-client-app`):** `/integrations` (list with status), `/integrations/{id}` (current configuration and form), `POST /integrations/{id}/validate` (step 5), `POST /integrations/{id}/save` (steps 6-7), `POST /integrations/{id}/check` (A3 retry). REST client `IntegrationApiClient`, form validations in `IntegrationSettingsForm` (https URL, no credentials in the URL, limits on user name and credential). The credential field is a password field that is never filled in again.
- **Inputs:** Integration identifier, supported endpoint and connection settings, securely supplied credentials, and authenticated administrator identity.
- **Outputs:** Configuration status, connection or synchronization health where supported, and diagnostics that do not disclose secrets.

## Analysis and Design Diagrams
- Analysis: [class diagram](../diagrams/UC-ADM-04/uc-adm-04-class-analysis.puml), [communication – normal flow](../diagrams/UC-ADM-04/uc-adm-04-comm-normal.puml), [communication – alternative flows](../diagrams/UC-ADM-04/uc-adm-04-comm-alt.puml).
- Design: [class diagram](../diagrams/UC-ADM-04/uc-adm-04-class-design.puml), [sequence diagram](../diagrams/UC-ADM-04/uc-adm-04-sequence-design.puml).
- Design decisions:
  - **The credential is write only.** It goes from the form to the server, is stored encrypted (AES-256-GCM, key in memory until the key management decision exists) and is never returned, logged, audited or put in an error message (A4).
  - **A failed check never replaces a working configuration.** `validateSettings` (step 5) saves nothing; `updateIntegration` checks again and throws if the check fails (A2, A3). `checkConnection` only changes the status of the saved configuration, never its settings or version.
  - **No false "connected".** The real connectors (SSO, HL7/FHIR, DICOM, messaging) are outside this use case. Until they exist, `NoOpConnectionProbe` returns `NOT_CHECKED`, so the status never claims a connection that was not tested. Each connector will bring its own `ConnectionProbe`.
  - **Only https endpoints**, without credentials inside the URL.
  - **Audit:** confirmed changes (`INTEGRATION_CONFIGURATION_UPDATED`), rejected changes (`INTEGRATION_CONFIGURATION_REJECTED`), connection retries (`INTEGRATION_CONNECTION_CHECKED`) and denied attempts (`INTEGRATION_ACCESS_DENIED`) are recorded in the C1 audit trail, without secrets.

## Acceptance Criteria
- [ ] Authorized administrators can view configured integrations and their status.
- [ ] Supported connection settings can be updated by authorized administrators.
- [ ] Invalid settings and failed connection checks are reported without silently replacing working settings.
- [ ] Credentials are protected in display, storage, and diagnostics.
- [ ] Connection failures are visible and distinguishable from successful operation.
- [ ] Successful changes are confirmed and attributable for audit.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../DECISIONS.md).

## Effort Estimation
- **Estimated effort without AI:** 12 hours
- **Estimated effort with AI (Copilot):** 6 hours
- **Estimated effort with AI + 25% safety margin:** 7.5 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
