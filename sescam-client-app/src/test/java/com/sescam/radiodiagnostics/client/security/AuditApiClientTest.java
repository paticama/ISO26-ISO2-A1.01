package com.sescam.radiodiagnostics.client.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sescam.radiodiagnostics.common.security.AuditResultStatus;
import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.security.AuditSearchResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuditApiClientTest {

    private static final String BASE = "http://server";
    private static final AuditSearchCriteriaDTO CRITERIA = new AuditSearchCriteriaDTO(
            Instant.parse("2026-11-20T00:00:00Z"), Instant.parse("2026-11-20T23:00:00Z"), "admin-1", null, null);

    private MockRestServiceServer server;
    private AuditApiClient client;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(c -> c.add(0, new MappingJackson2HttpMessageConverter(mapper)));
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AuditApiClient(builder, mapper, BASE);
    }

    @Test
    void searchSendsFiltersAndIdentityAndReadsTheResult() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE + "/api/v1/audit/events?")))
                .andExpect(queryParam("from", "2026-11-20T00:00:00Z"))
                .andExpect(queryParam("performedBy", "admin-1"))
                .andExpect(header("X-Actor-Id", "admin-1"))
                .andExpect(header("X-Actor-Role", "ADMINISTRATOR"))
                .andRespond(withSuccess("""
                        {"criteria":null,"status":"MATCHES_FOUND","totalMatches":1,"events":[
                          {"eventId":"e1","sequenceNumber":1,"timestamp":"2026-11-20T08:00:00Z","actorId":"admin-1",
                           "action":"LOGIN","resourceKey":null,"previousValue":null,"newValue":null,"details":null}]}
                        """, MediaType.APPLICATION_JSON));

        AuditSearchResultDTO result = client.search(CRITERIA, "admin-1", "ADMINISTRATOR");

        assertThat(result.status()).isEqualTo(AuditResultStatus.MATCHES_FOUND);
        assertThat(result.events()).singleElement()
                .satisfies(e -> assertThat(e.timestamp()).isEqualTo(Instant.parse("2026-11-20T08:00:00Z")));
        server.verify();
    }

    @Test
    void forbiddenIsTranslatedWithTheServerMessage() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE)))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"FORBIDDEN\",\"message\":\"Not allowed.\"}"));

        assertThatThrownBy(() -> client.search(CRITERIA, "patient-1", "PATIENT"))
                .isInstanceOfSatisfying(AuditClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(AuditClientException.Kind.FORBIDDEN);
                    assertThat(ex.getMessage()).isEqualTo("Not allowed.");
                    assertThat(ex.canRefine()).isFalse();
                    assertThat(ex.canRetry()).isFalse();
                });
    }

    @Test
    void tooBroadAndInvalidCanBeRefined() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE)))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).body("not json"));
        assertThatThrownBy(() -> client.search(CRITERIA, "admin-1", "ADMINISTRATOR"))
                .isInstanceOfSatisfying(AuditClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(AuditClientException.Kind.QUERY_TOO_BROAD);
                    assertThat(ex.getMessage()).contains("too broad");
                    assertThat(ex.canRefine()).isTrue();
                });

        server.reset();
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE)))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> client.exportCsv(CRITERIA, "admin-1", "ADMINISTRATOR"))
                .isInstanceOfSatisfying(AuditClientException.class,
                        ex -> assertThat(ex.getKind()).isEqualTo(AuditClientException.Kind.INVALID_QUERY));
    }

    @Test
    void serverFailureAndUnreachableServerCanBeRetried() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE)))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"QUERY_FAILED\"}"));
        assertThatThrownBy(() -> client.search(CRITERIA, null, null))
                .isInstanceOfSatisfying(AuditClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(AuditClientException.Kind.QUERY_FAILED);
                    assertThat(ex.canRetry()).isTrue();
                });

        server.reset();
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE)))
                .andRespond(withException(new java.net.ConnectException("refused")));
        assertThatThrownBy(() -> client.search(CRITERIA, "admin-1", "ADMINISTRATOR"))
                .isInstanceOfSatisfying(AuditClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(AuditClientException.Kind.SERVER_UNREACHABLE);
                    assertThat(ex).hasCauseInstanceOf(ResourceAccessException.class);
                });
    }

    @Test
    void exportReturnsTheCsvBody() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE + "/api/v1/audit/events/export?")))
                .andRespond(withSuccess("eventId,sequenceNumber\ne1,1\n", MediaType.parseMediaType("text/csv")));

        assertThat(client.exportCsv(CRITERIA, "admin-1", "ADMINISTRATOR")).startsWith("eventId");
    }
}
