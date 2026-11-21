package com.sescam.radiodiagnostics.security.domain;

import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditEventTest {

    private static final Instant T = Instant.parse("2026-11-20T10:00:00Z");

    @Test
    void usesRecordingTimeWhenDtoHasNoTimestamp() {
        AuditEvent event = AuditEvent.from(new AuditEventDTO(" LOGIN ", " u1 ", "  ", null, null, null, null), 7, T);

        assertThat(event.getTimestamp()).isEqualTo(T);
        assertThat(event.getAction()).isEqualTo("LOGIN");
        assertThat(event.getActorId()).isEqualTo("u1");
        assertThat(event.getResourceKey()).isNull();
        assertThat(event.getSequenceNumber()).isEqualTo(7);
        assertThat(event.toRecord().eventId()).isEqualTo(event.getEventId());
    }

    @Test
    void rejectsIncompleteEvents() {
        assertThatThrownBy(() -> AuditEvent.from(null, 1, T)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditEvent.from(new AuditEventDTO(" ", "u1", null, null, null, null, T), 1, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditEvent.from(new AuditEventDTO("LOGIN", null, null, null, null, null, T), 1, T))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void timeRangeIsInclusiveAtBothEnds() {
        AuditEvent event = AuditEvent.from(new AuditEventDTO("LOGIN", "u1", "r", "a", "b", "d", T), 1, T);

        assertThat(event.matches(new AuditSearchCriteriaDTO(T, T, null, null, null))).isTrue();
        assertThat(event.matches(new AuditSearchCriteriaDTO(T.plusSeconds(1), null, null, null, null))).isFalse();
        assertThat(event.matches(new AuditSearchCriteriaDTO(null, T.minusSeconds(1), null, null, null))).isFalse();
        assertThat(event.matches(new AuditSearchCriteriaDTO(T, T, "U1", "login", "R"))).isTrue();
        assertThat(event.matches(new AuditSearchCriteriaDTO(T, T, null, null, "other"))).isFalse();
        assertThat(event.getPreviousValue()).isEqualTo("a");
        assertThat(event.getNewValue()).isEqualTo("b");
        assertThat(event.getDetails()).isEqualTo("d");
    }
}
