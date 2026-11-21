package com.sescam.radiodiagnostics.common.security;

/**
 * Lets clients tell an empty result (A2) apart from a successful search with matches.
 * Failures are reported through exceptions, never through an empty list.
 */
public enum AuditResultStatus {
    MATCHES_FOUND,
    NO_MATCHES
}
