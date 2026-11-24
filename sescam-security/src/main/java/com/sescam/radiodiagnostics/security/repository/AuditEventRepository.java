package com.sescam.radiodiagnostics.security.repository;

import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;
import com.sescam.radiodiagnostics.security.domain.AuditEvent;

import java.util.List;

/**
 * Append-only store of audit events. On purpose it has no update or delete operation (ADR-008).
 */
public interface AuditEventRepository {

    /** Next sequence number, used to keep a stable order between events with the same timestamp. */
    long nextSequenceNumber();

    void append(AuditEvent event);

    /** Events that match the criteria, ordered by timestamp and then by sequence number. */
    List<AuditEvent> findMatching(AuditSearchCriteriaDTO criteria);

    long count();
}
