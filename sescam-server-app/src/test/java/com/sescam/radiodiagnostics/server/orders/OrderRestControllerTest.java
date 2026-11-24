package com.sescam.radiodiagnostics.server.orders;

import com.sescam.radiodiagnostics.common.orders.ConstraintType;
import com.sescam.radiodiagnostics.common.orders.OrderCreatedEvent;
import com.sescam.radiodiagnostics.common.orders.OrderEventPublisher;
import com.sescam.radiodiagnostics.common.orders.PatientSafetyGateway;
import com.sescam.radiodiagnostics.common.orders.SafetyConstraintDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OrderRestControllerTest {

    private static final String DOCTOR = "dr-1";
    private static final String ROLE = "PHYSICIAN";

    private static String json(String exam, String override) {
        return """
                {
                  "patientId": "PAT-1",
                  "examTypeCode": "%s",
                  "clinicalIndication": "Persistent cough",
                  "clinicalJustification": "Rule out pneumonia",
                  "priority": "PREFERENTIAL"%s
                }
                """.formatted(exam, override == null ? "" : ",\n  \"overrideJustification\": \"" + override + "\"");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientSafetyGateway safetyGateway;

    @MockitoBean
    private OrderEventPublisher eventPublisher;

    @Test
    @DisplayName("GET /api/v1/orders/exam-types lists the active catalogue (step 2)")
    void listsCatalogue() throws Exception {
        mockMvc.perform(get("/api/v1/orders/exam-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("XR-CHEST")))
                .andExpect(jsonPath("$[*].code", not(hasItem("XR-OLD"))));
    }

    @Test
    @DisplayName("POST /api/v1/orders creates a PENDING_SCHEDULING order (201) and publishes it for admissions")
    void createsOrder() throws Exception {
        when(safetyGateway.getActiveConstraints(any())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-Actor-Id", DOCTOR).header("X-Actor-Role", ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content(json("XR-CHEST", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING_SCHEDULING")))
                .andExpect(jsonPath("$.orderNumber", containsString("PHY-")))
                .andExpect(jsonPath("$.examTypeCode", is("XR-CHEST")))
                .andExpect(jsonPath("$.orderedBy", is(DOCTOR)))
                .andExpect(jsonPath("$.consent", nullValue()))
                .andExpect(jsonPath("$.overrideApproved", is(false)));

        verify(eventPublisher).publishOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("A6a: contrast examination returns the consent and preparation attachment")
    void attachesConsent() throws Exception {
        when(safetyGateway.getActiveConstraints(any())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-Actor-Id", DOCTOR).header("X-Actor-Role", ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content(json("CT-ABD-C", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiresContrast", is(true)))
                .andExpect(jsonPath("$.consent.informedConsentRequired", is(true)))
                .andExpect(jsonPath("$.consent.consentTemplateId", is("CONSENT-CONTRAST")))
                .andExpect(jsonPath("$.consent.fastingRequired", is(true)));
    }

    @Test
    @DisplayName("A6b: safety alert returns 422 with the constraints; resubmitting with a justification returns 201")
    void safetyAlertThenOverride() throws Exception {
        when(safetyGateway.getActiveConstraints("PAT-1")).thenReturn(List.of(
                new SafetyConstraintDTO("c-1", ConstraintType.FERROMAGNETIC_IMPLANT, true, "Pacemaker")));

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-Actor-Id", DOCTOR).header("X-Actor-Role", ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content(json("MR-BRAIN", null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error", is("SAFETY_ALERT")))
                .andExpect(jsonPath("$.constraints", hasSize(1)))
                .andExpect(jsonPath("$.constraints[0].type", is("FERROMAGNETIC_IMPLANT")));
        verify(eventPublisher, never()).publishOrderCreated(any());

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-Actor-Id", DOCTOR).header("X-Actor-Role", ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content(json("MR-BRAIN", "MR-conditional device")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overrideApproved", is(true)))
                .andExpect(jsonPath("$.overrideJustification", is("MR-conditional device")));
        verify(eventPublisher).publishOrderCreated(any());
    }

    @Test
    @DisplayName("A4a: missing mandatory data returns 400 with the invalid fields")
    void missingData() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("X-Actor-Id", DOCTOR).header("X-Actor-Role", ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"PAT-1\",\"examTypeCode\":\"XR-CHEST\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_DATA")))
                .andExpect(jsonPath("$.invalidFields", hasItem("clinicalJustification")))
                .andExpect(jsonPath("$.invalidFields", hasItem("priority")));
    }

    @Test
    @DisplayName("Other roles are rejected with 403 (also when the headers are missing)")
    void forbiddenRoles() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("X-Actor-Id", "admin-1").header("X-Actor-Role", "ADMINISTRATOR")
                        .contentType(MediaType.APPLICATION_JSON).content(json("XR-CHEST", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(json("XR-CHEST", null)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unknown or inactive examination types return 404")
    void unknownExam() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("X-Actor-Id", DOCTOR).header("X-Actor-Role", ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content(json("XR-OLD", null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("EXAM_TYPE_NOT_FOUND")));
    }
}
