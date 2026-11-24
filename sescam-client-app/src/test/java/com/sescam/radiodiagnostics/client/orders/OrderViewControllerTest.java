package com.sescam.radiodiagnostics.client.orders;

import com.sescam.radiodiagnostics.common.catalogue.ExamTypeDTO;
import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.orders.ConsentAndPreparationDTO;
import com.sescam.radiodiagnostics.common.orders.ConstraintType;
import com.sescam.radiodiagnostics.common.orders.OrderResponseDTO;
import com.sescam.radiodiagnostics.common.orders.OrderStatus;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import com.sescam.radiodiagnostics.common.orders.SafetyConstraintDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(OrderViewController.class)
class OrderViewControllerTest {

    private static final ExamTypeDTO XR = new ExamTypeDTO("XR-CHEST", "Chest X-ray", ModalityType.X_RAY, 10,
            null, false, false, false, null, true);
    private static final ExamTypeDTO MR = new ExamTypeDTO("MR-BRAIN", "Brain MRI", ModalityType.MRI, 45,
            null, false, false, false, null, true);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderApiClient apiClient;

    @BeforeEach
    void catalogue() {
        when(apiClient.listExamTypes()).thenReturn(List.of(XR, MR));
    }

    private static MockHttpServletRequestBuilder submit(String exam) {
        return post("/orders")
                .param("patientId", "PAT-1").param("examTypeCode", exam)
                .param("clinicalIndication", "cough").param("clinicalJustification", "rule out pneumonia")
                .param("priority", "PREFERENTIAL")
                .header("X-Actor-Id", "dr-1").header("X-Actor-Role", "PHYSICIAN");
    }

    private static OrderResponseDTO order(ConsentAndPreparationDTO consent, boolean override) {
        return new OrderResponseDTO("ORD-1", "PHY-2026-AB12CD34", "PAT-1", "XR-CHEST", "Chest X-ray",
                ModalityType.X_RAY, "cough", "rule out pneumonia", PriorityLevel.PREFERENTIAL,
                OrderStatus.PENDING_SCHEDULING, consent != null, consent, override,
                override ? "MR-conditional device" : null, "dr-1", Instant.parse("2026-11-25T10:15:30Z"), 1L);
    }

    @Test
    void newOrderShowsTheCatalogueAndThePriorities() throws Exception {
        mockMvc.perform(get("/orders/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/new"))
                .andExpect(content().string(containsString("Chest X-ray (X_RAY, 10 min)")))
                .andExpect(content().string(containsString("Brain MRI")))
                .andExpect(content().string(containsString("STAT_URGENT")))
                .andExpect(content().string(containsString("ROUTINE")));
    }

    @Test
    void catalogueFailureIsShownAndTheFormStillOpens() throws Exception {
        when(apiClient.listExamTypes()).thenThrow(new OrderClientException(
                OrderClientException.Kind.SERVER_UNREACHABLE, "The server cannot be reached."));

        mockMvc.perform(get("/orders/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"catalogue-failure\"")))
                .andExpect(content().string(containsString("The server cannot be reached.")));
    }

    @Test
    void successShowsTheTrackingNumberAndNoForm() throws Exception {
        when(apiClient.createOrder(any(), eq("dr-1"), eq("PHYSICIAN"))).thenReturn(order(null, false));

        mockMvc.perform(submit("XR-CHEST"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"confirmation\"")))
                .andExpect(content().string(containsString("PHY-2026-AB12CD34")))
                .andExpect(content().string(containsString("PENDING_SCHEDULING")))
                .andExpect(content().string(not(containsString("id=\"order-form\""))))
                .andExpect(content().string(not(containsString("id=\"consent\""))));

        verify(apiClient).createOrder(new OrderApiClient.OrderPayload(
                "PAT-1", "XR-CHEST", "cough", "rule out pneumonia", "PREFERENTIAL", null), "dr-1", "PHYSICIAN");
    }

    @Test
    void a6aConsentAndPreparationAreShownInTheConfirmation() throws Exception {
        when(apiClient.createOrder(any(), any(), any())).thenReturn(order(new ConsentAndPreparationDTO(
                true, "CONSENT-CONTRAST", "Fast for 6 hours.", true, true), false));

        mockMvc.perform(submit("CT-ABD-C"))
                .andExpect(content().string(containsString("id=\"consent\"")))
                .andExpect(content().string(containsString("Informed consent attached")))
                .andExpect(content().string(containsString("CONSENT-CONTRAST")))
                .andExpect(content().string(containsString("Contrast medium will be used.")))
                .andExpect(content().string(containsString("The patient must fast.")))
                .andExpect(content().string(containsString("Fast for 6 hours.")));
    }

    @Test
    void a4aMissingDataIsBlockedOnTheClientAndNeverSent() throws Exception {
        mockMvc.perform(post("/orders").param("patientId", "PAT-1").param("examTypeCode", "XR-CHEST")
                        .param("clinicalIndication", "cough"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The clinical justification is mandatory.")))
                .andExpect(content().string(containsString("Select the priority level.")))
                .andExpect(content().string(containsString("id=\"order-form\"")))
                .andExpect(content().string(not(containsString("id=\"confirmation\""))));

        verify(apiClient).listExamTypes();
        verifyNoMoreInteractions(apiClient);
    }

    @Test
    void a6bSafetyAlertShowsTheConstraintsAndAsksForAnOverrideOrCancel() throws Exception {
        when(apiClient.createOrder(any(), any(), any())).thenThrow(new OrderClientException(
                OrderClientException.Kind.SAFETY_ALERT, "Incompatible with Brain MRI.",
                List.of(new SafetyConstraintDTO("c-1", ConstraintType.FERROMAGNETIC_IMPLANT, true, "Pacemaker")),
                List.of(), null));

        mockMvc.perform(submit("MR-BRAIN"))
                .andExpect(content().string(containsString("id=\"safety-alert\"")))
                .andExpect(content().string(containsString("Incompatible with Brain MRI.")))
                .andExpect(content().string(containsString("FERROMAGNETIC_IMPLANT")))
                .andExpect(content().string(containsString("absolute contraindication")))
                .andExpect(content().string(containsString("Pacemaker")))
                .andExpect(content().string(containsString("id=\"overrideJustification\"")))
                .andExpect(content().string(containsString("Send with override")))
                .andExpect(content().string(containsString("Cancel order")))
                // the data the physician typed is kept
                .andExpect(content().string(containsString("rule out pneumonia")));
    }

    @Test
    void a6bResubmittingWithAJustificationSendsItAndShowsTheOverride() throws Exception {
        when(apiClient.createOrder(any(), any(), any())).thenReturn(order(null, true));

        mockMvc.perform(submit("MR-BRAIN").param("overrideJustification", "MR-conditional device"))
                .andExpect(content().string(containsString("id=\"override-note\"")))
                .andExpect(content().string(containsString("MR-conditional device")));

        verify(apiClient).createOrder(new OrderApiClient.OrderPayload("PAT-1", "MR-BRAIN", "cough",
                "rule out pneumonia", "PREFERENTIAL", "MR-conditional device"), "dr-1", "PHYSICIAN");
    }

    @Test
    void a6bCancelDiscardsTheFormAndStoresNothing() throws Exception {
        mockMvc.perform(post("/orders/cancel").param("patientId", "PAT-1").param("examTypeCode", "MR-BRAIN"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"cancelled\"")))
                .andExpect(content().string(containsString("Nothing was saved.")))
                .andExpect(content().string(not(containsString("value=\"PAT-1\""))));

        verify(apiClient).listExamTypes();
        verifyNoMoreInteractions(apiClient);
    }

    @Test
    void serverRejectionsAreShownWithTheRejectedFields() throws Exception {
        when(apiClient.createOrder(any(), any(), any())).thenThrow(new OrderClientException(
                OrderClientException.Kind.INVALID_DATA, "Mandatory order data is missing: priority.",
                List.of(), List.of("priority"), null));

        mockMvc.perform(submit("XR-CHEST"))
                .andExpect(content().string(containsString("id=\"failure\"")))
                .andExpect(content().string(containsString("Mandatory order data is missing: priority.")))
                .andExpect(content().string(containsString("This field is mandatory or not valid.")));
    }

    @Test
    void forbiddenAndUnreachableAreShownAndTheOrderCanBeSentAgain() throws Exception {
        when(apiClient.createOrder(any(), any(), any())).thenThrow(new OrderClientException(
                OrderClientException.Kind.FORBIDDEN, "Only physicians can order diagnostic procedures."));
        mockMvc.perform(submit("XR-CHEST"))
                .andExpect(content().string(containsString("Only physicians can order diagnostic procedures.")))
                .andExpect(content().string(not(containsString("You can send it again."))));

        org.mockito.Mockito.doThrow(new OrderClientException(
                OrderClientException.Kind.SERVER_UNREACHABLE, "The server cannot be reached."))
                .when(apiClient).createOrder(any(), any(), any());
        mockMvc.perform(submit("XR-CHEST"))
                .andExpect(content().string(containsString("The server cannot be reached.")))
                .andExpect(content().string(containsString("You can send it again.")))
                .andExpect(content().string(containsString("id=\"order-form\"")));
    }
}
