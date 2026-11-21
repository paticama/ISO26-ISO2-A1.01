package com.sescam.radiodiagnostics.client.security;

import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class AuditFilterFormTest {

    private static AuditFilterForm form(String from, String to) {
        AuditFilterForm form = new AuditFilterForm();
        form.setFrom(from == null ? null : LocalDateTime.parse(from));
        form.setTo(to == null ? null : LocalDateTime.parse(to));
        return form;
    }

    @Test
    void validFormBuildsCriteriaInTheConfiguredZone() {
        AuditFilterForm form = form("2026-11-20T00:00", "2026-11-20T23:59");
        form.setPerformedBy(" admin-1 ");
        form.setAction("");
        form.setResourceKey("patient-42");

        assertThat(form.validate()).isEmpty();
        AuditSearchCriteriaDTO criteria = form.toCriteria(ZoneId.of("Europe/Madrid"));
        assertThat(criteria.from()).isEqualTo(Instant.parse("2026-11-19T23:00:00Z"));
        assertThat(criteria.performedBy()).isEqualTo("admin-1");
        assertThat(criteria.action()).isNull();
        assertThat(criteria.resourceKey()).isEqualTo("patient-42");
    }

    @Test
    void timeRangeIsRequired() {
        assertThat(form(null, null).validate()).containsOnlyKeys("from", "to");
    }

    @Test
    void endMustNotBeBeforeStart() {
        assertThat(form("2026-11-20T10:00", "2026-11-20T09:00").validate())
                .containsEntry("to", "The end must be after the start.");
    }

    @Test
    void rangeLongerThan31DaysIsRejectedBeforeCallingTheServer() {
        assertThat(form("2026-10-01T00:00", "2026-11-20T00:00").validate()).containsKey("to");
        assertThat(form("2026-10-20T00:00", "2026-11-20T00:00").validate()).isEmpty();
    }

    @Test
    void textFiltersAreLimitedInLengthAndCharacters() {
        AuditFilterForm form = form("2026-11-20T00:00", "2026-11-20T01:00");
        form.setPerformedBy("x".repeat(101));
        form.setAction("DROP TABLE;");
        form.setResourceKey("booking.cancellation.min.hours");

        assertThat(form.validate()).containsOnlyKeys("performedBy", "action");
    }
}
