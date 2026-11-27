package com.sescam.radiodiagnostics.client.admissions;

import com.sescam.radiodiagnostics.common.admissions.AppointmentAction;
import com.sescam.radiodiagnostics.common.admissions.AppointmentDetailsDTO;
import com.sescam.radiodiagnostics.common.admissions.AppointmentSummaryDTO;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;
import com.sescam.radiodiagnostics.common.orders.ConsentAndPreparationDTO;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PatientAppointmentViewController.class)
class PatientAppointmentViewControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PatientAppointmentApiClient apiClient;

    private static AppointmentSummaryDTO summary() {
        return new AppointmentSummaryDTO("BKG-1", LocalDateTime.of(2026, 12, 10, 9, 0),
                LocalDateTime.of(2026, 12, 10, 9, 30), "Chest CT", "CT", "LOC-1", "ROOM-1", BookingStatus.BOOKED);
    }

    private static AppointmentDetailsDTO details(Set<AppointmentAction> actions, PriorityLevel priority) {
        return new AppointmentDetailsDTO(summary(), "CT-CHEST", priority,
                new ConsentAndPreparationDTO(false, null, "Fast for six hours.", true, false),
                actions, null, "Radiology: 555-0100", 3L);
    }

    @Test
    void listShowsAppointmentsAndDetailsLink() throws Exception {
        when(apiClient.list("PAT-1", "PATIENT")).thenReturn(List.of(summary()));
        mockMvc.perform(get("/appointments").header("X-Actor-Id", "PAT-1").header("X-Actor-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(view().name(PatientAppointmentViewController.LIST_VIEW))
                .andExpect(content().string(containsString("Chest CT")))
                .andExpect(content().string(containsString("/appointments/BKG-1")));
    }

    @Test
    void a1ShowsEmptyStateWithoutPlannedFollowUpsLink() throws Exception {
        when(apiClient.list("PAT-1", "PATIENT")).thenReturn(List.of());
        mockMvc.perform(get("/appointments").header("X-Actor-Id", "PAT-1").header("X-Actor-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no upcoming appointments")))
                .andExpect(content().string(not(containsString("/follow-ups"))));
    }

    @Test
    void detailShowsPreparationAndOnlyPermittedActions() throws Exception {
        when(apiClient.get("BKG-1", "PAT-1", "PATIENT"))
                .thenReturn(details(Set.of(AppointmentAction.CONFIRM, AppointmentAction.REQUEST_RESCHEDULE), PriorityLevel.ROUTINE));
        mockMvc.perform(get("/appointments/BKG-1").header("X-Actor-Id", "PAT-1").header("X-Actor-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fast for six hours.")))
                .andExpect(content().string(containsString("id=\"confirm-form\"")))
                .andExpect(content().string(not(containsString("id=\"cancel-form\""))));
    }

    @Test
    void unauthorizedPatientRequestsDoNotCallApi() throws Exception {
        mockMvc.perform(get("/appointments").header("X-Actor-Id", "staff-1").header("X-Actor-Role", "ADMISSIONS_STAFF"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sign in as a patient")));
        verifyNoInteractions(apiClient);
    }

    @Test
    void invalidRescheduleIsShownAndNeverSubmittedToServer() throws Exception {
        when(apiClient.get("BKG-1", "PAT-1", "PATIENT"))
                .thenReturn(details(Set.of(AppointmentAction.REQUEST_RESCHEDULE), PriorityLevel.ROUTINE));
        mockMvc.perform(post("/appointments/BKG-1/reschedule-requests")
                        .header("X-Actor-Id", "PAT-1").header("X-Actor-Role", "PATIENT")
                        .param("reason", " ").param("preferredDates", "not-a-date"))
                .andExpect(status().isOk())
                .andExpect(view().name(PatientAppointmentViewController.DETAIL_VIEW))
                .andExpect(model().attributeExists("errors", "appointment"));
        verify(apiClient, never()).requestRescheduling(org.mockito.ArgumentMatchers.eq("BKG-1"),
                org.mockito.ArgumentMatchers.anyString(), anyList(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void cancellationNeedsExplicitConfirmationAndWarnsForPriorityExam() throws Exception {
        when(apiClient.get("BKG-1", "PAT-1", "PATIENT"))
                .thenReturn(details(Set.of(AppointmentAction.CANCEL), PriorityLevel.STAT_URGENT));
        mockMvc.perform(post("/appointments/BKG-1/cancel")
                        .header("X-Actor-Id", "PAT-1").header("X-Actor-Role", "PATIENT")
                        .param("expectedBookingVersion", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("clinical priority")))
                .andExpect(model().attributeExists("errors", "appointment", "canCancel"));
        verify(apiClient, never()).cancel(org.mockito.ArgumentMatchers.eq("BKG-1"),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void confirmationUsesPostRedirectGetAndReportsSuccess() throws Exception {
        when(apiClient.confirm("BKG-1", 3L, "PAT-1", "PATIENT"))
                .thenReturn(details(Set.of(), PriorityLevel.ROUTINE));
        mockMvc.perform(post("/appointments/BKG-1/confirm")
                        .header("X-Actor-Id", "PAT-1").header("X-Actor-Role", "PATIENT")
                        .param("expectedBookingVersion", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments/BKG-1"))
                .andExpect(flash().attribute("success", "Your appointment has been confirmed."));
        verify(apiClient).confirm("BKG-1", 3L, "PAT-1", "PATIENT");
    }
}
