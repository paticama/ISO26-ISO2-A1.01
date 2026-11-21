package es.uclm.esi.sescam.security.api;

import java.util.Objects;

/**
 * A role as shown to clients (UC-ADM-02).
 *
 * @param name unique role name
 */
public record RoleDTO(String name) {

    public RoleDTO {
        Objects.requireNonNull(name, "name");
    }
}
