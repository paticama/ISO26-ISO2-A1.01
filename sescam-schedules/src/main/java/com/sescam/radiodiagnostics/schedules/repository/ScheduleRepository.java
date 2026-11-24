package com.sescam.radiodiagnostics.schedules.repository;

import com.sescam.radiodiagnostics.common.schedules.ScheduleSearchCriteriaDTO;
import com.sescam.radiodiagnostics.schedules.domain.Schedule;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port of C3 for {@link Schedule}.
 */
public interface ScheduleRepository {

    Schedule save(Schedule schedule);

    Optional<Schedule> findById(String scheduleId);

    /** Schedules of one resource, any status. */
    List<Schedule> findByResource(String resourceId);

    /** Schedules matching the criteria, ordered by start. */
    List<Schedule> search(ScheduleSearchCriteriaDTO criteria);
}
