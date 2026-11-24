package com.sescam.radiodiagnostics.common.security;

import java.util.List;

/**
 * Result of an audit search: time-ordered events plus an explicit status (UC-ADM-03).
 */
public record AuditSearchResultDTO(
        AuditSearchCriteriaDTO criteria,
        AuditResultStatus status,
        int totalMatches,
        List<AuditRecordDTO> events
) {
    public AuditSearchResultDTO {
        events = events == null ? List.of() : List.copyOf(events);
    }
}
