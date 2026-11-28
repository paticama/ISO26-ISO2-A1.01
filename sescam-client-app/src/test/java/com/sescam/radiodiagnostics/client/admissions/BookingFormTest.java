package com.sescam.radiodiagnostics.client.admissions;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingFormTest {

    @Test
    void rejectsMissingFieldsAndBuildsTrimmedPayloadWhenValid() {
        BookingForm invalid = new BookingForm();
        invalid.setOrderId(" ");
        invalid.setSlotId("");
        invalid.setExpectedSlotVersion(-1L);

        Map<String, String> errors = invalid.validate();
        assertEquals(3, errors.size());
        assertTrue(errors.containsKey("orderId"));
        assertTrue(errors.containsKey("slotId"));
        assertTrue(errors.containsKey("expectedSlotVersion"));

        BookingForm valid = new BookingForm();
        valid.setOrderId(" ORD-1 ");
        valid.setSlotId(" SLOT-1 ");
        valid.setExpectedSlotVersion(2L);

        assertTrue(valid.validate().isEmpty());
        BookingApiClient.CreateBookingPayload payload = valid.toPayload();
        assertEquals("ORD-1", payload.orderId());
        assertEquals("SLOT-1", payload.slotId());
        assertEquals(2L, payload.expectedSlotVersion());
    }
}
