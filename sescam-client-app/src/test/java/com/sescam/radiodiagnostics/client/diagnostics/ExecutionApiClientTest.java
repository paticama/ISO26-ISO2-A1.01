package com.sescam.radiodiagnostics.client.diagnostics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionRecordDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionStatus;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentDTO;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentSeverity;
import com.sescam.radiodiagnostics.common.diagnostics.IncidentType;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExecutionApiClientTest {

    private static final String BASE = "http://server";
    private static final ExecutionApiClient.ExecutionPayload PAYLOAD = new ExecutionApiClient.ExecutionPayload(
            "ORD-DEMO-CT", Instant.parse("2026-11-24T09:05:00Z"), Instant.parse("2026-11-24T09:21:00Z"), "CT-1",
            null, null, null, null, null, true, true, "Severe reaction",
            List.of(new IncidentDTO(IncidentType.ANAPHYLACTOID_EVENT, IncidentSeverity.SEVERE, "Reaction", null, null)));

    private MockRestServiceServer server;
    private ExecutionApiClient client;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(c -> c.add(0, new MappingJackson2HttpMessageConverter(mapper)));
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ExecutionApiClient(builder, mapper, BASE);
    }

    @Test
    void listPendingSendsTheActorAndReadsTheProcedures() {
        server.expect(requestTo(BASE + "/api/v1/executions/pending")).andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Actor-Id", "tech-3")).andExpect(header("X-Actor-Role", "RADIOLOGY_TECHNICIAN"))
                .andRespond(withSuccess("""
                        [{"orderId":"ORD-DEMO-CT","patientId":"PAT-DEMO-2","examTypeCode":"CT-ABD-C",
                          "examTypeName":"Abdominal CT with contrast","modality":"CT",
                          "scheduledAt":"2026-11-24T09:00:00Z","roomId":"CT-ROOM-1",
                          "requiresContrast":true,"requiresInformedConsent":true}]
                        """, MediaType.APPLICATION_JSON));

        List<ScheduledProcedureDTO> pending = client.listPending("tech-3", "RADIOLOGY_TECHNICIAN");

        assertThat(pending).singleElement().satisfies(p -> {
            assertThat(p.orderId()).isEqualTo("ORD-DEMO-CT");
            assertThat(p.requiresInformedConsent()).isTrue();
        });
        server.verify();
    }

    @Test
    void recordExecutionPostsThePayloadAndReadsTheRecord() {
        server.expect(requestTo(BASE + "/api/v1/executions")).andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Actor-Id", "dr-1"))
                .andExpect(jsonPath("$.orderId").value("ORD-DEMO-CT"))
                .andExpect(jsonPath("$.aborted").value(true))
                .andExpect(jsonPath("$.incidents[0].severity").value("SEVERE"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"executionId":"EXE-1","orderId":"ORD-DEMO-CT","patientId":"PAT-DEMO-2",
                         "examTypeName":"Abdominal CT with contrast","modality":"CT",
                         "startedAt":"2026-11-24T09:05:00Z","endedAt":"2026-11-24T09:21:00Z","roomEquipmentId":"CT-1",
                         "informedConsentVerified":true,"status":"INCOMPLETE_INCIDENT","abortReason":"Severe reaction",
                         "incidents":[{"incidentId":"INC-1","executionId":"EXE-1","patientId":"PAT-DEMO-2",
                                       "type":"ANAPHYLACTOID_EVENT","severity":"SEVERE","description":"Reaction",
                                       "occurredAt":"2026-11-24T09:21:00Z","procedureAborted":true}],
                         "recordedBy":"dr-1","recordedAt":"2026-11-24T09:30:00Z"}
                        """));

        ExecutionRecordDTO record = client.recordExecution(PAYLOAD, "dr-1", "PHYSICIAN");

        assertThat(record.status()).isEqualTo(ExecutionStatus.INCOMPLETE_INCIDENT);
        assertThat(record.incidents()).singleElement().satisfies(i -> assertThat(i.procedureAborted()).isTrue());
        server.verify();
    }

    @Test
    void invalidDataCarriesTheServerFields() {
        server.expect(requestTo(BASE + "/api/v1/executions"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Invalid.\",\"invalidFields\":[\"doseDlp\",\"informedConsentVerified\"]}"));

        assertThatThrownBy(() -> client.recordExecution(PAYLOAD, "dr-1", "PHYSICIAN"))
                .isInstanceOfSatisfying(ExecutionClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(ExecutionClientException.Kind.INVALID_DATA);
                    assertThat(ex.getMessage()).isEqualTo("Invalid.");
                    assertThat(ex.getInvalidFields()).containsExactly("doseDlp", "informedConsentVerified");
                    assertThat(ex.canRetry()).isFalse();
                });
    }

    @Test
    void statusCodesAreTranslatedWithDefaultMessages() {
        Object[][] cases = {
                {HttpStatus.FORBIDDEN, ExecutionClientException.Kind.FORBIDDEN, "not authorized"},
                {HttpStatus.NOT_FOUND, ExecutionClientException.Kind.PROCEDURE_NOT_FOUND, "not booked"},
                {HttpStatus.CONFLICT, ExecutionClientException.Kind.ALREADY_RECORDED, "already recorded"},
                {HttpStatus.INTERNAL_SERVER_ERROR, ExecutionClientException.Kind.FAILED, "could not be recorded"},
                {HttpStatus.BAD_REQUEST, ExecutionClientException.Kind.INVALID_DATA, "not valid"}};
        for (Object[] c : cases) {
            server.reset();
            server.expect(requestTo(BASE + "/api/v1/executions")).andRespond(withStatus((HttpStatus) c[0]).body("x"));
            assertThatThrownBy(() -> client.recordExecution(PAYLOAD, "dr-1", "PHYSICIAN"))
                    .isInstanceOfSatisfying(ExecutionClientException.class, ex -> {
                        assertThat(ex.getKind()).isEqualTo(c[1]);
                        assertThat(ex.getMessage()).contains((String) c[2]);
                    });
        }
    }

    @Test
    void pendingErrorsAndUnreachableServer() {
        server.expect(requestTo(BASE + "/api/v1/executions/pending"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":null}"));
        assertThatThrownBy(() -> client.listPending(null, null))
                .isInstanceOfSatisfying(ExecutionClientException.class,
                        ex -> assertThat(ex.getKind()).isEqualTo(ExecutionClientException.Kind.FORBIDDEN));

        server.reset();
        server.expect(requestTo(BASE + "/api/v1/executions"))
                .andRespond(withException(new java.net.ConnectException("refused")));
        assertThatThrownBy(() -> client.recordExecution(PAYLOAD, "dr-1", "PHYSICIAN"))
                .isInstanceOfSatisfying(ExecutionClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(ExecutionClientException.Kind.SERVER_UNREACHABLE);
                    assertThat(ex.getMessage()).contains("Nothing has been recorded");
                    assertThat(ex.canRetry()).isTrue();
                    assertThat(ex).hasCauseInstanceOf(ResourceAccessException.class);
                });
    }
}
