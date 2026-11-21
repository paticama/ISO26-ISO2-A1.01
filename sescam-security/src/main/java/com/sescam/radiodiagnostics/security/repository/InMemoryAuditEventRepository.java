package com.sescam.radiodiagnostics.security.repository;

import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;
import com.sescam.radiodiagnostics.security.domain.AuditEvent;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory append-only implementation, used until a persistent store is agreed.
 */
public class InMemoryAuditEventRepository implements AuditEventRepository {

    private static final Comparator<AuditEvent> CHRONOLOGICAL =
            Comparator.comparing(AuditEvent::getTimestamp).thenComparingLong(AuditEvent::getSequenceNumber);

    private final List<AuditEvent> events = new CopyOnWriteArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public long nextSequenceNumber() {
        return sequence.incrementAndGet();
    }

    @Override
    public void append(AuditEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Audit event must not be null");
        }
        events.add(event);
    }

    @Override
    public List<AuditEvent> findMatching(AuditSearchCriteriaDTO criteria) {
        return events.stream()
                .filter(event -> event.matches(criteria))
                .sorted(CHRONOLOGICAL)
                .toList();
    }

    @Override
    public long count() {
        return events.size();
    }
}
