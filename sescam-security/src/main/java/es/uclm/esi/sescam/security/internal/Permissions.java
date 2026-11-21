package es.uclm.esi.sescam.security.internal;

/** Well-known permissions used by the access-control rules of UC-ADM-02. */
public final class Permissions {

    /** Permission to manage roles and permissions; the System administrator role always includes it. */
    public static final Permission MANAGE_ROLES_AND_PERMISSIONS = new Permission("MANAGE_ROLES_AND_PERMISSIONS");

    private Permissions() {
    }
}
