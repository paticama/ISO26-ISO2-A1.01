package com.sescam.radiodiagnostics.server.patients;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PatientRestControllerTest {

    private static final String ADM_ID = "admin-staff-1";
    private static final String ADM_ROLE = "ADMINISTRATIVE_STAFF";

    private static final String CREATE_JSON = """
            {
              "cip": "CIP99887766",
              "dni": "99887766A",
              "firstName": "Lucia",
              "lastName": "Navarro",
              "birthDate": "1994-08-25",
              "gender": "FEMALE",
              "phone": "655333222",
              "email": "lucia.navarro@sescam.jccm.es",
              "address": {
                "street": "Calle Alarcos 12",
                "postalCode": "13001",
                "city": "Ciudad Real",
                "province": "Ciudad Real"
              },
              "preferredChannel": "SMS",
              "emergencyContact": {
                "name": "Pedro Navarro",
                "relationship": "Father",
                "phone": "655000111"
              },
              "clinicalSummaryNotes": "Penicillin allergy"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/v1/patients creates new patient (201 Created) and prevents duplicates (409 Conflict)")
    void registerAndDuplicate() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                        .header("X-Actor-Id", ADM_ID)
                        .header("X-Actor-Role", ADM_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cip", is("CIP99887766")))
                .andExpect(jsonPath("$.dni", is("99887766A")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.ehrSyncStatus", is("SYNCED")));

        // Duplicate registration attempt (A1)
        mockMvc.perform(post("/api/v1/patients")
                        .header("X-Actor-Id", ADM_ID)
                        .header("X-Actor-Role", ADM_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("DUPLICATE_PATIENT")));
    }

    @Test
    @DisplayName("POST /api/v1/patients with invalid data returns 400 Bad Request")
    void registerInvalidData() throws Exception {
        String invalidJson = """
                {
                  "cip": "SHORT",
                  "dni": "INVALID_DNI",
                  "firstName": "",
                  "lastName": "Navarro",
                  "birthDate": "2050-01-01"
                }
                """;

        mockMvc.perform(post("/api/v1/patients")
                        .header("X-Actor-Id", ADM_ID)
                        .header("X-Actor-Role", ADM_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_DATA")));
    }

    @Test
    @DisplayName("Non-administrative role is rejected with 403 Forbidden (A5)")
    void unauthorizedRejected() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                        .header("X-Actor-Id", "patient-user")
                        .header("X-Actor-Role", "PATIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("GET /api/v1/patients searches patients and retrieves details by ID and CIP")
    void searchAndGet() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/patients")
                        .header("X-Actor-Id", ADM_ID)
                        .header("X-Actor-Role", ADM_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        String patientId = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/patients")
                        .header("X-Actor-Id", ADM_ID)
                        .header("X-Actor-Role", ADM_ROLE)
                        .param("query", "Navarro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName", is("Lucia Navarro")));

        mockMvc.perform(get("/api/v1/patients/" + patientId)
                        .header("X-Actor-Id", ADM_ID)
                        .header("X-Actor-Role", ADM_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(patientId)))
                .andExpect(jsonPath("$.cip", is("CIP99887766")));

        mockMvc.perform(get("/api/v1/patients/cip/CIP99887766")
                        .header("X-Actor-Id", ADM_ID)
                        .header("X-Actor-Role", ADM_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(patientId)));

        mockMvc.perform(get("/api/v1/patients/NON_EXISTENT")
                        .header("X-Actor-Id", ADM_ID)
                        .header("X-Actor-Role", ADM_ROLE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("PUT /api/v1/patients/{id} updates data and checks concurrent modifications (409 Conflict)")
    void updateAndConcurrency() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/patients")
                        .header("X-Actor-Id", ADM_ID)
                        .header("X-Actor-Role", ADM_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        String patientId = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id");

        String updateJson = """
                {
                  "firstName": "Lucia Maria",
                  "lastName": "Navarro Gomez",
                  "phone": "655999888",
                  "email": "lucia.maria@sescam.jccm.es",
                  "clinicalSummaryNotes": "Updated allergy notes",
                  "expectedVersion": 1
                }
                """;

        mockMvc.perform(put("/api/v1/patients/" + patientId)
                        .header("X-Actor-Id", ADM_ID)
                        .header("X-Actor-Role", ADM_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Lucia Maria")))
                .andExpect(jsonPath("$.version", is(2)));

        // Outdated expectedVersion (A4)
        mockMvc.perform(put("/api/v1/patients/" + patientId)
                        .header("X-Actor-Id", ADM_ID)
                        .header("X-Actor-Role", ADM_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("CONCURRENT_MODIFICATION")));
    }

    @Test
    @DisplayName("POST /api/v1/patients/{id}/discharge marks patient as DISCHARGED")
    void discharge() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/patients")
                        .header("X-Actor-Id", ADM_ID)
                        .header("X-Actor-Role", ADM_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        String patientId = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id");

        String dischargeJson = """
                {
                  "dischargeReason": "Patient deceased or relocated",
                  "expectedVersion": 1
                }
                """;

        mockMvc.perform(post("/api/v1/patients/" + patientId + "/discharge")
                        .header("X-Actor-Id", ADM_ID)
                        .header("X-Actor-Role", ADM_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dischargeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DISCHARGED")))
                .andExpect(jsonPath("$.version", is(2)));
    }

    @Test
    @DisplayName("EhrSyncException is mapped to 502 Bad Gateway by PatientExceptionHandler")
    void ehrSyncExceptionMapped() {
        PatientExceptionHandler handler = new PatientExceptionHandler();
        var response = handler.handleEhrSync(new com.sescam.radiodiagnostics.common.patients.EhrSyncException("EHR timeout"));
        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().value()).isEqualTo(502);
        org.assertj.core.api.Assertions.assertThat(response.getBody().get("error")).isEqualTo("EHR_SYNC_FAILED");
    }
}
