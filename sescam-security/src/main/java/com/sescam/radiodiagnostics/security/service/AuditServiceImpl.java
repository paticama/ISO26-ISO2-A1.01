package com.sescam.radiodiagnostics.security.service;

import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.security.domain.AuditEvent;
import com.sescam.radiodiagnostics.security.repository.AuditEventRepository;

import java.time.Clock;
import java.util.Objects;

/**
 * C1 implementation of {@link AuditService}: every module records its auditable events here (NFR-11).
 * It replaces the no-op audit services the other modules use while C1 is not built.
 */
public class AuditServiceImpl implements AuditService {

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
}
