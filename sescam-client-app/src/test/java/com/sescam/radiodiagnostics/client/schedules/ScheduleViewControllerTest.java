package com.sescam.radiodiagnostics.client.schedules;

import com.sescam.radiodiagnostics.common.schedules.AffectedAppointmentDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleChangeResultDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleOperation;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatus;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSummaryDTO;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ScheduleViewController.class)
class ScheduleViewControllerTest {

    private static final LocalDateTime START = LocalDateTime.of(2030, 3, 4, 9, 0);
    private static final AffectedAppointmentDTO AFFECTED = new AffectedAppointmentDTO("BKG-1", "SCH-1",
            "SCH-1@20300304T0900", START, START.plusMinutes(30), "Schedule closed");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleApiClient apiClient;

    private static ScheduleDTO schedule(ScheduleStatus status) {
        SlotDTO slot = new SlotDTO("SCH-1@20300304T0900", "SCH-1", "MRI-ROOM-1", "HGUCR", "MRI", START,
                START.plusMinutes(30), 1, 1, 2, false);
        return new ScheduleDTO("SCH-1", "MRI-ROOM-1", "HGUCR", "MRI", START, START.plusMinutes(30), 30, 1, status, 2,
                List.of(slot), List.of(AFFECTED), null, Instant.EPOCH);
    }

    private static ScheduleChangeResultDTO result(ScheduleOperation op, List<AffectedAppointmentDTO> affected) {
        return new ScheduleChangeResultDTO(op, schedule(ScheduleStatus.OPEN), affected, !affected.isEmpty());
    }

    @Test
    @DisplayName("GET /schedules lists the schedules found by the server")
    void list() throws Exception {
        when(apiClient.search(any(), eq("staff-1"), eq("ADMISSIONS_STAFF"))).thenReturn(List.of(
                new ScheduleSummaryDTO("SCH-1", "MRI-ROOM-1", "HGUCR", "MRI", START, START.plusHours(2),
                        ScheduleStatus.OPEN, 4, 1)));

        mockMvc.perform(get("/schedules").param("date", "2030-03-04")
                        .header("X-Actor-Id", "staff-1").header("X-Actor-Role", "ADMISSIONS_STAFF"))
                .andExpect(status().isOk())
                .andExpect(view().name(ScheduleViewController.LIST_VIEW))
                .andExpect(content().string(containsString("MRI-ROOM-1")))
                .andExpect(content().string(containsString("1 / 4")));
    }

    @Test
    @DisplayName("A6: a server rejection is shown in the list view")
    void listFailure() throws Exception {
        when(apiClient.search(any(), any(), any())).thenThrow(new ScheduleClientException("UNAUTHORIZED_OPERATION",
                "Not authorized"));
        mockMvc.perform(get("/schedules").header("X-Actor-Role", "PATIENT"))
                .andExpect(content().string(containsString("Not authorized")));
    }

    @Test
    @DisplayName("A6: without identity headers no default actor is used, so the server refuses the change")
    void noDefaultActor() throws Exception {
        when(apiClient.open(argThat(r -> r != null && r.actorId().isEmpty() && r.actorRole().isEmpty())))
                .thenThrow(new ScheduleClientException("UNAUTHORIZED_OPERATION",
                        "User '' with role '' is not authorized to open schedules (reserved to Admissions Staff)."));
        mockMvc.perform(post("/schedules/new").param("resourceId", "MRI-ROOM-1").param("locationId", "HGUCR")
                        .param("modality", "MRI").param("date", "2030-03-04").param("startTime", "09:00")
                        .param("endTime", "09:30").param("slotDurationMinutes", "30").param("capacityPerSlot", "1"))
                .andExpect(view().name(ScheduleViewController.NEW_VIEW))
                .andExpect(model().attribute("errorCode", "UNAUTHORIZED_OPERATION"))
                .andExpect(content().string(containsString("not authorized")));
        verify(apiClient).open(argThat(r -> r.actorId().isEmpty() && r.actorRole().isEmpty()));

        mockMvc.perform(get("/schedules"));
        verify(apiClient).search(any(), eq(""), eq(""));
    }

    @Test
    @DisplayName("New form renders; invalid form is shown again without calling the server")
    void openValidation() throws Exception {
        mockMvc.perform(get("/schedules/new")).andExpect(view().name(ScheduleViewController.NEW_VIEW));
        mockMvc.perform(post("/schedules/new").param("resourceId", ""))
                .andExpect(view().name(ScheduleViewController.NEW_VIEW))
                .andExpect(content().string(containsString("Resource (room or equipment) is mandatory.")));
        verify(apiClient, never()).open(any());
    }

    @Test
    @DisplayName("Open redirects to the new schedule; A1 overlap shows the conflicts")
    void open() throws Exception {
        when(apiClient.open(any())).thenReturn(result(ScheduleOperation.OPEN, List.of()));
        mockMvc.perform(post("/schedules/new").param("resourceId", "MRI-ROOM-1").param("locationId", "HGUCR")
                        .param("modality", "MRI").param("date", "2030-03-04").param("startTime", "09:00")
                        .param("endTime", "09:30").param("slotDurationMinutes", "30").param("capacityPerSlot", "1"))
                .andExpect(redirectedUrl("/schedules/SCH-1"))
                .andExpect(flash().attribute("success", "Schedule opened with 1 slots."));

        when(apiClient.open(any())).thenThrow(new ScheduleClientException("SCHEDULE_OVERLAP", "Overlap", List.of(
                new ScheduleSummaryDTO("SCH-2", "MRI-ROOM-1", "HGUCR", "MRI", START, START.plusHours(1),
                        ScheduleStatus.OPEN, 2, 0)), List.of(), null));
        mockMvc.perform(post("/schedules/new").param("resourceId", "MRI-ROOM-1").param("locationId", "HGUCR")
                        .param("modality", "MRI").param("date", "2030-03-04").param("startTime", "09:00")
                        .param("endTime", "09:30").param("slotDurationMinutes", "30").param("capacityPerSlot", "1"))
                .andExpect(view().name(ScheduleViewController.NEW_VIEW))
                .andExpect(content().string(containsString("2030-03-04 09:00-10:00 (OPEN)")));
    }

    @Test
    @DisplayName("Detail shows slots, pending rescheduling and the close form of an open schedule")
    void detail() throws Exception {
        when(apiClient.get("SCH-1", "staff-1", "ADMISSIONS_STAFF")).thenReturn(schedule(ScheduleStatus.OPEN));
        mockMvc.perform(get("/schedules/SCH-1").header("X-Actor-Id", "staff-1").header("X-Actor-Role", "ADMISSIONS_STAFF"))
                .andExpect(view().name(ScheduleViewController.DETAIL_VIEW))
                .andExpect(content().string(containsString("09:00-09:30")))
                .andExpect(content().string(containsString("Pending rescheduling")))
                .andExpect(content().string(containsString("Close schedule")))
                .andExpect(content().string(not(containsString("Reopen schedule"))));
    }

    @Test
    @DisplayName("Detail of a closed schedule offers reopen; unknown schedule shows the error")
    void detailClosedAndMissing() throws Exception {
        when(apiClient.get(eq("SCH-1"), any(), any())).thenReturn(schedule(ScheduleStatus.CLOSED));
        mockMvc.perform(get("/schedules/SCH-1"))
                .andExpect(content().string(containsString("Reopen schedule")));
        when(apiClient.get(eq("SCH-9"), any(), any())).thenThrow(new ScheduleClientException("SCHEDULE_NOT_FOUND",
                "Schedule 'SCH-9' not found."));
        mockMvc.perform(get("/schedules/SCH-9"))
                .andExpect(content().string(containsString("not found")));
    }

    @Test
    @DisplayName("A2: update listing affected appointments asks to confirm; confirmed update redirects")
    void updateWithConfirmation() throws Exception {
        when(apiClient.get(eq("SCH-1"), any(), any())).thenReturn(schedule(ScheduleStatus.OPEN));
        when(apiClient.update(argThat(r -> r != null && !r.confirmAffectedAppointments()))).thenThrow(
                new ScheduleClientException("APPOINTMENTS_AFFECTED", "1 appointment(s) are affected.", List.of(),
                        List.of(AFFECTED), null));
        when(apiClient.update(argThat(r -> r != null && r.confirmAffectedAppointments())))
                .thenReturn(result(ScheduleOperation.UPDATE, List.of(AFFECTED)));

        mockMvc.perform(post("/schedules/SCH-1/update").param("date", "2030-03-04").param("startTime", "09:00")
                        .param("endTime", "09:30").param("slotDurationMinutes", "30").param("capacityPerSlot", "1")
                        .param("expectedVersion", "2"))
                .andExpect(view().name(ScheduleViewController.DETAIL_VIEW))
                .andExpect(model().attribute("needsConfirmation", true))
                .andExpect(content().string(containsString("Confirm change")))
                .andExpect(content().string(containsString("BKG-1")));

        mockMvc.perform(post("/schedules/SCH-1/update").param("date", "2030-03-04").param("startTime", "09:00")
                        .param("endTime", "09:30").param("slotDurationMinutes", "30").param("capacityPerSlot", "1")
                        .param("expectedVersion", "2").param("confirmAffectedAppointments", "true"))
                .andExpect(redirectedUrl("/schedules/SCH-1"))
                .andExpect(flash().attribute("success",
                        "Schedule updated. 1 appointment(s) were handed to admissions for rescheduling."));
    }

    @Test
    @DisplayName("A5: invalid update fields are shown on the detail view")
    void updateInvalid() throws Exception {
        when(apiClient.get(eq("SCH-1"), any(), any())).thenReturn(schedule(ScheduleStatus.OPEN));
        mockMvc.perform(post("/schedules/SCH-1/update").param("date", "2030-03-04").param("startTime", "10:00")
                        .param("endTime", "09:00"))
                .andExpect(content().string(containsString("End time must be after the start time.")));
        verify(apiClient, never()).update(any());
    }

    @Test
    @DisplayName("A3: close requires a reason, asks to confirm affected appointments, then redirects")
    void close() throws Exception {
        when(apiClient.get(eq("SCH-1"), any(), any())).thenReturn(schedule(ScheduleStatus.OPEN));
        mockMvc.perform(post("/schedules/SCH-1/close").param("expectedVersion", "2"))
                .andExpect(content().string(containsString("A reason is mandatory")));

        when(apiClient.close(argThat(r -> r != null && !r.confirmAffectedAppointments()))).thenThrow(
                new ScheduleClientException("APPOINTMENTS_AFFECTED", "affected", List.of(), List.of(AFFECTED), null));
        mockMvc.perform(post("/schedules/SCH-1/close").param("expectedVersion", "2").param("reason", "Maintenance"))
                .andExpect(content().string(containsString("Confirm close")));

        when(apiClient.close(argThat(r -> r != null && r.confirmAffectedAppointments())))
                .thenReturn(result(ScheduleOperation.CLOSE, List.of(AFFECTED)));
        mockMvc.perform(post("/schedules/SCH-1/close").param("expectedVersion", "2").param("reason", "Maintenance")
                        .param("confirmAffectedAppointments", "true"))
                .andExpect(redirectedUrl("/schedules/SCH-1"));
    }

    @Test
    @DisplayName("A4: reopen redirects, or shows the conflict found by the server")
    void reopen() throws Exception {
        when(apiClient.reopen(any())).thenReturn(result(ScheduleOperation.REOPEN, List.of()));
        mockMvc.perform(post("/schedules/SCH-1/reopen").param("expectedVersion", "3"))
                .andExpect(redirectedUrl("/schedules/SCH-1"))
                .andExpect(flash().attribute("success", "Schedule reopened: its slots are bookable again."));

        when(apiClient.reopen(any())).thenThrow(new ScheduleClientException("SCHEDULE_OVERLAP",
                "Resource already has 1 open schedule(s)"));
        when(apiClient.get(eq("SCH-1"), any(), any())).thenReturn(schedule(ScheduleStatus.CLOSED));
        mockMvc.perform(post("/schedules/SCH-1/reopen").param("expectedVersion", "3"))
                .andExpect(view().name(ScheduleViewController.DETAIL_VIEW))
                .andExpect(content().string(containsString("already has 1 open schedule")));
    }
}
