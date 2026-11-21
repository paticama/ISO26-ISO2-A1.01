package com.sescam.radiodiagnostics.common.security;

/**
 * Outbound port to C1 (sescam-security) for recording auditable events (NFR-11).
 */
public interface AuditService {

    void recordEvent(AuditEventDTO event);
}
