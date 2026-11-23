package com.sescam.radiodiagnostics.client.patients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sescam.radiodiagnostics.common.patients.CreatePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.DischargePatientRequestDTO;
import com.sescam.radiodiagnostics.common.patients.Gender;
import com.sescam.radiodiagnostics.common.patients.PatientDTO;
import com.sescam.radiodiagnostics.common.patients.PatientSummaryDTO;
import com.sescam.radiodiagnostics.common.patients.UpdatePatientRequestDTO;
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

class PatientApiClientTest {

    private static final String BASE = "http://server";
    private static final String PATIENT_JSON = """
            {
              "id": "p-1",
              "cip": "CIP12345678",
              "dni": "12345678Z",
              "firstName": "Carlos",
              "lastName": "Sanchez",
              "birthDate": "1990-01-01",
              "gender": "MALE",
              "phone": "600112233",
              "email": "carlos@sescam.es",
              "status": "ACTIVE",
              "version": 1,
              "ehrSyncStatus": "SYNCED"
            }
            """;

    private MockRestServiceServer server;
    private HttpPatientApiClient client;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(converters -> converters.add(0, new MappingJackson2HttpMessageConverter(mapper)));
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpPatientApiClient(builder, mapper, BASE);
    }

    @Test
    @DisplayName("Search calls GET /api/v1/patients with query parameters and headers")
    void search() {
        server.expect(requestTo("http://server/api/v1/patients?query=Sanchez&page=0&size=20"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Actor-Id", "staff-1"))
                .andExpect(header("X-Actor-Role", "ADMINISTRATIVE_STAFF"))
                .andRespond(withSuccess("[{\"id\":\"p-1\",\"cip\":\"CIP12345678\",\"dni\":\"12345678Z\",\"fullName\":\"Carlos Sanchez\",\"status\":\"ACTIVE\",\"version\":1}]", MediaType.APPLICATION_JSON));

        List<PatientSummaryDTO> results = client.search("Sanchez", null, 0, 20, "staff-1", "ADMINISTRATIVE_STAFF");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).fullName()).isEqualTo("Carlos Sanchez");
        server.verify();
    }

    @Test
    @DisplayName("getById and getByCip parse patient details")
    void getByIdAndCip() {
        server.expect(requestTo("http://server/api/v1/patients/p-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(PATIENT_JSON, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://server/api/v1/patients/cip/CIP12345678"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(PATIENT_JSON, MediaType.APPLICATION_JSON));

        PatientDTO byId = client.getById("p-1", "staff-1", "ADMINISTRATIVE_STAFF");
        assertThat(byId.cip()).isEqualTo("CIP12345678");

        PatientDTO byCip = client.getByCip("CIP12345678", "staff-1", "ADMINISTRATIVE_STAFF");
        assertThat(byCip.id()).isEqualTo("p-1");
        server.verify();
    }

    @Test
    @DisplayName("Register calls POST /api/v1/patients and maps 409 conflict to CONFLICT")
    void registerAndConflict() {
        CreatePatientRequestDTO req = new CreatePatientRequestDTO(
                "CIP12345678", "12345678Z", "Carlos", "Sanchez", LocalDate.of(1990, 1, 1),
                Gender.MALE, "600112233", "carlos@sescam.es", null, null, null, null,
                "staff-1", "ADMINISTRATIVE_STAFF"
        );

        server.expect(requestTo("http://server/api/v1/patients"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.cip").value("CIP12345678"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"DUPLICATE_PATIENT\",\"message\":\"CIP already registered\"}"));

        assertThatThrownBy(() -> client.register(req))
                .isInstanceOf(PatientClientException.class)
                .matches(ex -> ((PatientClientException) ex).getKind() == PatientClientException.Kind.CONFLICT);

        server.verify();
    }

    @Test
    @DisplayName("Update calls PUT /api/v1/patients/{id} and maps 403 to FORBIDDEN")
    void updateAndForbidden() {
        UpdatePatientRequestDTO req = new UpdatePatientRequestDTO(
                "p-1", "Carlos", "Sanchez", "600112233", "carlos@sescam.es",
                null, null, null, "Notes", 1L, "staff-1", "STAFF"
        );

        server.expect(requestTo("http://server/api/v1/patients/p-1"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("X-Actor-Id", "staff-1"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"FORBIDDEN\",\"message\":\"Unauthorized\"}"));

        assertThatThrownBy(() -> client.update(req))
                .isInstanceOf(PatientClientException.class)
                .matches(ex -> ((PatientClientException) ex).getKind() == PatientClientException.Kind.FORBIDDEN);

        server.verify();
    }

    @Test
    @DisplayName("Discharge calls POST /api/v1/patients/{id}/discharge and maps 404 to NOT_FOUND")
    void dischargeAndNotFound() {
        DischargePatientRequestDTO req = new DischargePatientRequestDTO(
                "p-99", "Relocated", 1L, "staff-1", "ADMINISTRATIVE_STAFF"
        );

        server.expect(requestTo("http://server/api/v1/patients/p-99/discharge"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"PATIENT_NOT_FOUND\",\"message\":\"Not found\"}"));

        assertThatThrownBy(() -> client.discharge(req))
                .isInstanceOf(PatientClientException.class)
                .matches(ex -> ((PatientClientException) ex).getKind() == PatientClientException.Kind.NOT_FOUND);

        server.verify();
    }

    @Test
    @DisplayName("Bad gateway 502 maps to EHR_SYNC_FAILED and 400 maps to INVALID_DATA")
    void statusErrorMappings() {
        server.expect(requestTo("http://server/api/v1/patients/p-2"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"EHR_ERROR\",\"message\":\"EHR timeout\"}"));

        server.expect(requestTo("http://server/api/v1/patients/p-3"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"VALIDATION_FAILED\",\"errors\":[\"CIP mandatory\"]}"));

        assertThatThrownBy(() -> client.getById("p-2", "s", "r"))
                .isInstanceOf(PatientClientException.class)
                .matches(ex -> ((PatientClientException) ex).getKind() == PatientClientException.Kind.EHR_SYNC_FAILED);

        assertThatThrownBy(() -> client.getById("p-3", "s", "r"))
                .isInstanceOf(PatientClientException.class)
                .matches(ex -> ((PatientClientException) ex).getKind() == PatientClientException.Kind.INVALID_DATA
                        && ((PatientClientException) ex).getErrors().contains("CIP mandatory"));

        server.verify();
    }

    @Test
    @DisplayName("Network error maps to SERVER_UNREACHABLE")
    void networkError() {
        server.expect(requestTo("http://server/api/v1/patients/p-9"))
                .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> client.getById("p-9", "s", "r"))
                .isInstanceOf(PatientClientException.class)
                .matches(ex -> ((PatientClientException) ex).getKind() == PatientClientException.Kind.SERVER_UNREACHABLE);
    }
}
