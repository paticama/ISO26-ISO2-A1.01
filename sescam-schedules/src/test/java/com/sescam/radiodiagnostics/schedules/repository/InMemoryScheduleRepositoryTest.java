package com.sescam.radiodiagnostics.schedules.repository;

import com.sescam.radiodiagnostics.common.schedules.ScheduleSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatus;
import com.sescam.radiodiagnostics.schedules.domain.Schedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryScheduleRepositoryTest {

    private static final LocalDateTime DAY = LocalDateTime.of(2026, 12, 1, 9, 0);

    @Test
    @DisplayName("Search filters by date, resource, location and status, ordered by start")
    void search() {
        InMemoryScheduleRepository repo = new InMemoryScheduleRepository();
        Schedule late = new Schedule("SCH-2", "ROOM-1", "HGUCR", "MRI", DAY.plusHours(3), DAY.plusHours(4), 30, 1,
                Instant.EPOCH);
        Schedule early = new Schedule("SCH-1", "ROOM-1", "HGUCR", "MRI", DAY, DAY.plusHours(1), 30, 1, Instant.EPOCH);
        Schedule other = new Schedule("SCH-3", "ROOM-2", "HMANCHA", "CT", DAY.plusDays(1), DAY.plusDays(1).plusHours(1),
                30, 1, Instant.EPOCH);
        repo.save(late);
        repo.save(early);
        repo.save(other);
        other.close("x", Instant.EPOCH);

        assertThat(repo.search(null)).extracting(Schedule::getId).containsExactly("SCH-1", "SCH-2", "SCH-3");
        assertThat(repo.search(new ScheduleSearchCriteriaDTO(DAY.toLocalDate(), null, null, null))).hasSize(2);
        assertThat(repo.search(new ScheduleSearchCriteriaDTO(null, " room-2 ", null, null))).hasSize(1);
        assertThat(repo.search(new ScheduleSearchCriteriaDTO(null, null, "hgucr", null))).hasSize(2);
        assertThat(repo.search(new ScheduleSearchCriteriaDTO(null, null, null, ScheduleStatus.CLOSED)))
                .extracting(Schedule::getId).containsExactly("SCH-3");
        assertThat(repo.findByResource("room-1")).extracting(Schedule::getId).containsExactly("SCH-1", "SCH-2");
        assertThat(repo.findById(null)).isEmpty();
        assertThat(repo.findById("SCH-1")).contains(early);
    }
}
