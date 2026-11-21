package com.sescam.radiodiagnostics.client.security;

import com.sescam.radiodiagnostics.common.security.AuditRecordDTO;
import com.sescam.radiodiagnostics.common.security.AuditResultStatus;
import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.security.AuditSearchResultDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AuditViewController.class)
@TestPropertySource(properties = "sescam.client.zone=UTC")
class AuditViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditApiClient apiClient;

    private static final AuditRecordDTO EVENT = new AuditRecordDTO("e1", 1, Instant.parse("2026-11-20T08:30:00Z"),
            "physician-7", "CLINICAL_DATA_ACCESSED", "patient-42", null, null, "read report");

    @Test
    void opensTheAuditAreaWithAnEmptyForm() throws Exception {
        mockMvc.perform(get("/security/audit"))
                .andExpect(status().isOk())
                .andExpect(view().name("security/audit"))
                .andExpect(content().string(containsString("Audit and compliance")));
    }

    @Test
    void showsTimeOrderedEventsWithActorActionTimeAndTarget() throws Exception {
        when(apiClient.search(any(), eq("admin-1"), eq("ADMINISTRATOR"))).thenReturn(
                new AuditSearchResultDTO(null, AuditResultStatus.MATCHES_FOUND, 1, List.of(EVENT)));

        mockMvc.perform(get("/security/audit/search")
                        .param("from", "2026-11-20T00:00").param("to", "2026-11-20T23:59")
                        .param("resourceKey", "patient-42")
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1 events found")))
                .andExpect(content().string(containsString("2026-11-20 08:30:00")))
                .andExpect(content().string(containsString("physician-7")))
                .andExpect(content().string(containsString("CLINICAL_DATA_ACCESSED")))
                .andExpect(content().string(containsString("read report")));

        verify(apiClient).search(eq(new AuditSearchCriteriaDTO(Instant.parse("2026-11-20T00:00:00Z"),
                Instant.parse("2026-11-20T23:59:00Z"), null, null, "patient-42")), eq("admin-1"), eq("ADMINISTRATOR"));
    }

    @Test
    void emptyResultIsShownClearly() throws Exception {
        when(apiClient.search(any(), any(), any())).thenReturn(
                new AuditSearchResultDTO(null, AuditResultStatus.NO_MATCHES, 0, List.of()));

        mockMvc.perform(get("/security/audit/search").param("from", "2026-11-20T00:00").param("to", "2026-11-20T01:00"))
                .andExpect(content().string(containsString("No audit events match")))
                .andExpect(content().string(not(containsString("role=\"alert\""))));
    }

    @Test
    void invalidFormIsNotSentToTheServer() throws Exception {
        mockMvc.perform(get("/security/audit/search").param("from", "2026-11-20T10:00").param("to", "2026-11-20T09:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The end must be after the start.")));

        mockMvc.perform(get("/security/audit/export").param("from", "2026-11-20T10:00"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(apiClient);
    }

    @Test
    void deniedAccessIsShownAsAnError() throws Exception {
        when(apiClient.search(any(), any(), any())).thenThrow(
                new AuditClientException(AuditClientException.Kind.FORBIDDEN, "You are not authorized to view audit records."));

        mockMvc.perform(get("/security/audit/search").param("from", "2026-11-20T00:00").param("to", "2026-11-20T01:00")
                        .header("X-Actor-Id", "patient-1").header("X-Actor-Role", "PATIENT"))
                .andExpect(content().string(containsString("You are not authorized to view audit records.")))
                .andExpect(content().string(not(containsString("No audit events match"))));
    }

    @Test
    void failedQueryOffersRetryAndTooBroadOffersRefine() throws Exception {
        when(apiClient.search(any(), any(), any())).thenThrow(
                new AuditClientException(AuditClientException.Kind.QUERY_FAILED, "Could not query."));
        mockMvc.perform(get("/security/audit/search").param("from", "2026-11-20T00:00").param("to", "2026-11-20T01:00"))
                .andExpect(content().string(containsString("Retry")));

        doThrow(new AuditClientException(AuditClientException.Kind.QUERY_TOO_BROAD, "Too many events."))
                .when(apiClient).search(any(), any(), any());
        mockMvc.perform(get("/security/audit/search").param("from", "2026-11-20T00:00").param("to", "2026-11-20T01:00"))
                .andExpect(content().string(containsString("Refine the filters")));
    }

    @Test
    void exportDownloadsCsvOrReportsTheError() throws Exception {
        when(apiClient.exportCsv(any(), eq("admin-1"), any())).thenReturn("eventId\ne1\n");
        mockMvc.perform(get("/security/audit/export").param("from", "2026-11-20T00:00").param("to", "2026-11-20T01:00")
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("audit-events.csv")))
                .andExpect(content().string("eventId\ne1\n"));

        when(apiClient.exportCsv(any(), eq("patient-1"), any())).thenThrow(
                new AuditClientException(AuditClientException.Kind.FORBIDDEN, "Denied."));
        mockMvc.perform(get("/security/audit/export").param("from", "2026-11-20T00:00").param("to", "2026-11-20T01:00")
                        .header("X-Actor-Id", "patient-1"))
                .andExpect(status().isForbidden());

        when(apiClient.exportCsv(any(), eq("admin-2"), any())).thenThrow(
                new AuditClientException(AuditClientException.Kind.SERVER_UNREACHABLE, "Down."));
        mockMvc.perform(get("/security/audit/export").param("from", "2026-11-20T00:00").param("to", "2026-11-20T01:00")
                        .header("X-Actor-Id", "admin-2"))
                .andExpect(status().isServiceUnavailable());

        when(apiClient.exportCsv(any(), eq("admin-3"), any())).thenThrow(
                new AuditClientException(AuditClientException.Kind.QUERY_TOO_BROAD, "Too broad."));
        mockMvc.perform(get("/security/audit/export").param("from", "2026-11-20T00:00").param("to", "2026-11-20T01:00")
                        .header("X-Actor-Id", "admin-3"))
                .andExpect(status().isBadRequest());
    }
}
