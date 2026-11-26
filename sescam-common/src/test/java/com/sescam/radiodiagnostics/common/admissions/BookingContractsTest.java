package com.sescam.radiodiagnostics.common.admissions;

import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingContractsTest {

    @Test
    void exceptionsCarryExpectedErrorCodesAndPayloads() {
        var invalid = new InvalidBookingDataException("bad", List.of("orderId"));
        assertEquals(BookingErrorCode.INVALID_BOOKING_DATA, invalid.getCode());
        assertEquals(List.of("orderId"), invalid.getInvalidFields());

        assertEquals(BookingErrorCode.UNAUTHORIZED_BOOKING_ACCESS,
                new UnauthorizedBookingAccessException("denied").getCode());
        assertEquals(BookingErrorCode.BOOKING_NOT_FOUND,
                new BookingNotFoundException("missing").getCode());
        assertEquals(BookingErrorCode.REQUEST_ALREADY_PROCESSED,
                new RequestAlreadyProcessedException("booked").getCode());
        assertEquals(BookingErrorCode.PATIENT_NOT_ELIGIBLE,
                new PatientNotEligibleException("discharged").getCode());
        assertEquals(BookingErrorCode.BOOKING_CONFLICT,
                new BookingConflictException("conflict").getCode());
        assertEquals(BookingErrorCode.CONCURRENT_BOOKING,
                new ConcurrentBookingException("stale").getCode());

        var slot = new SlotDTO("S1", "SCH-1", "CT-1", "LOC-1", "CT",
                LocalDateTime.of(2026, 12, 1, 9, 0), LocalDateTime.of(2026, 12, 1, 9, 30),
                2, 0, 1L, true);
        var unavailable = new SlotUnavailableBookingException("full", List.of(slot));
        assertEquals(BookingErrorCode.SLOT_UNAVAILABLE, unavailable.getCode());
        assertEquals(1, unavailable.getAlternativeSlots().size());
    }
}
