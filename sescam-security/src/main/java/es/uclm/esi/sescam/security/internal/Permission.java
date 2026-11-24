package es.uclm.esi.sescam.security.internal;

import java.util.Objects;

/** One capability of the system that a {@link Role} can grant. */
public record Permission(String name) {

    public Permission {
        Objects.requireNonNull(name, "name");
    }
}
