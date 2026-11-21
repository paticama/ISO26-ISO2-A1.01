package es.uclm.esi.sescam.security.internal;

import es.uclm.esi.sescam.security.api.AuditEventDTO;
import es.uclm.esi.sescam.security.api.AuditService;

/** Audit implementation used until UC-ADM-03 provides the real one. It records nothing. */
public class NoOpAuditService implements AuditService {

    @Override
    public void record(AuditEventDTO event) {
        // intentionally empty: replaced by the UC-ADM-03 implementation
    }
}
