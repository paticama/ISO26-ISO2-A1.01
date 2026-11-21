package es.uclm.esi.sescam.security.api;

import java.util.List;
import java.util.Objects;

/**
 * Validation or save result of a role change, with the resulting access (outputs listed in the
 * UC-ADM-02 specification).
 *
 * @param outcome              what happened, see {@link ChangeOutcome}
 * @param message              explanation shown to the administrator (A2, A3, A4) or confirmation text
 * @param roles                roles of the user after the change ({@code VALID}, {@code CONFIRMED}); the
 *                             unchanged current roles on {@code INVALID}, {@code SAFEGUARD_REQUIRED} and
 *                             {@code SAVE_FAILED}; empty on {@code UNAUTHORIZED} so nothing is disclosed
 * @param effectivePermissions permission names that result from {@code roles}
 */
public record RoleChangeResultDTO(ChangeOutcome outcome, String message, List<RoleDTO> roles,
                                  List<String> effectivePermissions) {

    public RoleChangeResultDTO {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(message, "message");
        roles = List.copyOf(Objects.requireNonNull(roles, "roles"));
        effectivePermissions = List.copyOf(Objects.requireNonNull(effectivePermissions, "effectivePermissions"));
    }
}
