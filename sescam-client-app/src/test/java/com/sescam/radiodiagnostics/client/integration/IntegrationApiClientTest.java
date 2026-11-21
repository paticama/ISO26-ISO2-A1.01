package com.sescam.radiodiagnostics.client.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sescam.radiodiagnostics.common.integration.ConnectionCheckResultDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationSettingsDTO;
import com.sescam.radiodiagnostics.common.integration.IntegrationStatus;
import com.sescam.radiodiagnostics.common.integration.IntegrationSummaryDTO;
import com.sescam.radiodiagnostics.common.integration.SettingsCheckResultDTO;
import com.sescam.radiodiagnostics.common.integration.UpdateIntegrationResponseDTO;
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

class IntegrationApiClientTest {

    private static final String BASE = "http://server";
    private static final IntegrationSettingsDTO SETTINGS =
            new IntegrationSettingsDTO("https://sso.hospital.example/auth", "svc", "S3cret-Value-123");
    private static final String SUMMARY = """
            {"id":"sso","type":"SSO","displayName":"Single sign-on (SSO)","endpointUrl":"https://sso.hospital.example/auth",
             "username":"svc","credentialConfigured":true,"status":"CONNECTED","statusMessage":"ok","version":2,
             "lastCheckedAt":"2026-11-21T12:00:00Z","lastModifiedBy":"admin-1","lastModifiedAt":"2026-11-21T12:00:00Z"}
            """;

    private MockRestServiceServer server;
    private IntegrationApiClient client;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(c -> c.add(0, new MappingJackson2HttpMessageConverter(mapper)));
        server = MockRestServiceServer.bindTo(builder).build();
        client = new IntegrationApiClient(builder, mapper, BASE);
    }

    @Test
    @DisplayName("list and get send the actor headers and read the DTOs; the status is visible")
    void listAndGet() {
        server.expect(requestTo(BASE + "/api/v1/integrations"))
                .andExpect(header("X-Actor-Id", "admin-1")).andExpect(header("X-Actor-Role", "ADMINISTRATOR"))
                .andRespond(withSuccess("[" + SUMMARY + "]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/v1/integrations/sso"))
                .andRespond(withSuccess(SUMMARY, MediaType.APPLICATION_JSON));

        List<IntegrationSummaryDTO> list = client.list("admin-1", "ADMINISTRATOR");
        IntegrationSummaryDTO one = client.get("sso", "admin-1", "ADMINISTRATOR");

        assertThat(list).singleElement().satisfies(i -> assertThat(i.status()).isEqualTo(IntegrationStatus.CONNECTED));
        assertThat(one.credentialConfigured()).isTrue();
        assertThat(one.version()).isEqualTo(2L);
        server.verify();
    }

    @Test
    @DisplayName("validate posts the settings and reads the check result")
    void validate() {
        server.expect(requestTo(BASE + "/api/v1/integrations/sso/validate")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.endpointUrl").value("https://sso.hospital.example/auth"))
                .andExpect(jsonPath("$.secret").value("S3cret-Value-123"))
                .andRespond(withSuccess("""
                        {"integrationId":"sso","valid":true,"errors":[],"connection":
                         {"integrationId":"sso","status":"UNAVAILABLE","message":"No answer.","checkedAt":"2026-11-21T12:00:00Z"}}
                        """, MediaType.APPLICATION_JSON));

        SettingsCheckResultDTO result = client.validate("sso", SETTINGS, "admin-1", "ADMINISTRATOR");

        assertThat(result.valid()).isTrue();
        assertThat(result.connection().status()).isEqualTo(IntegrationStatus.UNAVAILABLE);
        assertThat(result.canSave()).isFalse();
    }

    @Test
    @DisplayName("update sends settings with the expected version and reads the confirmation")
    void update() {
        server.expect(requestTo(BASE + "/api/v1/integrations/sso")).andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.expectedVersion").value(1))
                .andExpect(jsonPath("$.settings.username").value("svc"))
                .andRespond(withSuccess("""
                        {"integrationId":"sso","status":"NOT_CHECKED","message":"Not tested.","newVersion":2,
                         "updatedBy":"admin-1","updatedAt":"2026-11-21T12:00:00Z"}
                        """, MediaType.APPLICATION_JSON));

        UpdateIntegrationResponseDTO response = client.update("sso", SETTINGS, 1L, "admin-1", "ADMINISTRATOR");

        assertThat(response.newVersion()).isEqualTo(2L);
        assertThat(response.status()).isEqualTo(IntegrationStatus.NOT_CHECKED);
    }

    @Test
    @DisplayName("check posts to /check and reads the result")
    void check() {
        server.expect(requestTo(BASE + "/api/v1/integrations/sso/check")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"integrationId":"sso","status":"CONNECTED","message":"ok","checkedAt":"2026-11-21T12:00:00Z"}
                        """, MediaType.APPLICATION_JSON));

        ConnectionCheckResultDTO result = client.check("sso", "admin-1", "ADMINISTRATOR");

        assertThat(result.status()).isEqualTo(IntegrationStatus.CONNECTED);
    }

    @Test
    @DisplayName("A1: 403 becomes FORBIDDEN with the server message")
    void forbidden() {
        server.expect(requestTo(BASE + "/api/v1/integrations")).andRespond(withStatus(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"FORBIDDEN\",\"message\":\"Not allowed.\"}"));

        assertThatThrownBy(() -> client.list("patient-1", "PATIENT"))
                .isInstanceOfSatisfying(IntegrationClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(IntegrationClientException.Kind.FORBIDDEN);
                    assertThat(ex.getMessage()).isEqualTo("Not allowed.");
                    assertThat(ex.canRetry()).isFalse();
                });
    }

    @Test
    @DisplayName("A2: 400 becomes INVALID_SETTINGS with the list of problems; the administrator must fix the settings")
    void invalidSettings() {
        server.expect(requestTo(BASE + "/api/v1/integrations/sso")).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"INVALID_SETTINGS\",\"message\":\"Invalid.\",\"errors\":[\"Endpoint URL must use https.\"]}"));

        assertThatThrownBy(() -> client.update("sso", SETTINGS, 1L, "admin-1", "ADMINISTRATOR"))
                .isInstanceOfSatisfying(IntegrationClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(IntegrationClientException.Kind.INVALID_SETTINGS);
                    assertThat(ex.getErrors()).containsExactly("Endpoint URL must use https.");
                    assertThat(ex.mustFixSettings()).isTrue();
                });
    }

    @Test
    @DisplayName("A2/A3: 502 tells a refused connection (fix settings) from an unreachable system (retry)")
    void connectionFailed() {
        server.expect(requestTo(BASE + "/api/v1/integrations/sso")).andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"CONNECTION_FAILED\",\"status\":\"CONNECTION_FAILED\",\"message\":\"Rejected.\"}"));
        server.expect(requestTo(BASE + "/api/v1/integrations/sso")).andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"CONNECTION_FAILED\",\"status\":\"UNAVAILABLE\",\"message\":\"No answer.\"}"));

        assertThatThrownBy(() -> client.update("sso", SETTINGS, 1L, "a", "r"))
                .isInstanceOfSatisfying(IntegrationClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(IntegrationClientException.Kind.CONNECTION_FAILED);
                    assertThat(ex.mustFixSettings()).isTrue();
                    assertThat(ex.canRetry()).isFalse();
                });
        assertThatThrownBy(() -> client.update("sso", SETTINGS, 1L, "a", "r"))
                .isInstanceOfSatisfying(IntegrationClientException.class, ex -> {
                    assertThat(ex.getConnectionStatus()).isEqualTo("UNAVAILABLE");
                    assertThat(ex.canRetry()).isTrue();
                    assertThat(ex.mustFixSettings()).isFalse();
                });
    }

    @Test
    @DisplayName("404, 409 and 500 are translated; a missing message gets a default text")
    void otherErrors() {
        server.expect(requestTo(BASE + "/api/v1/integrations/nope")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(BASE + "/api/v1/integrations/sso")).andRespond(withStatus(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON).body("not json"));
        server.expect(requestTo(BASE + "/api/v1/integrations/sso")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.get("nope", "a", "r")).isInstanceOfSatisfying(IntegrationClientException.class,
                ex -> assertThat(ex.getKind()).isEqualTo(IntegrationClientException.Kind.NOT_FOUND));
        assertThatThrownBy(() -> client.update("sso", SETTINGS, 1L, "a", "r")).isInstanceOfSatisfying(IntegrationClientException.class,
                ex -> {
                    assertThat(ex.getKind()).isEqualTo(IntegrationClientException.Kind.CONFLICT);
                    assertThat(ex.getMessage()).contains("Reload");
                });
        assertThatThrownBy(() -> client.update("sso", SETTINGS, 1L, "a", "r")).isInstanceOfSatisfying(IntegrationClientException.class,
                ex -> {
                    assertThat(ex.getKind()).isEqualTo(IntegrationClientException.Kind.SAVE_FAILED);
                    assertThat(ex.canRetry()).isTrue();
                });
    }

    @Test
    @DisplayName("A3: an unreachable server becomes SERVER_UNREACHABLE and can be retried")
    void serverUnreachable() {
        server.expect(requestTo(BASE + "/api/v1/integrations")).andRespond(withException(new IOException("refused")));

        assertThatThrownBy(() -> client.list("a", "r")).isInstanceOfSatisfying(IntegrationClientException.class, ex -> {
            assertThat(ex.getKind()).isEqualTo(IntegrationClientException.Kind.SERVER_UNREACHABLE);
            assertThat(ex.canRetry()).isTrue();
        });
    }

    @Test
    @DisplayName("Null actor headers are sent as empty strings")
    void nullActor() {
        server.expect(requestTo(BASE + "/api/v1/integrations")).andExpect(header("X-Actor-Id", ""))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.list(null, null)).isEmpty();
    }
}
