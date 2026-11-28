package com.sescam.radiodiagnostics.server.schedules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sescam.radiodiagnostics.common.schedules.ScheduleService;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import com.sescam.radiodiagnostics.common.security.AuditQueryService;
import com.sescam.radiodiagnostics.common.security.AuditRecordDTO;
import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.security.AuditSearchResultDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ScheduleRestControllerTest {

    private static final String STAFF = "staff-1";
    private static final String STAFF_ROLE = "ADMISSIONS_STAFF";
    private static final String BASE = "/api/v1/schedules";

    private static final String OPEN_JSON = """
            {"resourceId": "MRI-ROOM-1", "locationId": "HGUCR", "modality": "MRI",
             "start": "2030-03-04T09:00:00", "end": "2030-03-04T11:00:00",
             "slotDurationMinutes": 30, "capacityPerSlot": 1}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private AuditQueryService auditQueryService;

    private MockHttpServletRequestBuilder asStaff(MockHttpServletRequestBuilder request) {
        return request.header("X-Actor-Id", STAFF).header("X-Actor-Role", STAFF_ROLE)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private JsonNode open() throws Exception {
        String body = mockMvc.perform(asStaff(post(BASE)).content(OPEN_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("schedule");
    }

    @Test
    @DisplayName("POST opens a schedule (201), GET lists and consults it, slots are exposed to booking")
    void openSearchAndConsult() throws Exception {
        JsonNode schedule = open();
        String id = schedule.get("scheduleId").asText();

        mockMvc.perform(asStaff(get(BASE)).param("date", "2030-03-04").param("resourceId", "MRI-ROOM-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].totalCapacity", is(4)));
        mockMvc.perform(asStaff(get(BASE + "/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.slots", hasSize(4)));
        mockMvc.perform(get(BASE + "/slots/available").param("modality", "MRI")
                        .param("from", "2030-03-04T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("A1: overlapping schedule returns 409 SCHEDULE_OVERLAP with the conflicts")
    void overlap() throws Exception {
        open();
        mockMvc.perform(asStaff(post(BASE)).content(OPEN_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("SCHEDULE_OVERLAP")))
                .andExpect(jsonPath("$.conflictingSchedules", hasSize(1)));
    }

    @Test
    @DisplayName("A2: reducing availability lists affected appointments (409) until confirmed (200)")
    void affectedAppointments() throws Exception {
        JsonNode schedule = open();
        String id = schedule.get("scheduleId").asText();
        SlotDTO last = scheduleService.getSchedule(id, STAFF, STAFF_ROLE).slots().get(3);
        scheduleService.reserveSlotAtomically(last.slotId(), last.version(), "BKG-77");
        long version = scheduleService.getSchedule(id, STAFF, STAFF_ROLE).version();
        String update = """
                {"start": "2030-03-04T09:00:00", "end": "2030-03-04T10:00:00", "slotDurationMinutes": 30,
                 "capacityPerSlot": 1, "expectedVersion": %d, "confirmAffectedAppointments": %s, "reason": "Staff"}
                """;

        mockMvc.perform(asStaff(put(BASE + "/" + id)).content(update.formatted(version, false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("APPOINTMENTS_AFFECTED")))
                .andExpect(jsonPath("$.affectedAppointments[0].bookingReference", is("BKG-77")));

        mockMvc.perform(asStaff(put(BASE + "/" + id)).content(update.formatted(version, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reschedulingRequired", is(true)))
                .andExpect(jsonPath("$.schedule.slots", hasSize(2)))
                .andExpect(jsonPath("$.schedule.appointmentsPendingRescheduling", hasSize(1)));
    }

    @Test
    @DisplayName("A3/A4: close and reopen, with audit entries recorded in C1")
    void closeAndReopen() throws Exception {
        String id = open().get("scheduleId").asText();

        mockMvc.perform(asStaff(post(BASE + "/" + id + "/close"))
                        .content("{\"expectedVersion\": 1, \"reason\": \"Scanner maintenance\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedule.status", is("CLOSED")));
        mockMvc.perform(get(BASE + "/slots/available").param("modality", "MRI"))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(asStaff(post(BASE + "/" + id + "/reopen")).content("{\"expectedVersion\": 2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedule.status", is("OPEN")));
        mockMvc.perform(asStaff(post(BASE + "/" + id + "/reopen")).content("{\"expectedVersion\": 2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("CONCURRENT_MODIFICATION")));

        Instant now = Instant.now();
        AuditSearchResultDTO audit = auditQueryService.searchEvents(new AuditSearchCriteriaDTO(
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS), STAFF, null, id),
                "admin-1", "SYSTEM_ADMINISTRATOR");
        assertThat(audit.events()).extracting(AuditRecordDTO::action)
                .contains("SCHEDULE_OPENED", "SCHEDULE_CLOSED", "SCHEDULE_REOPENED");
    }

    @Test
    @DisplayName("A5 and capacity: 400 INVALID_SCHEDULE, 422 CAPACITY_EXCEEDED, 400 on malformed body")
    void invalid() throws Exception {
        mockMvc.perform(asStaff(post(BASE)).content(OPEN_JSON.replace("11:00:00", "08:00:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_SCHEDULE")));
        mockMvc.perform(asStaff(post(BASE)).content(OPEN_JSON.replace("\"capacityPerSlot\": 1", "\"capacityPerSlot\": 9")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error", is("CAPACITY_EXCEEDED")));
        mockMvc.perform(asStaff(post(BASE)).content("{\"start\": \"tomorrow\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_SCHEDULE")));
        mockMvc.perform(asStaff(get(BASE + "/SCH-NOPE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("SCHEDULE_NOT_FOUND")));
    }

    @Test
    @DisplayName("A6: System Administrator gets 403 UNAUTHORIZED_OPERATION when operating schedules")
    void unauthorized() throws Exception {
        mockMvc.perform(post(BASE).header("X-Actor-Id", "admin-1").header("X-Actor-Role", "SYSTEM_ADMINISTRATOR")
                        .contentType(MediaType.APPLICATION_JSON).content(OPEN_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_OPERATION")));
        mockMvc.perform(get(BASE))
                .andExpect(status().isForbidden());
    }
}
