package com.sescam.radiodiagnostics.common.security;

/**
 * Outbound port to C1 (sescam-security) for verifying role-based permissions (UC-ADM-02).
 */
public interface AccessControlService {

    /**
     * Checks whether the authenticated user has permission to manage system configurations.
     */
    boolean canManageConfiguration(String actorId, String role);

    /**
     * Checks whether the authenticated user may view and change the external integrations (UC-ADM-04).
     * Until C1 defines a separate permission, it is the same as managing the system configuration.
     */
    default boolean canManageIntegrations(String actorId, String role) {
        return canManageConfiguration(actorId, role);
    }

    /**
     * Checks whether the authenticated user has permission to register, update, consult, or discharge patients (UC-AAD-01).
     * By default allows admissions/administrative staff, receptionists, and system administrators.
     */
    default boolean canManagePatients(String actorId, String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String r = role.trim().toUpperCase();
        return r.contains("ADMISSION") || r.contains("ADMINISTRATIVE") || r.contains("RECEPTIONIST") || r.contains("ADMIN");
    }

    /**
     * Checks whether the authenticated user may order and prescribe diagnostic procedures (UC-PHY-01).
     * By default only physicians; administrators are not allowed to prescribe.
     */
    default boolean canOrderProcedures(String actorId, String role) {
        return role != null && role.trim().toUpperCase().contains("PHYSICIAN");
    }

    /**
     * Checks whether the authenticated user may log procedure executions and incidents (UC-PHY-05).
     * By default physicians, radiologists and radiology technicians (the actors of the communication diagrams).
     */
    default boolean canLogProcedureExecution(String actorId, String role) {
        if (role == null) {
            return false;
        }
        String r = role.trim().toUpperCase();
        return r.contains("PHYSICIAN") || r.contains("RADIOLOGIST") || r.contains("TECHNICIAN");
    }

    /**
     * Checks whether the authenticated user may book, reschedule, or cancel examination appointments (UC-AAD-05).
     * By default only admissions/administrative/receptionist staff; System Administrators are denied (A6).
     */
    default boolean canManageBookings(String actorId, String role) {
        if (actorId == null || actorId.isBlank() || role == null || role.isBlank()) {
            return false;
        }
        String r = role.trim().toUpperCase();
        return r.contains("ADMISSION") || r.contains("ADMINISTRATIVE") || r.contains("RECEPTIONIST");
    }

    /**
     * Checks whether the authenticated user may consult and manage their own appointments (UC-PAT-01).
     * Only patients; staff use the booking area of UC-AAD-05 instead. Whether each appointment belongs
     * to this patient is checked separately by the admissions module (A8).
     */
    default boolean canManageOwnAppointments(String actorId, String role) {
        if (actorId == null || actorId.isBlank() || role == null || role.isBlank()) {
            return false;
        }
        return role.trim().equalsIgnoreCase("PATIENT");
    }

    /**
     * Checks whether the authenticated user may view DICOM studies and author/sign diagnostic reports (UC-PHY-04).
     * Only physicians and radiologists may author, sign, or amend diagnostic reports.
     */
    default boolean canAuthorDiagnosticReports(String actorId, String role) {
        if (role == null) {
            return false;
        }
        String r = role.trim().toUpperCase();
        return r.contains("PHYSICIAN") || r.contains("RADIOLOGIST");
    }
}
