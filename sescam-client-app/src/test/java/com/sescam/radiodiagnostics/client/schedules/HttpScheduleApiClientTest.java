package com.sescam.radiodiagnostics.client.schedules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sescam.radiodiagnostics.common.schedules.OpenScheduleRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleChangeResultDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatus;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatusChangeRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.UpdateScheduleRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpScheduleApiClientTest {

    private static final LocalDateTime START = LocalDateTime.of(2030, 3, 4, 9, 0);
    private static final String SCHEDULE_JSON = """
            {"scheduleId": "SCH-1", "resourceId": "MRI-ROOM-1", "locationId": "HGUCR", "modality": "MRI",
             "start": "2030-03-04T09:00:00", "end": "2030-03-04T10:00:00", "slotDurationMinutes": 30,
             "capacityPerSlot": 1, "status": "OPEN", "version": 1,
             "slots": [{"slotId": "SCH-1@20300304T0900", "scheduleId": "SCH-1", "resourceId": "MRI-ROOM-1",
                        "locationId": "HGUCR", "modality": "MRI", "start": "2030-03-04T09:00:00",
                        "end": "2030-03-04T09:30:00", "capacity": 1, "bookedCount": 0, "version": 1,
                        "bookable": true}],
             "appointmentsPendingRescheduling": []}
            """;
    private static final String RESULT_JSON = "{\"operation\": \"OPEN\", \"schedule\": " + SCHEDULE_JSON
            + ", \"affectedAppointments\": [], \"reschedulingRequired\": false}";

    private MockRestServiceServer server;
    private HttpScheduleApiClient client;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(c -> c.add(0, new MappingJackson2HttpMessageConverter(mapper)));
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpScheduleApiClient(builder, mapper, "http://server");
    }

    @Test
    @DisplayName("Search sends the filters and the actor headers")
    void search() {
        server.expect(requestTo("http://server/api/v1/schedules?date=2030-03-04&resourceId=MRI-ROOM-1&status=OPEN"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Actor-Role", "ADMISSIONS_STAFF"))
                .andRespond(withSuccess("[{\"scheduleId\": \"SCH-1\", \"status\": \"OPEN\"}]",
                        MediaType.APPLICATION_JSON));

        assertThat(client.search(new ScheduleSearchCriteriaDTO(LocalDate.of(2030, 3, 4), " MRI-ROOM-1 ", " ",
                ScheduleStatus.OPEN), "staff-1", "ADMISSIONS_STAFF")).hasSize(1);
        server.verify();
    }

    @Test
    @DisplayName("Search without criteria and get by id")
    void searchAllAndGet() {
        server.expect(requestTo("http://server/api/v1/schedules")).andRespond(withSuccess("[]",
                MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://server/api/v1/schedules/SCH-1")).andRespond(withSuccess(SCHEDULE_JSON,
                MediaType.APPLICATION_JSON));

        assertThat(client.search(null, "staff-1", "ADMISSIONS_STAFF")).isEmpty();
        assertThat(client.get("SCH-1", null, null).slots()).hasSize(1);
        server.verify();
    }

    @Test
    @DisplayName("Open posts the schedule with ISO dates")
    void open() {
        server.expect(requestTo("http://server/api/v1/schedules"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.start").value("2030-03-04T09:00"))
                .andExpect(jsonPath("$.capacityPerSlot").value(1))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(RESULT_JSON));

        ScheduleChangeResultDTO result = client.open(new OpenScheduleRequestDTO("MRI-ROOM-1", "HGUCR", "MRI", START,
                START.plusHours(1), 30, 1, "staff-1", "ADMISSIONS_STAFF"));
        assertThat(result.schedule().scheduleId()).isEqualTo("SCH-1");
        server.verify();
    }

    @Test
    @DisplayName("Update, close and reopen call their endpoints with version and confirmation")
    void updateCloseReopen() {
        server.expect(requestTo("http://server/api/v1/schedules/SCH-1")).andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.expectedVersion").value(1))
                .andExpect(jsonPath("$.confirmAffectedAppointments").value(true))
                .andRespond(withSuccess(RESULT_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://server/api/v1/schedules/SCH-1/close")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.reason").value("Maintenance"))
                .andRespond(withSuccess(RESULT_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://server/api/v1/schedules/SCH-1/reopen")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(RESULT_JSON, MediaType.APPLICATION_JSON));

        client.update(new UpdateScheduleRequestDTO("SCH-1", START, START.plusHours(1), 30, 1, 1, true, null,
                "staff-1", "ADMISSIONS_STAFF"));
        client.close(new ScheduleStatusChangeRequestDTO("SCH-1", 1, false, "Maintenance", "staff-1",
                "ADMISSIONS_STAFF"));
        client.reopen(new ScheduleStatusChangeRequestDTO("SCH-1", 2, false, null, "staff-1", "ADMISSIONS_STAFF"));
        server.verify();
    }

    @Test
    @DisplayName("A2: 409 APPOINTMENTS_AFFECTED carries the affected appointments and asks for confirmation")
    void affected() {
        server.expect(requestTo("http://server/api/v1/schedules/SCH-1/close"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body("""
                        {"error": "APPOINTMENTS_AFFECTED", "message": "1 appointment(s) are affected.",
                         "affectedAppointments": [{"bookingReference": "BKG-1", "scheduleId": "SCH-1",
                           "slotId": "SCH-1@20300304T0900", "slotStart": "2030-03-04T09:00:00",
                           "slotEnd": "2030-03-04T09:30:00", "reason": "Schedule closed"}]}
                        """));

        assertThatThrownBy(() -> client.close(new ScheduleStatusChangeRequestDTO("SCH-1", 1, false, "x", "s", "r")))
                .isInstanceOfSatisfying(ScheduleClientException.class, ex -> {
                    assertThat(ex.needsConfirmation()).isTrue();
                    assertThat(ex.getAffectedAppointments()).hasSize(1);
                    assertThat(ex.getMessage()).contains("affected");
                });
    }

    @Test
    @DisplayName("A1: 409 SCHEDULE_OVERLAP carries the conflicting schedules")
    void overlap() {
        server.expect(requestTo("http://server/api/v1/schedules"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body("""
                        {"error": "SCHEDULE_OVERLAP", "message": "overlap",
                         "conflictingSchedules": [{"scheduleId": "SCH-2", "status": "OPEN"}]}
                        """));

        assertThatThrownBy(() -> client.open(new OpenScheduleRequestDTO("R", "L", "MRI", START, START.plusHours(1),
                30, 1, "s", "r")))
                .isInstanceOfSatisfying(ScheduleClientException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("SCHEDULE_OVERLAP");
                    assertThat(ex.getConflictingSchedules()).hasSize(1);
                    assertThat(ex.needsConfirmation()).isFalse();
                });
    }

    @Test
    @DisplayName("Errors without body get a code from the HTTP status; unreachable server is reported")
    void fallbacks() {
        server.expect(requestTo("http://server/api/v1/schedules/A")).andRespond(withStatus(HttpStatus.FORBIDDEN));
        server.expect(requestTo("http://server/api/v1/schedules/B")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://server/api/v1/schedules/C")).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON).body("{\"affectedAppointments\": \"oops\"}"));
        server.expect(requestTo("http://server/api/v1/schedules/D"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("not json"));
        server.expect(requestTo("http://server/api/v1/schedules/E")).andRespond(withException(new IOException("down")));
        server.expect(requestTo("http://server/api/v1/schedules/F")).andRespond(withStatus(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON).body("{\"conflictingSchedules\": [{\"status\": \"BAD\"}]}"));

        assertThatThrownBy(() -> client.get("A", "s", "r")).hasFieldOrPropertyWithValue("errorCode",
                "UNAUTHORIZED_OPERATION");
        assertThatThrownBy(() -> client.get("B", "s", "r")).hasFieldOrPropertyWithValue("errorCode",
                "SCHEDULE_NOT_FOUND");
        assertThatThrownBy(() -> client.get("C", "s", "r")).hasFieldOrPropertyWithValue("errorCode",
                "INVALID_SCHEDULE");
        assertThatThrownBy(() -> client.get("D", "s", "r")).hasFieldOrPropertyWithValue("errorCode",
                ScheduleClientException.GENERIC_ERROR).hasMessageContaining("HTTP 500");
        assertThatThrownBy(() -> client.get("E", "s", "r")).hasFieldOrPropertyWithValue("errorCode",
                ScheduleClientException.SERVER_UNREACHABLE);
        assertThatThrownBy(() -> client.get("F", "s", "r"))
                .isInstanceOfSatisfying(ScheduleClientException.class,
                        ex -> assertThat(ex.getConflictingSchedules()).isEmpty());
    }
}
