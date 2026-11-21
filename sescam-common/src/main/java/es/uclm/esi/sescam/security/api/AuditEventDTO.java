package es.uclm.esi.sescam.security.api;

import java.time.Instant;
import java.util.Objects;

/**
 * One audit record: actor, action, timestamp, and target or outcome (content defined by
 * UC-ADM-03 and ADR-008). Audit records are never modified or deleted through the system.
 *
 * @param actor     who performed the action
 * @param action    what was done
 * @param timestamp when it happened
 * @param target    affected user, if any
 * @param outcome   result of the action
 */
public record AuditEventDTO(String actor, String action, Instant timestamp, String target, String outcome) {

    public AuditEventDTO {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(outcome, "outcome");
    }
}
