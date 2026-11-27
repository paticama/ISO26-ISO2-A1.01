package com.sescam.radiodiagnostics.client.admissions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sescam.radiodiagnostics.common.admissions.AppointmentSummaryDTO;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PatientAppointmentApiClientTest {

    private MockRestServiceServer server;
    private PatientAppointmentApiClient client;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper().findAndRegisterModules();
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PatientAppointmentApiClient(builder, mapper, "http://localhost:8080");
    }

    @Test
    void listsOnlyAuthenticatedPatientsAppointments() throws Exception {
        AppointmentSummaryDTO summary = new AppointmentSummaryDTO("BKG-1",
                LocalDateTime.of(2026, 12, 10, 9, 0), LocalDateTime.of(2026, 12, 10, 9, 30),
                "Chest CT", "CT", "LOC-1", "ROOM-1", BookingStatus.BOOKED);
        server.expect(requestTo("http://localhost:8080/api/v1/my-appointments"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Actor-Id", "PAT-1"))
                .andExpect(header("X-Actor-Role", "PATIENT"))
                .andRespond(withSuccess(mapper.writeValueAsString(List.of(summary)), MediaType.APPLICATION_JSON));
        assertEquals("BKG-1", client.list("PAT-1", "PATIENT").getFirst().bookingId());
        server.verify();
    }

    @Test
    void sendsConfirmationWithVersionAndParsesServerValidationErrors() throws Exception {
        server.expect(requestTo("http://localhost:8080/api/v1/my-appointments/BKG-1/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Actor-Role", "PATIENT"))
                .andExpect(content().json("{\"expectedBookingVersion\":4}"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"CONCURRENT_BOOKING\",\"message\":\"Reload appointment\"}"));
        BookingClientException ex = assertThrows(BookingClientException.class,
                () -> client.confirm("BKG-1", 4, "PAT-1", "PATIENT"));
        assertEquals("CONCURRENT_BOOKING", ex.getCode());
        assertEquals("Reload appointment", ex.getMessage());
        server.verify();
    }
}
