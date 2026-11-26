package com.sescam.radiodiagnostics.client.admissions;

import java.util.LinkedHashMap;
import java.util.Map;

/** Form backing UC-AAD-05 booking confirmation in the Admissions UI. */
public class BookingForm {

    private String orderId;
    private String slotId;
    private Long expectedSlotVersion;

    public Map<String, String> validate() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(orderId)) {
            errors.put("orderId", "Select a pending examination request.");
        }
        if (isBlank(slotId)) {
            errors.put("slotId", "Select a compatible slot.");
        }
        if (expectedSlotVersion == null || expectedSlotVersion < 0) {
            errors.put("expectedSlotVersion", "Slot version is required.");
        }
        return errors;
    }

    public BookingApiClient.CreateBookingPayload toPayload() {
        return new BookingApiClient.CreateBookingPayload(
                orderId.trim(),
                slotId.trim(),
                expectedSlotVersion == null ? 0L : expectedSlotVersion
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSlotId() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public Long getExpectedSlotVersion() {
        return expectedSlotVersion;
    }

    public void setExpectedSlotVersion(Long expectedSlotVersion) {
        this.expectedSlotVersion = expectedSlotVersion;
    }
}
