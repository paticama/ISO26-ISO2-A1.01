package com.sescam.radiodiagnostics.admissions.repository;

import com.sescam.radiodiagnostics.admissions.domain.Booking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(String bookingId);

    boolean existsActiveByOrderId(String orderId);

    List<Booking> findActiveByPatientAndDate(String patientId, LocalDate date);
}
