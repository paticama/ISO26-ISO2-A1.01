package es.uclm.esi.sescam.security.api;

import java.util.List;
import java.util.Objects;

/**
 * Current role assignments and relevant account status of a user (steps 1-2 of UC-ADM-02).
 *
 * @param userId        identifier of the target user account
 * @param accountStatus relevant account status
 * @param roles         roles currently assigned to the user (at least one)
 */
public record RoleAssignmentDTO(String userId, String accountStatus, List<RoleDTO> roles) {

    public RoleAssignmentDTO {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(accountStatus, "accountStatus");
        roles = List.copyOf(Objects.requireNonNull(roles, "roles"));
    }
}
