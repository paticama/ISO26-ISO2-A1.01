package es.uclm.esi.sescam.security.api;

/**
 * A1 of UC-ADM-02: the requester lacks the permission to manage roles and permissions.
 * The existing assignment is not changed.
 */
public class AccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException(String message) {
        super(message);
    }
}
