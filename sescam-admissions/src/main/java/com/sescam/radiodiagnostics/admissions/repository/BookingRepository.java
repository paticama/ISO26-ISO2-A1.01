package com.sescam.radiodiagnostics.admissions.repository;

import com.sescam.radiodiagnostics.admissions.domain.Booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(String bookingId);

    boolean existsActiveByOrderId(String orderId);

    List<Booking> findActiveByPatientAndDate(String patientId, LocalDate date);

    /**
     * Bookings of the patient that start at {@code from} or later, in any state except PENDING, so the patient
     * also sees cancelled or executed ones with their status (UC-PAT-01 step 2, A7). Order is not guaranteed.
     */
    List<Booking> findUpcomingByPatientId(String patientId, LocalDateTime from);
}
