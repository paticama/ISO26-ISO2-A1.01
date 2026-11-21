package com.sescam.radiodiagnostics.client.catalogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sescam.radiodiagnostics.common.catalogue.ActivationMode;
import com.sescam.radiodiagnostics.common.catalogue.ActivationStatus;
import com.sescam.radiodiagnostics.common.catalogue.ConcurrentConfigurationChangeException;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationNotFoundException;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationParameterDTO;
import com.sescam.radiodiagnostics.common.catalogue.ConfigurationSaveException;
import com.sescam.radiodiagnostics.common.catalogue.InvalidConfigurationValueException;
import com.sescam.radiodiagnostics.common.catalogue.ParameterValueType;
import com.sescam.radiodiagnostics.common.catalogue.UnauthorizedConfigurationAccessException;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterRequestDTO;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterResponseDTO;
import com.sescam.radiodiagnostics.common.catalogue.ValidationResultDTO;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HttpConfigurationRestClientTest {

    private HttpServer server;
    private HttpConfigurationRestClient client;
    private ObjectMapper mapper;
    private AtomicInteger nextStatus;
    private AtomicReference<String> nextBody;

    @BeforeEach
    void setUp() throws IOException {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        nextStatus = new AtomicInteger(200);
        nextBody = new AtomicReference<>("[]");

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/configurations", exchange -> {
            byte[] bytes = nextBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(nextStatus.get(), bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        client = new HttpConfigurationRestClient("http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("GET, POST /validate, and PUT deserialize sescam-common DTOs over HTTP")
    void successfulRestCallsDeserializeDtos() throws Exception {
        ConfigurationParameterDTO param = new ConfigurationParameterDTO(
                "triage.sla", "Triage SLA", "TRIAGE", "desc", "4",
                ParameterValueType.INTEGER, 1.0, 48.0, List.of(),
                ActivationMode.IMMEDIATE, 1L, "system", Instant.parse("2026-11-20T18:00:00Z")
        );
        nextStatus.set(200);
        nextBody.set(mapper.writeValueAsString(List.of(param)));
        List<ConfigurationParameterDTO> list = client.fetchAllParameters("admin-1", "ADMINISTRATOR");
        assertEquals(1, list.size());
        assertEquals("triage.sla", list.get(0).key());

        nextBody.set(mapper.writeValueAsString(ValidationResultDTO.ok("triage.sla")));
        ValidationResultDTO val = client.validateRemotely("triage.sla", "6", "admin-1", "ADMINISTRATOR");
        assertTrue(val.valid());

        UpdateParameterResponseDTO resp = new UpdateParameterResponseDTO(
                "triage.sla", "4", "6", 2L, ActivationStatus.ACTIVE,
                "Saved", "admin-1", Instant.parse("2026-11-20T19:00:00Z")
        );
        nextBody.set(mapper.writeValueAsString(resp));
        UpdateParameterResponseDTO actual = client.updateParameter(
                new UpdateParameterRequestDTO("triage.sla", "6", 1L, "admin-1", "ADMINISTRATOR")
        );
        assertEquals("6", actual.currentValue());
        assertEquals(ActivationStatus.ACTIVE, actual.activationStatus());
    }

    @Test
    @DisplayName("HTTP 400, 403, 404, 409, and 500 responses map to exact sescam-common exceptions")
    void errorStatusesMapToDomainExceptions() {
        UpdateParameterRequestDTO req = new UpdateParameterRequestDTO("triage.sla", "6", 1L, "admin-1", "ADMINISTRATOR");

        nextStatus.set(400);
        nextBody.set("{\"message\":\"Out of range\"}");
        assertThrows(InvalidConfigurationValueException.class, () -> client.updateParameter(req));

        nextStatus.set(403);
        nextBody.set("{\"message\":\"Forbidden\"}");
        assertThrows(UnauthorizedConfigurationAccessException.class, () -> client.fetchAllParameters("u", "PATIENT"));

        nextStatus.set(404);
        nextBody.set("{\"message\":\"Not found\"}");
        assertThrows(ConfigurationNotFoundException.class, () -> client.validateRemotely("missing", "1", "a", "ADMIN"));

        nextStatus.set(409);
        nextBody.set("{\"parameterKey\":\"triage.sla\",\"expectedVersion\":1,\"actualVersion\":2,\"currentStoredValue\":\"8\",\"message\":\"Conflict\"}");
        ConcurrentConfigurationChangeException conflict = assertThrows(
                ConcurrentConfigurationChangeException.class, () -> client.updateParameter(req)
        );
        assertEquals("8", conflict.getCurrentStoredValue());
        assertEquals(2L, conflict.getActualVersion());

        nextStatus.set(500);
        nextBody.set("{\"previousValue\":\"4\",\"message\":\"Disk error\"}");
        ConfigurationSaveException saveEx = assertThrows(
                ConfigurationSaveException.class, () -> client.updateParameter(req)
        );
        assertEquals("4", saveEx.getPreviousValue());
    }
}
