package com.sescam.radiodiagnostics.common.security;

/**
 * Read-only port of C1 (sescam-security) for investigating audit records (UC-ADM-03,
 * FR-ADM-11..15). Implementations must never modify or delete existing audit records.
 */
public interface AuditQueryService {

    /**
     * Returns the audit events that match the criteria, ordered by timestamp (oldest first).
     *
     * @throws UnauthorizedAuditAccessException if the requester may not view audit records (A1)
     * @throws InvalidAuditQueryException       if the criteria are incomplete or inconsistent (A3)
     * @throws AuditQueryTooBroadException      if the time range or the number of matches is too large (A3)
     * @throws AuditQueryFailedException        if the audit store cannot be queried (A3)
     */
    AuditSearchResultDTO searchEvents(AuditSearchCriteriaDTO criteria, String requesterId, String requesterRole);

    /**
     * Same search as {@link #searchEvents}, exported as CSV (main scenario, step 5).
     */
    String exportEventsAsCsv(AuditSearchCriteriaDTO criteria, String requesterId, String requesterRole);
}
