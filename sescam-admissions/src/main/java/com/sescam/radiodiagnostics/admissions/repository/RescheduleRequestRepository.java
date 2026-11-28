package com.sescam.radiodiagnostics.admissions.repository;

import com.sescam.radiodiagnostics.admissions.domain.RescheduleRequest;

import java.util.Optional;

/** Storage of the patients' rescheduling requests (UC-PAT-01 A2, A6). */
public interface RescheduleRequestRepository {

    RescheduleRequest save(RescheduleRequest request);

    /** The request of this booking still waiting for Admissions, if any. There is at most one (A6). */
    Optional<RescheduleRequest> findPendingByBookingId(String bookingId);
}
