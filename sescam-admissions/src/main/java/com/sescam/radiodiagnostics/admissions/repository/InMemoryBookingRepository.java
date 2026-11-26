package com.sescam.radiodiagnostics.admissions.repository;

import com.sescam.radiodiagnostics.admissions.domain.Booking;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBookingRepository implements BookingRepository {

    private final Map<String, Booking> store = new ConcurrentHashMap<>();

    @Override
    public Booking save(Booking booking) {
        store.put(booking.getBookingId(), booking);
        return booking;
    }

    @Override
    public Optional<Booking> findById(String bookingId) {
        return Optional.ofNullable(store.get(bookingId));
    }

    @Override
    public boolean existsActiveByOrderId(String orderId) {
        return store.values().stream()
                .anyMatch(b -> b.getOrderId().equals(orderId) && b.getStatus() != BookingStatus.CANCELLED);
    }

    @Override
    public List<Booking> findActiveByPatientAndDate(String patientId, LocalDate date) {
        return store.values().stream()
                .filter(b -> b.getPatientId().equals(patientId))
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .filter(b -> b.getScheduledStart().toLocalDate().equals(date))
                .toList();
    }
}
