package com.sescam.radiodiagnostics.client.admissions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sescam.radiodiagnostics.common.admissions.BookingDTO;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;
import com.sescam.radiodiagnostics.common.admissions.NotificationStatus;
import com.sescam.radiodiagnostics.common.admissions.PendingExamRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.WorklistItemDTO;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BookingApiClientTest {

    private MockRestServiceServer server;
    private BookingApiClient client;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper().findAndRegisterModules();
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new BookingApiClient(builder, mapper, "http://localhost:8080");
    }

    @Test
    void performsAllBookingEndpointsAndParsesA1ConflictAlternatives() throws Exception {
        PendingExamRequestDTO pending = new PendingExamRequestDTO("ORD-1", "PHY-1", "PAT-1", "CT-CHEST",
                "Chest CT", "CT", PriorityLevel.STAT_URGENT, Instant.parse("2026-11-26T08:00:00Z"));
        server.expect(requestTo("http://localhost:8080/api/v1/bookings/pending-orders"))
                .andExpect(header("X-Actor-Id", "adm-1"))
                .andRespond(withSuccess(mapper.writeValueAsString(List.of(pending)), MediaType.APPLICATION_JSON));
        assertEquals(1, client.listPendingOrders("adm-1", "ADMISSIONS_STAFF").size());

        SlotDTO slot = new SlotDTO("SLOT-1", "SCH-1", "CT-1", "LOC-1", "CT",
                LocalDateTime.of(2026, 12, 1, 9, 0), LocalDateTime.of(2026, 12, 1, 9, 30),
                2, 0, 1L, true);
        server.reset();
        server.expect(requestTo("http://localhost:8080/api/v1/bookings/slots?orderId=ORD-1"))
                .andRespond(withSuccess(mapper.writeValueAsString(List.of(slot)), MediaType.APPLICATION_JSON));
        assertEquals(1, client.findCompatibleSlots("ORD-1", "adm-1", "ADMISSIONS_STAFF").size());

        BookingDTO dto = new BookingDTO("BKG-1", "ORD-1", "PAT-1", "CT", "SLOT-1", "CT-1", "LOC-1",
                slot.start(), slot.end(), BookingStatus.BOOKED, NotificationStatus.SENT, 1L, List.of());
        server.reset();
        server.expect(requestTo("http://localhost:8080/api/v1/bookings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mapper.writeValueAsString(dto), MediaType.APPLICATION_JSON));
        assertEquals("BKG-1", client.createBooking(new BookingApiClient.CreateBookingPayload("ORD-1", "SLOT-1", 1L),
                "adm-1", "ADMISSIONS_STAFF").bookingId());

        server.reset();
        server.expect(requestTo("http://localhost:8080/api/v1/bookings/BKG-1"))
                .andRespond(withSuccess(mapper.writeValueAsString(dto), MediaType.APPLICATION_JSON));
        assertEquals(BookingStatus.BOOKED, client.getBooking("BKG-1", "adm-1", "ADMISSIONS_STAFF").status());

        server.reset();
        server.expect(requestTo("http://localhost:8080/api/v1/bookings/BKG-1/reschedule"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mapper.writeValueAsString(dto), MediaType.APPLICATION_JSON));
        assertEquals("BKG-1", client.rescheduleBooking("BKG-1",
                new BookingApiClient.ReschedulePayload("SLOT-2", 1L, 1L, "Move"), "adm-1", "ADMISSIONS_STAFF").bookingId());

        server.reset();
        server.expect(requestTo("http://localhost:8080/api/v1/bookings/BKG-1/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mapper.writeValueAsString(dto), MediaType.APPLICATION_JSON));
        assertEquals("BKG-1", client.cancelBooking("BKG-1",
                new BookingApiClient.CancelPayload(1L, "Cancel"), "adm-1", "ADMISSIONS_STAFF").bookingId());

        WorklistItemDTO item = new WorklistItemDTO("WL-1", "BKG-1", "ORD-1", "PAT-1", "CT-1", "CT",
                slot.start(), slot.end(), BookingStatus.BOOKED);
        server.reset();
        server.expect(requestTo("http://localhost:8080/api/v1/bookings/worklist?date=2026-12-01"))
                .andRespond(withSuccess(mapper.writeValueAsString(List.of(item)), MediaType.APPLICATION_JSON));
        assertEquals(1, client.listWorklist(LocalDate.of(2026, 12, 1), "adm-1", "ADMISSIONS_STAFF").size());

        String conflictJson = """
                {"code":"SLOT_UNAVAILABLE","message":"Slot full","invalidFields":[],"alternativeSlots":%s}
                """.formatted(mapper.writeValueAsString(List.of(slot)));
        server.reset();
        server.expect(requestTo("http://localhost:8080/api/v1/bookings"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(conflictJson));
        BookingClientException ex = assertThrows(BookingClientException.class,
                () -> client.createBooking(new BookingApiClient.CreateBookingPayload("ORD-1", "SLOT-1", 1L),
                        "adm-1", "ADMISSIONS_STAFF"));
        assertEquals("SLOT_UNAVAILABLE", ex.getCode());
        assertEquals(1, ex.getAlternativeSlots().size());
        server.verify();
    }
}
