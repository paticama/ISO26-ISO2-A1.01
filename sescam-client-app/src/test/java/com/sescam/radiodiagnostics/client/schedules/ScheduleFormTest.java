package com.sescam.radiodiagnostics.client.schedules;

import com.sescam.radiodiagnostics.common.schedules.OpenScheduleRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatus;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatusChangeRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.UpdateScheduleRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleFormTest {

    private static ScheduleForm valid() {
        ScheduleForm f = new ScheduleForm();
        f.setResourceId(" MRI-ROOM-1 ");
        f.setLocationId("HGUCR");
        f.setModality("MRI");
        f.setDate("2030-03-04");
        f.setStartTime("09:00");
        f.setEndTime("11:00");
        f.setSlotDurationMinutes(30);
        f.setCapacityPerSlot(1);
        return f;
    }

    @Test
    @DisplayName("A valid form builds the open request with trimmed fields")
    void validOpen() {
        ScheduleForm f = valid();
        assertThat(f.validateOpen()).isEmpty();
        OpenScheduleRequestDTO r = f.toOpenRequest("staff-1", "ADMISSIONS_STAFF");
        assertThat(r.resourceId()).isEqualTo("MRI-ROOM-1");
        assertThat(r.start()).isEqualTo(LocalDateTime.of(2030, 3, 4, 9, 0));
        assertThat(r.end()).isEqualTo(LocalDateTime.of(2030, 3, 4, 11, 0));
        assertThat(r.actorRole()).isEqualTo("ADMISSIONS_STAFF");
    }

    @Test
    @DisplayName("A5: missing resource, location, modality and date are reported before calling the server")
    void mandatoryFields() {
        ScheduleForm f = new ScheduleForm();
        assertThat(f.validateOpen()).containsKeys("resourceId", "locationId", "modality", "date", "startTime",
                "endTime");
    }

    @Test
    @DisplayName("A5: wrong formats, inverted window, slot and capacity are reported")
    void invalidWindow() {
        ScheduleForm f = valid();
        f.setDate("04/03/2030");
        f.setStartTime("9h");
        assertThat(f.validateWindow()).containsKeys("date", "startTime");

        f = valid();
        f.setEndTime("08:00");
        assertThat(f.validateWindow()).containsEntry("endTime", "End time must be after the start time.");

        f = valid();
        f.setSlotDurationMinutes(45);
        assertThat(f.validateWindow()).containsKey("slotDurationMinutes");
        f.setSlotDurationMinutes(null);
        f.setCapacityPerSlot(0);
        assertThat(f.validateWindow()).containsKeys("slotDurationMinutes", "capacityPerSlot");
    }

    @Test
    @DisplayName("Close requires a reason")
    void closeReason() {
        ScheduleForm f = valid();
        assertThat(f.validateClose()).containsKey("reason");
        f.setReason("Maintenance");
        assertThat(f.validateClose()).isEmpty();
    }

    @Test
    @DisplayName("Form loaded from a schedule builds update and status-change requests with its version")
    void fromDtoAndRequests() {
        ScheduleDTO dto = new ScheduleDTO("SCH-1", "R", "L", "MRI", LocalDateTime.of(2030, 3, 4, 23, 0),
                LocalDateTime.of(2030, 3, 5, 0, 0), 30, 2, ScheduleStatus.OPEN, 3, List.of(), List.of(), null,
                Instant.EPOCH);
        ScheduleForm f = ScheduleForm.fromDTO(dto);
        f.setConfirmAffectedAppointments(true);
        f.setReason("  ");

        UpdateScheduleRequestDTO u = f.toUpdateRequest("staff-1", "ADMISSIONS_STAFF");
        assertThat(u.scheduleId()).isEqualTo("SCH-1");
        assertThat(u.expectedVersion()).isEqualTo(3);
        assertThat(u.end()).as("midnight ends the same day").isEqualTo(LocalDateTime.of(2030, 3, 5, 0, 0));
        assertThat(u.confirmAffectedAppointments()).isTrue();
        assertThat(u.reason()).isNull();

        ScheduleStatusChangeRequestDTO c = f.toStatusChangeRequest("staff-1", "ADMISSIONS_STAFF");
        assertThat(c.expectedVersion()).isEqualTo(3);
        assertThat(f.getCapacityPerSlot()).isEqualTo(2);
        assertThat(f.getModality()).isEqualTo("MRI");
        assertThat(f.getLocationId()).isEqualTo("L");
        assertThat(f.getResourceId()).isEqualTo("R");
        assertThat(f.getSlotDurationMinutes()).isEqualTo(30);
        assertThat(f.isConfirmAffectedAppointments()).isTrue();
        assertThat(f.getScheduleId()).isEqualTo("SCH-1");
        assertThat(f.getStartTime()).isEqualTo("23:00");
        assertThat(f.getEndTime()).isEqualTo("00:00");
        assertThat(f.getDate()).isEqualTo("2030-03-04");
        assertThat(f.getReason()).isBlank();
        assertThat(f.getExpectedVersion()).isEqualTo(3);
    }
}
