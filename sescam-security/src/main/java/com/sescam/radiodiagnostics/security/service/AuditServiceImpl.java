package com.sescam.radiodiagnostics.security.service;

import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.security.domain.AuditEvent;
import com.sescam.radiodiagnostics.security.repository.AuditEventRepository;

import java.time.Clock;
import java.util.Objects;

/**
 * C1 implementation of the audit contracts: every module records its auditable events here (NFR-11).
 * It implements both {@link AuditService} (used by catalogue, UC-ADM-01) and
 * {@link es.uclm.esi.sescam.security.api.AuditService} (used by role administration, UC-ADM-02),
 * so all events end up in the same append-only trail that UC-ADM-03 searches.
 * It replaces the no-op audit services the other modules used while UC-ADM-03 was not built.
 */
public class AuditServiceImpl implements AuditService, es.uclm.esi.sescam.security.api.AuditService {

    private final AuditEventRepository repository;
    private final Clock clock;

    public AuditServiceImpl(AuditEventRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void recordEvent(AuditEventDTO event) {
        repository.append(AuditEvent.from(event, repository.nextSequenceNumber(), clock.instant()));
    }

    /**
     * UC-ADM-02 events: {@code target} is stored as the affected resource and {@code outcome} as the details.
     */
    @Override
    public void record(es.uclm.esi.sescam.security.api.AuditEventDTO event) {
        if (event == null) {
            throw new IllegalArgumentException("Audit event must not be null");
        }
        recordEvent(new AuditEventDTO(event.action(), event.actor(), event.target(), null, null,
                event.outcome(), event.timestamp()));
    }
}
