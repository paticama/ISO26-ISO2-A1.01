package com.sescam.radiodiagnostics.admissions.repository;

import com.sescam.radiodiagnostics.admissions.domain.WorklistItem;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryWorklistRepository implements WorklistRepository {

    private final Map<String, WorklistItem> byBookingId = new ConcurrentHashMap<>();

    @Override
    public WorklistItem save(WorklistItem item) {
        byBookingId.put(item.getBookingId(), item);
        return item;
    }

    @Override
    public List<WorklistItem> findByDate(LocalDate date) {
        return byBookingId.values().stream()
                .filter(item -> item.getScheduledStart().toLocalDate().equals(date))
                .sorted(Comparator.comparing(WorklistItem::getScheduledStart))
                .toList();
    }
}
