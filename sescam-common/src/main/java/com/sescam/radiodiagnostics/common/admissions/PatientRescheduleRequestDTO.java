package com.sescam.radiodiagnostics.common.admissions;

import java.time.LocalDate;
import java.util.List;

/**
 * The patient asks Admissions to move an appointment (UC-PAT-01 A2).
 * Not to be confused with {@link RescheduleBookingRequestDTO}, with which staff actually move it (UC-AAD-05).
 *
 * @param reason         mandatory reason
 * @param preferredDates optional dates the patient would prefer; never null (an absent list becomes empty)
 */
public record PatientRescheduleRequestDTO(
        String bookingId,
        String reason,
        List<LocalDate> preferredDates,
        String actorId,
        String actorRole
) {
    public PatientRescheduleRequestDTO {
        preferredDates = preferredDates == null ? List.of() : List.copyOf(preferredDates);
    }
}
