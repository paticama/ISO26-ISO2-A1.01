package com.sescam.radiodiagnostics.common.admissions;

/** Raised when the patient is not eligible for booking (HTTP 422). */
public class PatientNotEligibleException extends BookingException {

    public PatientNotEligibleException(String message) {
        super(BookingErrorCode.PATIENT_NOT_ELIGIBLE, message);
    }
}
