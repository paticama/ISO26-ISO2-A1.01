package com.sescam.radiodiagnostics.client.admissions;

import com.sescam.radiodiagnostics.common.admissions.BookingDTO;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;
import com.sescam.radiodiagnostics.common.admissions.NotificationStatus;
import com.sescam.radiodiagnostics.common.admissions.PendingExamRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.WorklistItemDTO;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(BookingViewController.class)
class BookingViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingApiClient apiClient;

    private static SlotDTO sampleSlot(String id) {
        return new SlotDTO(id, "SCH-1", "CT-1", "LOC-1", "CT",
                LocalDateTime.of(2026, 12, 1, 9, 0), LocalDateTime.of(2026, 12, 1, 9, 30),
                2, 0, 1L, true);
    }

    private static BookingDTO sampleBooking(BookingStatus status) {
        return new BookingDTO("BKG-1", "ORD-1", "PAT-1", "CT", "SLOT-1", "CT-1", "LOC-1",
                LocalDateTime.of(2026, 12, 1, 9, 0), LocalDateTime.of(2026, 12, 1, 9, 30),
                status, NotificationStatus.SENT, 1L, List.of());
    }

    @Test
    void indexLoadsPendingOrdersSlotsAndWorklistWithDateFilter() throws Exception {
        when(apiClient.listPendingOrders("adm-1", "ADMISSIONS_STAFF")).thenReturn(List.of(
                new PendingExamRequestDTO("ORD-1", "PHY-1", "PAT-1", "CT-CHEST", "Chest CT", "CT",
                        PriorityLevel.STAT_URGENT, Instant.now())));
        when(apiClient.findCompatibleSlots("ORD-1", "adm-1", "ADMISSIONS_STAFF"))
                .thenReturn(List.of(sampleSlot("SLOT-1")));
        when(apiClient.listWorklist(eq(LocalDate.of(2026, 12, 1)), eq("adm-1"), eq("ADMISSIONS_STAFF")))
                .thenReturn(List.of(new WorklistItemDTO("WL-1", "BKG-1", "ORD-1", "PAT-1", "CT-1", "CT",
                        LocalDateTime.of(2026, 12, 1, 9, 0), LocalDateTime.of(2026, 12, 1, 9, 30), BookingStatus.BOOKED)));

        mockMvc.perform(get("/bookings").param("orderId", "ORD-1").param("date", "2026-12-01")
                        .header("X-Actor-Id", "adm-1")
                        .header("X-Actor-Role", "ADMISSIONS_STAFF"))
                .andExpect(status().isOk())
                .andExpect(view().name(BookingViewController.INDEX_VIEW))
                .andExpect(model().attributeExists("pendingOrders", "slots", "worklist", "selectedDate"));
    }

    @Test
    void createBookingRedirectsOnSuccessAndShowsAlternativesOnA1Conflict() throws Exception {
        when(apiClient.createBooking(any(), eq("adm-1"), eq("ADMISSIONS_STAFF")))
                .thenReturn(sampleBooking(BookingStatus.BOOKED));

        mockMvc.perform(post("/bookings")
                        .header("X-Actor-Id", "adm-1")
                        .header("X-Actor-Role", "ADMISSIONS_STAFF")
                        .param("orderId", "ORD-1")
                        .param("slotId", "SLOT-1|1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/BKG-1"));

        when(apiClient.createBooking(any(), eq("adm-1"), eq("ADMISSIONS_STAFF")))
                .thenThrow(new BookingClientException("SLOT_UNAVAILABLE", 409, "Slot full",
                        List.of(), List.of(sampleSlot("SLOT-ALT"))));
        mockMvc.perform(post("/bookings")
                        .header("X-Actor-Id", "adm-1")
                        .header("X-Actor-Role", "ADMISSIONS_STAFF")
                        .param("orderId", "ORD-1")
                        .param("slotId", "SLOT-1")
                        .param("expectedSlotVersion", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name(BookingViewController.INDEX_VIEW))
                .andExpect(model().attributeExists("failure"));
    }

    @Test
    void detailRescheduleAndCancelUsePrgAndHideFormsWhenCancelled() throws Exception {
        when(apiClient.getBooking("BKG-1", "adm-1", "ADMISSIONS_STAFF"))
                .thenReturn(sampleBooking(BookingStatus.BOOKED));
        when(apiClient.findCompatibleSlots("ORD-1", "adm-1", "ADMISSIONS_STAFF"))
                .thenReturn(List.of(sampleSlot("SLOT-2")));
        mockMvc.perform(get("/bookings/BKG-1")
                        .header("X-Actor-Id", "adm-1")
                        .header("X-Actor-Role", "ADMISSIONS_STAFF"))
                .andExpect(status().isOk())
                .andExpect(view().name(BookingViewController.DETAIL_VIEW))
                .andExpect(content().string(containsString("id=\"reschedule-form\"")));

        when(apiClient.rescheduleBooking(eq("BKG-1"), any(), eq("adm-1"), eq("ADMISSIONS_STAFF")))
                .thenThrow(new BookingClientException("SLOT_UNAVAILABLE", 409, "Full",
                        List.of(), List.of(sampleSlot("SLOT-ALT"))));
        mockMvc.perform(post("/bookings/BKG-1/reschedule")
                        .header("X-Actor-Id", "adm-1")
                        .header("X-Actor-Role", "ADMISSIONS_STAFF")
                        .param("newSlotId", "SLOT-2|1")
                        .param("expectedBookingVersion", "1")
                        .param("reason", "Earlier time"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/BKG-1"))
                .andExpect(flash().attributeExists("failure"));

        when(apiClient.cancelBooking(eq("BKG-1"), any(), eq("adm-1"), eq("ADMISSIONS_STAFF")))
                .thenReturn(sampleBooking(BookingStatus.CANCELLED));
        mockMvc.perform(post("/bookings/BKG-1/cancel")
                        .header("X-Actor-Id", "adm-1")
                        .header("X-Actor-Role", "ADMISSIONS_STAFF")
                        .param("expectedBookingVersion", "1")
                        .param("reason", "Patient cancelled"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/BKG-1"));

        when(apiClient.getBooking("BKG-1", "adm-1", "ADMISSIONS_STAFF"))
                .thenReturn(sampleBooking(BookingStatus.CANCELLED));
        mockMvc.perform(get("/bookings/BKG-1")
                        .header("X-Actor-Id", "adm-1")
                        .header("X-Actor-Role", "ADMISSIONS_STAFF"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"reschedule-form\""))));
    }
}
