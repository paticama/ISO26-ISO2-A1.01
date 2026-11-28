package com.sescam.radiodiagnostics.schedules.repository;

import com.sescam.radiodiagnostics.common.schedules.ScheduleSearchCriteriaDTO;
import com.sescam.radiodiagnostics.schedules.domain.Schedule;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository, enough for Elaboration (same approach as the other modules).
 */
public class InMemoryScheduleRepository implements ScheduleRepository {

    private final Map<String, Schedule> storage = new ConcurrentHashMap<>();

    @Override
    public Schedule save(Schedule schedule) {
        storage.put(schedule.getId(), schedule);
        return schedule;
    }

    @Override
    public Optional<Schedule> findById(String scheduleId) {
        return scheduleId == null ? Optional.empty() : Optional.ofNullable(storage.get(scheduleId));
    }

    @Override
    public List<Schedule> findByResource(String resourceId) {
        return storage.values().stream()
                .filter(s -> s.getResourceId().equalsIgnoreCase(resourceId))
                .sorted(Comparator.comparing(Schedule::getStart))
                .toList();
    }

    @Override
    public List<Schedule> search(ScheduleSearchCriteriaDTO criteria) {
        ScheduleSearchCriteriaDTO c = criteria == null ? ScheduleSearchCriteriaDTO.all() : criteria;
        return storage.values().stream()
                .filter(s -> c.date() == null || s.getStart().toLocalDate().equals(c.date()))
                .filter(s -> isBlank(c.resourceId()) || s.getResourceId().equalsIgnoreCase(c.resourceId().trim()))
                .filter(s -> isBlank(c.locationId()) || s.getLocationId().equalsIgnoreCase(c.locationId().trim()))
                .filter(s -> c.status() == null || s.getStatus() == c.status())
                .sorted(Comparator.comparing(Schedule::getStart).thenComparing(Schedule::getResourceId))
                .toList();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
