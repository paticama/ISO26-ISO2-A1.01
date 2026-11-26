package com.sescam.radiodiagnostics.common.admissions;

import com.sescam.radiodiagnostics.common.schedules.SlotDTO;

import java.util.List;

/** Raised when the requested slot is occupied or closed (A1, HTTP 409), returning alternative slots. */
public class SlotUnavailableBookingException extends BookingException {

    private final List<SlotDTO> alternativeSlots;

    public SlotUnavailableBookingException(String message, List<SlotDTO> alternativeSlots) {
        super(BookingErrorCode.SLOT_UNAVAILABLE, message);
        this.alternativeSlots = alternativeSlots == null ? List.of() : List.copyOf(alternativeSlots);
    }

    public List<SlotDTO> getAlternativeSlots() {
        return alternativeSlots;
    }
}
