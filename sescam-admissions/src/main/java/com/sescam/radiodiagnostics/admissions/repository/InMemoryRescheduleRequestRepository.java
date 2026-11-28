package com.sescam.radiodiagnostics.admissions.repository;

import com.sescam.radiodiagnostics.admissions.domain.RescheduleRequest;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRescheduleRequestRepository implements RescheduleRequestRepository {

    private final Map<String, RescheduleRequest> store = new ConcurrentHashMap<>();

    @Override
    public RescheduleRequest save(RescheduleRequest request) {
        store.put(request.getRequestId(), request);
        return request;
    }

    @Override
    public Optional<RescheduleRequest> findPendingByBookingId(String bookingId) {
        return store.values().stream()
                .filter(r -> r.getBookingId().equals(bookingId))
                .filter(RescheduleRequest::isPending)
                .findFirst();
    }
}
