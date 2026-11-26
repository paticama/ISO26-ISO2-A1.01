package com.sescam.radiodiagnostics.client.admissions;

import com.sescam.radiodiagnostics.common.schedules.SlotDTO;

import java.util.List;

/** Client-side exception wrapping server booking errors and A1 alternative slots. */
public class BookingClientException extends RuntimeException {

    private final String code;
    private final int statusCode;
    private final List<String> invalidFields;
    private final List<SlotDTO> alternativeSlots;

    public BookingClientException(String code, int statusCode, String message,
                                  List<String> invalidFields, List<SlotDTO> alternativeSlots) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
        this.invalidFields = invalidFields == null ? List.of() : List.copyOf(invalidFields);
        this.alternativeSlots = alternativeSlots == null ? List.of() : List.copyOf(alternativeSlots);
    }

    public String getCode() {
        return code;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public List<String> getInvalidFields() {
        return invalidFields;
    }

    public List<SlotDTO> getAlternativeSlots() {
        return alternativeSlots;
    }
}
