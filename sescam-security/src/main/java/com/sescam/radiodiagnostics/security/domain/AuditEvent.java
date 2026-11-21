package com.sescam.radiodiagnostics.security.domain;

import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditRecordDTO;
import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit record owned by C1 (NFR-11, ADR-008). There are no setters and no way to
 * change an event once it is created: audit records cannot be modified or deleted through the system.
 */
public final class AuditEvent {

    private final String eventId;
    private final long sequenceNumber;
    private final Instant timestamp;
    private final String actorId;
    private final String action;
    private final String resourceKey;
    private final String previousValue;
    private final String newValue;
    private final String details;

    private AuditEvent(String eventId, long sequenceNumber, Instant timestamp, String actorId, String action,
                       String resourceKey, String previousValue, String newValue, String details) {
        this.eventId = eventId;
        this.sequenceNumber = sequenceNumber;
        this.timestamp = timestamp;
        this.actorId = actorId;
        this.action = action;
        this.resourceKey = resourceKey;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.details = details;
    }

    /**
     * Creates an event from the DTO sent by any module. If the DTO has no timestamp, {@code recordedAt} is used.
     *
     * @throws IllegalArgumentException if the DTO is null or has no actor or action
     */
    public static AuditEvent from(AuditEventDTO dto, long sequenceNumber, Instant recordedAt) {
        if (dto == null) {
            throw new IllegalArgumentException("Audit event must not be null");
        }
        if (isBlank(dto.action())) {
            throw new IllegalArgumentException("Audit event must have an action");
        }
        if (isBlank(dto.actorId())) {
            throw new IllegalArgumentException("Audit event must have an actor");
        }
        Instant timestamp = dto.timestamp() != null ? dto.timestamp() : Objects.requireNonNull(recordedAt);
        return new AuditEvent(UUID.randomUUID().toString(), sequenceNumber, timestamp, dto.actorId().trim(),
                dto.action().trim(), trimToNull(dto.resourceKey()), dto.previousValue(), dto.newValue(),
                dto.details());
    }

    /**
     * True when the event falls inside the time range (both ends inclusive) and matches every non-blank filter.
     */
    public boolean matches(AuditSearchCriteriaDTO criteria) {
        if (criteria.from() != null && timestamp.isBefore(criteria.from())) {
            return false;
        }
        if (criteria.to() != null && timestamp.isAfter(criteria.to())) {
            return false;
        }
        return matchesFilter(criteria.performedBy(), actorId)
                && matchesFilter(criteria.action(), action)
                && matchesFilter(criteria.resourceKey(), resourceKey);
    }

    public AuditRecordDTO toRecord() {
        return new AuditRecordDTO(eventId, sequenceNumber, timestamp, actorId, action, resourceKey,
                previousValue, newValue, details);
    }

    private static boolean matchesFilter(String filter, String value) {
        return isBlank(filter) || filter.trim().equalsIgnoreCase(value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    public String getEventId() {
        return eventId;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getDetails() {
        return details;
    }
}
