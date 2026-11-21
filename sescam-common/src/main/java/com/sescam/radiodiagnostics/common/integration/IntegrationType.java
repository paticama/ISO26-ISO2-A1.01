package com.sescam.radiodiagnostics.common.integration;

/**
 * External hospital systems that C9 (sescam-integration) can be connected to (UC-ADM-04).
 */
public enum IntegrationType {
    /** Single sign-on identity provider (FR-ADM-16). */
    SSO,
    /** Electronic health record, EHR/HCE (FR-ADM-17, FR-ADM-18). */
    EHR,
    /** Radiology information system and image archive, RIS/PACS (FR-ADM-19, FR-ADM-20). */
    RIS_PACS,
    /** SMS and email gateway used for notifications (FR-ADM-21). */
    MESSAGING
}
