package com.sescam.radiodiagnostics.catalogue.internal;

import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory fallback audit recorder used until C1 (sescam-security) AuditService is wired.
 */
public class NoOpAuditService implements AuditService {

    private final List<AuditEventDTO> recordedEvents = new CopyOnWriteArrayList<>();

    @Override
    public void recordEvent(AuditEventDTO event) {
        if (event != null) {
            recordedEvents.add(event);
        }
    }

    public List<AuditEventDTO> getRecordedEvents() {
        return Collections.unmodifiableList(recordedEvents);
    }
}
