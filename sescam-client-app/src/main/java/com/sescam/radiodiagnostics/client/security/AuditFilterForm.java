package com.sescam.radiodiagnostics.client.security;

import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Audit search form of the UC-ADM-03 view. It validates the fields before calling the server,
 * with the same limits the server applies, so the actor gets the message without a round trip.
 */
public class AuditFilterForm {

    public static final Duration MAX_RANGE = Duration.ofDays(31);
    static final int MAX_FILTER_LENGTH = 100;
    private static final Pattern FILTER_PATTERN = Pattern.compile("[A-Za-z0-9._:@-]*");

    private LocalDateTime from;
    private LocalDateTime to;
    private String performedBy;
    private String action;
    private String resourceKey;

    /** Field name → error message; empty when the form is valid. */
    public Map<String, String> validate() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (from == null) {
            errors.put("from", "Enter the start of the time range.");
        }
        if (to == null) {
            errors.put("to", "Enter the end of the time range.");
        }
        if (from != null && to != null) {
            if (from.isAfter(to)) {
                errors.put("to", "The end must be after the start.");
            } else if (Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
                errors.put("to", "The time range cannot exceed " + MAX_RANGE.toDays() + " days.");
            }
        }
        checkFilter(errors, "performedBy", performedBy);
        checkFilter(errors, "action", action);
        checkFilter(errors, "resourceKey", resourceKey);
        return errors;
    }

    private static void checkFilter(Map<String, String> errors, String field, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_FILTER_LENGTH) {
            errors.put(field, "Use at most " + MAX_FILTER_LENGTH + " characters.");
        } else if (!FILTER_PATTERN.matcher(trimmed).matches()) {
            errors.put(field, "Use only letters, digits and . _ : @ -");
        }
    }

    /** Builds the criteria sent to the server. Call only on a valid form. */
    public AuditSearchCriteriaDTO toCriteria(ZoneId zone) {
        return new AuditSearchCriteriaDTO(from.atZone(zone).toInstant(), to.atZone(zone).toInstant(),
                blankToNull(performedBy), blankToNull(action), blankToNull(resourceKey));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public void setFrom(LocalDateTime from) {
        this.from = from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public void setTo(LocalDateTime to) {
        this.to = to;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }
}
