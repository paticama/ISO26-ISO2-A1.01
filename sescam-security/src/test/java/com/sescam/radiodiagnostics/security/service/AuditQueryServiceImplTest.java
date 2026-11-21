package com.sescam.radiodiagnostics.security.service;

import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditQueryFailedException;
import com.sescam.radiodiagnostics.common.security.AuditQueryTooBroadException;
import com.sescam.radiodiagnostics.common.security.AuditRecordDTO;
import com.sescam.radiodiagnostics.common.security.AuditResultStatus;
import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.security.AuditSearchResultDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.common.security.InvalidAuditQueryException;
import com.sescam.radiodiagnostics.common.security.UnauthorizedAuditAccessException;
import com.sescam.radiodiagnostics.security.internal.AuditAccessPolicy;
import com.sescam.radiodiagnostics.security.repository.AuditEventRepository;
import com.sescam.radiodiagnostics.security.repository.InMemoryAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditQueryServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static final Instant DAY_START = Instant.parse("2026-11-20T00:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private InMemoryAuditEventRepository repository;
    private AuditServiceImpl auditService;
    private AuditQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAuditEventRepository();
        auditService = new AuditServiceImpl(repository, clock);
        queryService = new AuditQueryServiceImpl(repository, auditService, new AuditAccessPolicy(), clock);

        // Recorded out of order on purpose, to check that results come back time-ordered.
        record("CONFIGURATION_PARAMETER_UPDATED", "admin-1", "booking.cancellation.min.hours",
                "24", "48", "08:30");
        record("CLINICAL_DATA_ACCESSED", "physician-7", "patient-42", null, null, "07:15");
        record("ROLE_ASSIGNMENT_CHANGED", "admin-2", "user-9", "PATIENT", "PHYSICIAN", "10:00");
        record("CLINICAL_DATA_ACCESSED", "physician-7", "patient-43", null, null, "2026-11-10T09:00:00Z");
    }

    private void record(String action, String actor, String resource, String before, String after, String time) {
        Instant ts = time.length() == 5 ? Instant.parse("2026-11-20T" + time + ":00Z") : Instant.parse(time);
        auditService.recordEvent(new AuditEventDTO(action, actor, resource, before, after, null, ts));
    }

    private static AuditSearchCriteriaDTO today() {
        return new AuditSearchCriteriaDTO(DAY_START, NOW, null, null, null);
    }

    @Nested
    @DisplayName("Acceptance criteria")
    class AcceptanceCriteria {

        @Test
        @DisplayName("AC1: an authorized actor searches a time range; results are time-ordered")
        void authorizedActorSearchesTimeRange() {
            AuditSearchResultDTO result = queryService.searchEvents(today(), "admin-1", "ADMINISTRATOR");

            assertThat(result.status()).isEqualTo(AuditResultStatus.MATCHES_FOUND);
            assertThat(result.totalMatches()).isEqualTo(3);
            assertThat(result.events()).extracting(AuditRecordDTO::timestamp).isSorted();
            assertThat(result.events()).extracting(AuditRecordDTO::action).containsExactly(
                    "CLINICAL_DATA_ACCESSED", "CONFIGURATION_PARAMETER_UPDATED", "ROLE_ASSIGNMENT_CHANGED");
        }

        @Test
        @DisplayName("AC1: filters by actor, action and target combine with the time range")
        void filtersNarrowTheResult() {
            AuditSearchCriteriaDTO criteria = new AuditSearchCriteriaDTO(
                    Instant.parse("2026-11-01T00:00:00Z"), NOW, "physician-7", "clinical_data_accessed", null);

            AuditSearchResultDTO result = queryService.searchEvents(criteria, "auditor-1", "COMPLIANCE_REVIEWER");

            assertThat(result.events()).extracting(AuditRecordDTO::resourceKey)
                    .containsExactly("patient-43", "patient-42");

            AuditSearchResultDTO byTarget = queryService.searchEvents(
                    new AuditSearchCriteriaDTO(DAY_START, NOW, null, null, "user-9"), "admin-1", "ADMIN");
            assertThat(byTarget.events()).singleElement()
                    .extracting(AuditRecordDTO::action).isEqualTo("ROLE_ASSIGNMENT_CHANGED");
        }

        @Test
        @DisplayName("AC2: each result shows actor, action, timestamp, target and recorded values")
        void resultsIdentifyActorActionTimestampAndTarget() {
            AuditRecordDTO event = queryService.searchEvents(
                    new AuditSearchCriteriaDTO(DAY_START, NOW, "admin-1", null, null),
                    "admin-1", "ADMINISTRATOR").events().get(0);

            assertThat(event.eventId()).isNotBlank();
            assertThat(event.actorId()).isEqualTo("admin-1");
            assertThat(event.action()).isEqualTo("CONFIGURATION_PARAMETER_UPDATED");
            assertThat(event.timestamp()).isEqualTo(Instant.parse("2026-11-20T08:30:00Z"));
            assertThat(event.resourceKey()).isEqualTo("booking.cancellation.min.hours");
            assertThat(event.previousValue()).isEqualTo("24");
            assertThat(event.newValue()).isEqualTo("48");
        }

        @Test
        @DisplayName("AC3: unauthorized actors cannot access audit records and the attempt is audited")
        void unauthorizedActorIsDenied() {
            assertThatThrownBy(() -> queryService.searchEvents(today(), "patient-1", "PATIENT"))
                    .isInstanceOf(UnauthorizedAuditAccessException.class);

            AuditSearchResultDTO denied = queryService.searchEvents(
                    new AuditSearchCriteriaDTO(DAY_START, NOW, null, AuditQueryServiceImpl.ACTION_ACCESS_DENIED, null),
                    "admin-1", "ADMINISTRATOR");
            assertThat(denied.events()).singleElement()
                    .extracting(AuditRecordDTO::actorId).isEqualTo("patient-1");
        }

        @Test
        @DisplayName("AC4: an empty result is reported with NO_MATCHES, failures with an exception")
        void emptyResultAndFailureAreDistinguishable() {
            AuditSearchResultDTO empty = queryService.searchEvents(
                    new AuditSearchCriteriaDTO(DAY_START, NOW, "nobody", null, null), "admin-1", "ADMINISTRATOR");
            assertThat(empty.status()).isEqualTo(AuditResultStatus.NO_MATCHES);
            assertThat(empty.events()).isEmpty();

            AuditEventRepository broken = mock(AuditEventRepository.class);
            when(broken.findMatching(any())).thenThrow(new IllegalStateException("store down"));
            AuditQueryServiceImpl failing =
                    new AuditQueryServiceImpl(broken, auditService, new AuditAccessPolicy(), clock);
            assertThatThrownBy(() -> failing.searchEvents(today(), "admin-1", "ADMINISTRATOR"))
                    .isInstanceOf(AuditQueryFailedException.class)
                    .hasCauseInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("AC5: searching and exporting do not alter the stored records")
        void searchingDoesNotAlterRecords() {
            List<AuditRecordDTO> before = queryService.searchEvents(today(), "admin-1", "ADMINISTRATOR").events();

            queryService.searchEvents(today(), "admin-1", "ADMINISTRATOR");
            queryService.exportEventsAsCsv(today(), "admin-1", "ADMINISTRATOR");

            AuditSearchCriteriaDTO originalOnly = new AuditSearchCriteriaDTO(DAY_START, NOW, null, null, null);
            List<AuditRecordDTO> after = queryService.searchEvents(originalOnly, "admin-1", "ADMINISTRATOR")
                    .events().stream().filter(e -> !"audit-trail".equals(e.resourceKey())).toList();
            assertThat(after).containsExactlyElementsOf(before);
            assertThatThrownBy(() -> before.add(before.get(0))).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Alternative and exception flows")
    class AlternativeFlows {

        @Test
        @DisplayName("A1: missing identity or role is treated as unauthorized")
        void missingIdentityIsUnauthorized() {
            assertThatThrownBy(() -> queryService.searchEvents(today(), "", "ADMINISTRATOR"))
                    .isInstanceOf(UnauthorizedAuditAccessException.class);
            assertThatThrownBy(() -> queryService.exportEventsAsCsv(today(), "admin-1", null))
                    .isInstanceOf(UnauthorizedAuditAccessException.class);
        }

        @Test
        @DisplayName("A2: no matching records in the time range")
        void noMatchingRecords() {
            AuditSearchCriteriaDTO lastYear = new AuditSearchCriteriaDTO(
                    Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-02T00:00:00Z"), null, null, null);

            AuditSearchResultDTO result = queryService.searchEvents(lastYear, "admin-1", "ADMINISTRATOR");

            assertThat(result.status()).isEqualTo(AuditResultStatus.NO_MATCHES);
            assertThat(result.totalMatches()).isZero();
        }

        @Test
        @DisplayName("A3: missing or inverted time range is rejected")
        void invalidTimeRange() {
            assertThatThrownBy(() -> queryService.searchEvents(
                    new AuditSearchCriteriaDTO(null, NOW, null, null, null), "admin-1", "ADMINISTRATOR"))
                    .isInstanceOf(InvalidAuditQueryException.class);
            assertThatThrownBy(() -> queryService.searchEvents(null, "admin-1", "ADMINISTRATOR"))
                    .isInstanceOf(InvalidAuditQueryException.class);
            assertThatThrownBy(() -> queryService.searchEvents(
                    new AuditSearchCriteriaDTO(NOW, DAY_START, null, null, null), "admin-1", "ADMINISTRATOR"))
                    .isInstanceOf(InvalidAuditQueryException.class)
                    .hasMessageContaining("must not be after");
        }

        @Test
        @DisplayName("A3: a time range longer than the maximum is too broad")
        void timeRangeTooBroad() {
            AuditSearchCriteriaDTO wide = new AuditSearchCriteriaDTO(NOW.minus(Duration.ofDays(60)), NOW,
                    null, null, null);

            assertThatThrownBy(() -> queryService.searchEvents(wide, "admin-1", "ADMINISTRATOR"))
                    .isInstanceOfSatisfying(AuditQueryTooBroadException.class, ex -> {
                        assertThat(ex.getLimit()).isEqualTo(31);
                        assertThat(ex.getActual()).isEqualTo(60);
                    });
        }

        @Test
        @DisplayName("A3: too many matches asks the actor to refine the search")
        void tooManyMatches() {
            AuditQueryServiceImpl strict = new AuditQueryServiceImpl(repository, auditService,
                    new AuditAccessPolicy(), clock, Duration.ofDays(31), 2);

            assertThatThrownBy(() -> strict.searchEvents(today(), "admin-1", "ADMINISTRATOR"))
                    .isInstanceOfSatisfying(AuditQueryTooBroadException.class, ex -> {
                        assertThat(ex.getLimit()).isEqualTo(2);
                        assertThat(ex.getActual()).isEqualTo(3);
                    });
        }

        @Test
        @DisplayName("A3: if the access cannot be audited, the result is not returned")
        void accessThatCannotBeAuditedFails() {
            AuditService failingAudit = mock(AuditService.class);
            doThrow(new IllegalStateException("audit down")).when(failingAudit).recordEvent(any());
            AuditQueryServiceImpl service =
                    new AuditQueryServiceImpl(repository, failingAudit, new AuditAccessPolicy(), clock);

            assertThatThrownBy(() -> service.searchEvents(today(), "admin-1", "ADMINISTRATOR"))
                    .isInstanceOf(AuditQueryFailedException.class);
        }
    }

    @Nested
    @DisplayName("Step 5: export and audit of the audit")
    class ExportAndTraceability {

        @Test
        @DisplayName("Export returns a CSV with a header and one escaped line per event")
        void exportCsv() {
            auditService.recordEvent(new AuditEventDTO("NOTE", "admin-1", "x", null, null,
                    "contains, comma and \"quotes\"", Instant.parse("2026-11-20T11:00:00Z")));

            String csv = queryService.exportEventsAsCsv(today(), "admin-1", "ADMINISTRATOR");

            String[] lines = csv.split("\n");
            assertThat(lines[0]).startsWith("eventId,sequenceNumber,timestamp,actorId,action");
            assertThat(lines).hasSize(5);
            assertThat(csv).contains("\"contains, comma and \"\"quotes\"\"\"");
        }

        @Test
        @DisplayName("Each search and export is recorded as a new audit event")
        void searchIsAudited() {
            AuditService spyAudit = mock(AuditService.class);
            AuditQueryServiceImpl service =
                    new AuditQueryServiceImpl(repository, spyAudit, new AuditAccessPolicy(), clock);

            service.searchEvents(today(), "admin-1", "ADMINISTRATOR");

            ArgumentCaptor<AuditEventDTO> captor = ArgumentCaptor.forClass(AuditEventDTO.class);
            verify(spyAudit).recordEvent(captor.capture());
            assertThat(captor.getValue().action()).isEqualTo(AuditQueryServiceImpl.ACTION_QUERIED);
            assertThat(captor.getValue().actorId()).isEqualTo("admin-1");
            assertThat(captor.getValue().details()).contains("matches=3");
            assertThat(captor.getValue().timestamp()).isEqualTo(NOW);
        }
    }
}
