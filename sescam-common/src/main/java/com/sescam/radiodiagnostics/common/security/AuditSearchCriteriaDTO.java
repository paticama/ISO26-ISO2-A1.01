package com.sescam.radiodiagnostics.common.security;

import java.time.Instant;

/**
 * Filters of an audit search (UC-ADM-03). {@code from} and {@code to} are mandatory;
 * the other filters are optional (null or blank means "any").
 *
 * @param from        start of the time range (inclusive)
 * @param to          end of the time range (inclusive)
 * @param performedBy actor who performed the audited action
 * @param action      audited action, e.g. {@code CONFIGURATION_PARAMETER_UPDATED}
 * @param resourceKey affected resource (target), e.g. a parameter key or a patient id
 */
public record AuditSearchCriteriaDTO(
        Instant from,
        Instant to,
        String performedBy,
        String action,
        String resourceKey
) {
}
