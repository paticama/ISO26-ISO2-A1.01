package com.sescam.radiodiagnostics.client.admissions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sescam.radiodiagnostics.common.admissions.AppointmentDetailsDTO;
import com.sescam.radiodiagnostics.common.admissions.AppointmentSummaryDTO;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;
import com.sescam.radiodiagnostics.common.admissions.RescheduleRequestStatus;
import com.sescam.radiodiagnostics.common.admissions.RescheduleRequestStatusDTO;
import com.sescam.radiodiagnostics.common.orders.ConsentAndPreparationDTO;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
        mapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        RestClient.Builder builder = RestClient.builder();
        builder.messageConverters(converters -> converters.stream()
                .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                .map(MappingJackson2HttpMessageConverter.class::cast)
                .forEach(converter -> converter.setObjectMapper(mapper)));
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PatientAppointmentApiClient(builder, mapper, "http://localhost:8080");
    }

    private static AppointmentSummaryDTO summary() {
        return new AppointmentSummaryDTO("BKG-1", LocalDateTime.of(2026, 12, 10, 9, 0),
                LocalDateTime.of(2026, 12, 10, 9, 30), "Chest CT", "CT", "LOC-1", "ROOM-1", BookingStatus.BOOKED);
    }

    private static AppointmentDetailsDTO details() {
        return new AppointmentDetailsDTO(summary(), "CT-CHEST", PriorityLevel.ROUTINE,
                new ConsentAndPreparationDTO(false, null, "Drink water before the examination.", true, false),
                Set.of(), null, "Radiology: 555-0100", 4L);
    }

    @Test
    void listsOnlyAuthenticatedPatientsAppointments() throws Exception {
        server.expect(requestTo("http://localhost:8080/api/v1/my-appointments"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Actor-Id", "PAT-1"))
                .andExpect(header("X-Actor-Role", "PATIENT"))
                .andRespond(withSuccess(mapper.writeValueAsString(List.of(summary())), MediaType.APPLICATION_JSON));
        assertEquals("BKG-1", client.list("PAT-1", "PATIENT").getFirst().bookingId());
        server.verify();
    }

    @Test
    void getsAppointmentDetailsAndPreparationUsingSharedDto() throws Exception {
        server.expect(requestTo("http://localhost:8080/api/v1/my-appointments/BKG-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(mapper.writeValueAsString(details()), MediaType.APPLICATION_JSON));
        assertEquals("Drink water before the examination.",
                client.get("BKG-1", "PAT-1", "PATIENT").preparation().preparationGuide());
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

    @Test
    void sendsReschedulingReasonAndPreferredDatesAndParsesPendingStatus() throws Exception {
        RescheduleRequestStatusDTO response = new RescheduleRequestStatusDTO("REQ-1", "BKG-1",
                RescheduleRequestStatus.PENDING_REVIEW, "Work conflict", List.of(LocalDate.of(2026, 12, 12)),
                Instant.parse("2026-11-27T10:00:00Z"), null);
        server.expect(requestTo("http://localhost:8080/api/v1/my-appointments/BKG-1/reschedule-requests"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"reason\":\"Work conflict\",\"preferredDates\":[\"2026-12-12\"]}"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON).body(mapper.writeValueAsString(response)));
        assertEquals(RescheduleRequestStatus.PENDING_REVIEW,
                client.requestRescheduling("BKG-1", "Work conflict", List.of(LocalDate.of(2026, 12, 12)),
                        "PAT-1", "PATIENT").status());
        server.verify();
    }

    @Test
    void sendsCancellationConfirmationVersionReasonAndPriorityAcknowledgement() throws Exception {
        server.expect(requestTo("http://localhost:8080/api/v1/my-appointments/BKG-1/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"expectedBookingVersion\":4,\"reason\":\"Patient request\",\"priorityWarningAcknowledged\":true}"))
                .andRespond(withSuccess(mapper.writeValueAsString(details()), MediaType.APPLICATION_JSON));
        assertEquals("BKG-1", client.cancel("BKG-1", 4, "Patient request", true,
                "PAT-1", "PATIENT").summary().bookingId());
        server.verify();
    }
}
