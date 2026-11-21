package com.sescam.radiodiagnostics.security.internal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AuditAccessPolicyTest {

    private final AuditAccessPolicy policy = new AuditAccessPolicy();

    @ParameterizedTest
    @CsvSource(value = {
            "admin-1, ADMINISTRATOR, true",
            "admin-1, system_administrator, true",
            "admin-1, ' Admin ', true",
            "auditor-1, COMPLIANCE_REVIEWER, true",
            "physician-1, PHYSICIAN, false",
            "patient-1, PATIENT, false",
            "'', ADMINISTRATOR, false",
            "NULL, ADMINISTRATOR, false",
            "admin-1, NULL, false",
            "admin-1, ' ', false"
    }, nullValues = "NULL")
    void onlyAdministratorsAndComplianceReviewersCanViewAudit(String actor, String role, boolean expected) {
        assertThat(policy.canViewAuditRecords(actor, role)).isEqualTo(expected);
    }
}
