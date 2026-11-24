package es.uclm.esi.sescam.security.api;

import java.util.Objects;

/**
 * Input of a role change: authenticated administrator identity, target user account and
 * requested role assignment or change (inputs listed in the UC-ADM-02 specification).
 *
 * @param administratorId identifier of the authenticated administrator making the request
 * @param targetUserId    identifier of the user account whose assignment changes
 * @param roleName        name of the requested role
 */
public record RoleChangeRequestDTO(String administratorId, String targetUserId, String roleName) {

    public RoleChangeRequestDTO {
        Objects.requireNonNull(administratorId, "administratorId");
        Objects.requireNonNull(targetUserId, "targetUserId");
        Objects.requireNonNull(roleName, "roleName");
    }
}
