package es.uclm.esi.sescam.security.api;

/**
 * Result of validating or confirming a role change (UC-ADM-02).
 * One value per flow of the use case specification.
 */
public enum ChangeOutcome {

    /** Step 4: the requested change passed validation; nothing has been saved yet. */
    VALID,

    /** Step 6: the change was saved and its effective result is returned. */
    CONFIRMED,

    /** A1: the requester lacks the permission to manage roles and permissions. Assignment unchanged. */
    UNAUTHORIZED,

    /**
     * A2: invalid role or access rule (unknown role, disallowed change, or a change that would
     * leave the user without a role). Assignment unchanged.
     */
    INVALID,

    /** A4: own privileges or a designated sensitive assignment; the applicable safeguards are required. */
    SAFEGUARD_REQUIRED,

    /** A3: the assignment could not be saved. Access has NOT changed. */
    SAVE_FAILED
}
