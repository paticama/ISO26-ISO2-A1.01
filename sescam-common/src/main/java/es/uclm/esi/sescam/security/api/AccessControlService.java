package es.uclm.esi.sescam.security.api;

/**
 * Contract of UC-ADM-02 "Manage user roles and access control" (module sescam-security).
 *
 * <p>Main flow: {@link #getRoleAssignments} (steps 1-2), {@link #validateRoleChange}
 * (steps 3-4), {@link #confirmRoleChange} (steps 5-6).
 *
 * <p>Rules shared by every operation:
 * <ul>
 *   <li>The requester must hold the permission to manage roles and permissions (granted through
 *       its role; the System administrator role always includes it). Otherwise A1.</li>
 *   <li>Invalid, unauthorized or failed changes never alter the existing assignment.</li>
 *   <li>Every successful change is recorded through {@link AuditService}.</li>
 * </ul>
 */
public interface AccessControlService {

    /**
     * Steps 1-2: returns the current role assignments and account status of a user.
     *
     * @param administratorId authenticated administrator making the request
     * @param targetUserId    existing user account
     * @return current assignments and status
     * @throws AccessDeniedException A1: the administrator lacks the required permission
     */
    RoleAssignmentDTO getRoleAssignments(String administratorId, String targetUserId);

    /**
     * Steps 3-4: validates the requested change against the available roles and the
     * access-control rules, without saving anything.
     *
     * @return outcome {@code VALID}, {@code UNAUTHORIZED} (A1), {@code INVALID} (A2) or
     *         {@code SAFEGUARD_REQUIRED} (A4)
     */
    RoleChangeResultDTO validateRoleChange(RoleChangeRequestDTO request);

    /**
     * Steps 5-6: saves the confirmed change, records it for audit and returns the effective result.
     *
     * @return outcome {@code CONFIRMED}, {@code UNAUTHORIZED} (A1), {@code INVALID} (A2),
     *         {@code SAFEGUARD_REQUIRED} (A4) or {@code SAVE_FAILED} (A3, access not changed)
     */
    RoleChangeResultDTO confirmRoleChange(RoleChangeRequestDTO request);
}
