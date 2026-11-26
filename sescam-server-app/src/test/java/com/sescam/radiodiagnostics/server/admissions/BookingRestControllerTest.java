package com.sescam.radiodiagnostics.server.admissions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sescam.radiodiagnostics.common.schedules.OpenScheduleRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleChangeResultDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleService;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void endToEndBookingRescheduleCancelAndA6UnauthorizedFlow() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(5);
        ScheduleChangeResultDTO opened = scheduleService.openSchedule(new OpenScheduleRequestDTO(
                "RES-CT-99", "LOC-MAIN", "CT",
                futureDate.atTime(9, 0), futureDate.atTime(10, 0),
                30, 1, "adm-1", "ADMISSIONS_STAFF"));
        SlotDTO firstSlot = opened.schedule().slots().get(0);
        SlotDTO secondSlot = opened.schedule().slots().get(1);

        // A6: System Administrator denied
        mockMvc.perform(get("/api/v1/bookings/pending-orders")
                        .header("X-Actor-Id", "admin-1")
                        .header("X-Actor-Role", "SYSTEM_ADMINISTRATOR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_BOOKING_ACCESS"));

        // List pending orders
        mockMvc.perform(get("/api/v1/bookings/pending-orders")
                        .header("X-Actor-Id", "adm-1")
                        .header("X-Actor-Role", "ADMISSIONS_STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("ORD-1001"));

        // Create booking -> 201
        String createBody = """
                {"orderId":"ORD-1001","slotId":"%s","expectedSlotVersion":%d}
                """.formatted(firstSlot.slotId(), firstSlot.version());
        String responseJson = mockMvc.perform(post("/api/v1/bookings")
                        .header("X-Actor-Id", "adm-1")
                        .header("X-Actor-Role", "ADMISSIONS_STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andReturn().getResponse().getContentAsString();

        String bookingId = objectMapper.readTree(responseJson).get("bookingId").asText();
        long bookingVersion = objectMapper.readTree(responseJson).get("version").asLong();

        // A1: Occupied slot returns 409 with alternativeSlots
        String conflictBody = """
                {"orderId":"ORD-1001","slotId":"%s","expectedSlotVersion":%d}
                """.formatted(firstSlot.slotId(), firstSlot.version());
        mockMvc.perform(post("/api/v1/bookings")
                        .header("X-Actor-Id", "adm-1")
                        .header("X-Actor-Role", "ADMISSIONS_STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conflictBody))
                .andExpect(status().isConflict());

        // A2: Reschedule to secondSlot
        String rescheduleBody = """
                {"newSlotId":"%s","expectedSlotVersion":%d,"expectedBookingVersion":%d,"reason":"Earlier room change"}
                """.formatted(secondSlot.slotId(), secondSlot.version(), bookingVersion);
        String rescheduledJson = mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/reschedule")
                        .header("X-Actor-Id", "adm-1")
                        .header("X-Actor-Role", "ADMISSIONS_STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rescheduleBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESCHEDULED"))
                .andReturn().getResponse().getContentAsString();

        long rescheduledVersion = objectMapper.readTree(rescheduledJson).get("version").asLong();

        // A3: Cancel booking
        String cancelBody = """
                {"expectedBookingVersion":%d,"reason":"Patient postponed"}
                """.formatted(rescheduledVersion);
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/cancel")
                        .header("X-Actor-Id", "adm-1")
                        .header("X-Actor-Role", "ADMISSIONS_STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
