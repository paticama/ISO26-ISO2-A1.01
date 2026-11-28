package com.sescam.radiodiagnostics.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Default RBAC rule for UC-PAT-01: only patients manage their own appointments. */
class PatientAppointmentAccessTest {

    /** Only the abstract method is stubbed, so the default methods under test run as written. */
    private final AccessControlService accessControl = (actorId, role) -> false;

    @Test
    void patientRoleMayManageOwnAppointments() {
        assertTrue(accessControl.canManageOwnAppointments("PAT-001", "PATIENT"));
        assertTrue(accessControl.canManageOwnAppointments("PAT-001", " patient "));
    }

    @Test
    void staffRolesMayNotUseThePatientAppointmentArea() {
        assertFalse(accessControl.canManageOwnAppointments("adm-1", "ADMISSIONS_STAFF"));
        assertFalse(accessControl.canManageOwnAppointments("phy-1", "PHYSICIAN"));
        assertFalse(accessControl.canManageOwnAppointments("admin-1", "SYSTEM_ADMINISTRATOR"));
    }

    @Test
    void missingActorOrRoleIsDenied() {
        assertFalse(accessControl.canManageOwnAppointments(null, "PATIENT"));
        assertFalse(accessControl.canManageOwnAppointments(" ", "PATIENT"));
        assertFalse(accessControl.canManageOwnAppointments("PAT-001", null));
        assertFalse(accessControl.canManageOwnAppointments("PAT-001", ""));
    }
}
