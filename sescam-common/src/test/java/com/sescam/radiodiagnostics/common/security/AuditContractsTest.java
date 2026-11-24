package com.sescam.radiodiagnostics.common.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditContractsTest {

    private static final Instant T = Instant.parse("2026-11-20T10:00:00Z");

    @Test
    void searchResultIsImmutableAndNeverNull() {
        List<AuditRecordDTO> events = new ArrayList<>();
        events.add(new AuditRecordDTO("id-1", 1, T, "admin-1", "LOGIN", null, null, null, null));
        AuditSearchCriteriaDTO criteria = new AuditSearchCriteriaDTO(T, T, null, null, null);

        AuditSearchResultDTO result = new AuditSearchResultDTO(criteria, AuditResultStatus.MATCHES_FOUND, 1, events);
        events.clear();

        assertThat(result.events()).hasSize(1);
        assertThatThrownBy(() -> result.events().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(new AuditSearchResultDTO(criteria, AuditResultStatus.NO_MATCHES, 0, null).events()).isEmpty();
    }

    @Test
    void exceptionsCarryTheInformationTheClientNeeds() {
        AuditQueryTooBroadException tooBroad = new AuditQueryTooBroadException("too broad", 31, 60);
        assertThat(tooBroad.getLimit()).isEqualTo(31);
        assertThat(tooBroad.getActual()).isEqualTo(60);

        IllegalStateException cause = new IllegalStateException("down");
        assertThat(new AuditQueryFailedException("failed", cause)).hasCause(cause);
        assertThat(new InvalidAuditQueryException("invalid")).hasMessage("invalid");
        assertThat(new UnauthorizedAuditAccessException("denied")).hasMessage("denied");
    }
}
