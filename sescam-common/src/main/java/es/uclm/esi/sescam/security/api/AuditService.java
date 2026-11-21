package es.uclm.esi.sescam.security.api;

/**
 * Records audit events. Implemented by UC-ADM-03; until it exists, a no-op implementation is used.
 * UC-ADM-02 only needs {@link #record(AuditEventDTO)} so every successful role change is
 * attributable and available for audit.
 */
public interface AuditService {

    /**
     * Stores an audit record. Records cannot be modified or deleted afterwards.
     *
     * @param event the event to record
     */
    void record(AuditEventDTO event);
}
