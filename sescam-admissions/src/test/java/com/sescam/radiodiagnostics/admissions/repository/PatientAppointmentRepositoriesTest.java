package com.sescam.radiodiagnostics.admissions.repository;

import com.sescam.radiodiagnostics.admissions.domain.Booking;
import com.sescam.radiodiagnostics.admissions.domain.RescheduleRequest;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Repository queries added by UC-PAT-01. */
class PatientAppointmentRepositoriesTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");
    private static final LocalDateTime TODAY = LocalDateTime.of(2026, 11, 26, 0, 0);

    private static Booking booking(String id, String patientId, LocalDateTime start) {
        SlotDTO slot = new SlotDTO("SLOT-" + id, "SCH-1", "CT-1", "LOC-1", "CT", start, start.plusMinutes(30),
                1, 0, 1L, true);
        return new Booking(id, "ORD-" + id, patientId, "CT", slot, "adm-1", NOW);
    }

    @Test
    void upcomingAppointmentsAreThoseOfThePatientFromTheGivenMomentOn() {
        InMemoryBookingRepository repository = new InMemoryBookingRepository();
        repository.save(booking("B-PAST", "PAT-001", TODAY.minusDays(1).withHour(9)));
        repository.save(booking("B-TODAY", "PAT-001", TODAY.withHour(8)));
        repository.save(booking("B-NEXT", "PAT-001", TODAY.plusDays(3).withHour(9)));
        repository.save(booking("B-OTHER", "PAT-002", TODAY.plusDays(3).withHour(9)));

        List<Booking> upcoming = repository.findUpcomingByPatientId("PAT-001", TODAY);

        // Order is not checked here: the service sorts the appointments chronologically.
        assertEquals(Set.of("B-TODAY", "B-NEXT"),
                upcoming.stream().map(Booking::getBookingId).collect(Collectors.toSet()));
    }

    @Test
    void cancelledAppointmentsAreStillListedSoThePatientSeesTheirStatus() {
        InMemoryBookingRepository repository = new InMemoryBookingRepository();
        Booking cancelled = booking("B-1", "PAT-001", TODAY.plusDays(2));
        cancelled.cancel("Patient request", "PAT-001", NOW);
        repository.save(cancelled);

        assertEquals(1, repository.findUpcomingByPatientId("PAT-001", TODAY).size());
    }

    @Test
    void onlyThePendingRequestOfTheBookingIsFound() {
        InMemoryRescheduleRequestRepository repository = new InMemoryRescheduleRequestRepository();
        RescheduleRequest pending = RescheduleRequest.pending("B-1", "PAT-001", "Work trip", List.of(), NOW);
        repository.save(pending);
        repository.save(RescheduleRequest.pending("B-2", "PAT-001", "Holiday", List.of(), NOW));

        assertEquals(pending.getRequestId(),
                repository.findPendingByBookingId("B-1").orElseThrow().getRequestId());
        assertTrue(repository.findPendingByBookingId("B-3").isEmpty());
    }
}
