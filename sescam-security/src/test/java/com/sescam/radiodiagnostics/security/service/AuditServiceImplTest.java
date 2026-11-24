package com.sescam.radiodiagnostics.security.service;

import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;
import com.sescam.radiodiagnostics.security.domain.AuditEvent;
import com.sescam.radiodiagnostics.security.repository.InMemoryAuditEventRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditServiceImplTest {

    private static final Instant T = Instant.parse("2026-11-20T10:00:00Z");

    @Test
    void roleAdministrationEventsAreStoredInTheSameTrail() {
        InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository();
        AuditServiceImpl service = new AuditServiceImpl(repository, Clock.fixed(T, ZoneOffset.UTC));

        service.record(new es.uclm.esi.sescam.security.api.AuditEventDTO(
                "admin", "ROLE_CHANGED", T, "ana", "CONFIRMED: [Patient] -> [Physician]"));

        assertThat(repository.findMatching(new AuditSearchCriteriaDTO(T, T, null, null, null)))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getActorId()).isEqualTo("admin");
                    assertThat(event.getAction()).isEqualTo("ROLE_CHANGED");
                    assertThat(event.getResourceKey()).isEqualTo("ana");
                    assertThat(event.getDetails()).isEqualTo("CONFIRMED: [Patient] -> [Physician]");
                    assertThat(event.getTimestamp()).isEqualTo(T);
                })
                .extracting(AuditEvent::getSequenceNumber).isEqualTo(1L);
        assertThatThrownBy(() -> service.record(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
