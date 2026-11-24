package com.sescam.radiodiagnostics.client.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeDTO;
import com.sescam.radiodiagnostics.common.orders.ConstraintType;
import com.sescam.radiodiagnostics.common.orders.OrderResponseDTO;
import com.sescam.radiodiagnostics.common.orders.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

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

class OrderApiClientTest {

    private static final String BASE = "http://server";
    private static final OrderApiClient.OrderPayload PAYLOAD = new OrderApiClient.OrderPayload(
            "PAT-1", "MR-BRAIN", "headache", "rule out mass", "PREFERENTIAL", null);

    private static final String ORDER_JSON = """
            {"orderId":"ORD-1","orderNumber":"PHY-2026-AB12CD34","patientId":"PAT-1","examTypeCode":"CT-ABD-C",
             "examTypeName":"Abdominal CT with contrast","modality":"CT","clinicalIndication":"i","clinicalJustification":"j",
             "priority":"PREFERENTIAL","status":"PENDING_SCHEDULING","requiresContrast":true,
             "consent":{"informedConsentRequired":true,"consentTemplateId":"CONSENT-CONTRAST","preparationGuide":"Fast",
                        "fastingRequired":true,"contrastRequired":true},
             "overrideApproved":false,"overrideJustification":null,"orderedBy":"dr-1",
             "orderedAt":"2026-11-25T10:15:30Z","version":1}
            """;

    private MockRestServiceServer server;
    private OrderApiClient client;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(c -> c.add(0, new MappingJackson2HttpMessageConverter(mapper)));
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OrderApiClient(builder, mapper, BASE);
    }

    @Test
    void listExamTypesReadsTheCatalogue() {
        server.expect(requestTo(BASE + "/api/v1/orders/exam-types")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"code":"XR-CHEST","name":"Chest X-ray","modality":"X_RAY","estimatedDurationMinutes":10,
                          "preparationGuide":null,"requiresFasting":false,"requiresContrast":false,
                          "requiresInformedConsent":false,"consentTemplateId":null,"active":true}]
                        """, MediaType.APPLICATION_JSON));

        List<ExamTypeDTO> exams = client.listExamTypes();

        assertThat(exams).singleElement().satisfies(e -> {
            assertThat(e.code()).isEqualTo("XR-CHEST");
            assertThat(e.estimatedDurationMinutes()).isEqualTo(10);
        });
        server.verify();
    }

    @Test
    void createOrderSendsThePayloadAndTheActorAndReadsTheOrder() {
        server.expect(requestTo(BASE + "/api/v1/orders")).andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Actor-Id", "dr-1")).andExpect(header("X-Actor-Role", "PHYSICIAN"))
                .andExpect(jsonPath("$.patientId").value("PAT-1"))
                .andExpect(jsonPath("$.examTypeCode").value("MR-BRAIN"))
                .andExpect(jsonPath("$.priority").value("PREFERENTIAL"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(ORDER_JSON));

        OrderResponseDTO order = client.createOrder(PAYLOAD, "dr-1", "PHYSICIAN");

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_SCHEDULING);
        assertThat(order.orderNumber()).isEqualTo("PHY-2026-AB12CD34");
        assertThat(order.consent().consentTemplateId()).isEqualTo("CONSENT-CONTRAST");
        assertThat(order.orderedAt()).isEqualTo(java.time.Instant.parse("2026-11-25T10:15:30Z"));
        server.verify();
    }

    @Test
    void safetyAlertCarriesTheConstraints() {
        server.expect(requestTo(BASE + "/api/v1/orders"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON).body("""
                        {"error":"SAFETY_ALERT","message":"Incompatible with Brain MRI.","constraints":[
                          {"constraintId":"c-1","type":"FERROMAGNETIC_IMPLANT","absoluteContraindication":true,
                           "description":"Pacemaker"}]}
                        """));

        assertThatThrownBy(() -> client.createOrder(PAYLOAD, "dr-1", "PHYSICIAN"))
                .isInstanceOfSatisfying(OrderClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(OrderClientException.Kind.SAFETY_ALERT);
                    assertThat(ex.isSafetyAlert()).isTrue();
                    assertThat(ex.getMessage()).isEqualTo("Incompatible with Brain MRI.");
                    assertThat(ex.getConstraints()).singleElement().satisfies(c -> {
                        assertThat(c.type()).isEqualTo(ConstraintType.FERROMAGNETIC_IMPLANT);
                        assertThat(c.absoluteContraindication()).isTrue();
                    });
                    assertThat(ex.canRetry()).isFalse();
                });
    }

    @Test
    void invalidDataCarriesTheRejectedFields() {
        server.expect(requestTo(BASE + "/api/v1/orders"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"INVALID_DATA\",\"message\":\"Missing.\",\"invalidFields\":[\"priority\",\"patientId\"]}"));

        assertThatThrownBy(() -> client.createOrder(PAYLOAD, "dr-1", "PHYSICIAN"))
                .isInstanceOfSatisfying(OrderClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(OrderClientException.Kind.INVALID_DATA);
                    assertThat(ex.getInvalidFields()).containsExactly("priority", "patientId");
                    assertThat(ex.getConstraints()).isEmpty();
                });
    }

    @Test
    void forbiddenAndNotFoundUseDefaultMessagesWhenTheBodyIsNotJson() {
        server.expect(requestTo(BASE + "/api/v1/orders")).andRespond(withStatus(HttpStatus.FORBIDDEN).body("nope"));
        assertThatThrownBy(() -> client.createOrder(PAYLOAD, null, null))
                .isInstanceOfSatisfying(OrderClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(OrderClientException.Kind.FORBIDDEN);
                    assertThat(ex.getMessage()).contains("physicians");
                });

        server.reset();
        server.expect(requestTo(BASE + "/api/v1/orders")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> client.createOrder(PAYLOAD, "dr-1", "PHYSICIAN"))
                .isInstanceOfSatisfying(OrderClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(OrderClientException.Kind.EXAM_NOT_FOUND);
                    assertThat(ex.getMessage()).contains("no longer active");
                });
    }

    @Test
    void serverErrorsAndAMalformedAlertCanBeRetriedOrIgnored() {
        server.expect(requestTo(BASE + "/api/v1/orders")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThatThrownBy(() -> client.createOrder(PAYLOAD, "dr-1", "PHYSICIAN"))
                .isInstanceOfSatisfying(OrderClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(OrderClientException.Kind.FAILED);
                    assertThat(ex.canRetry()).isTrue();
                });

        server.reset();
        server.expect(requestTo(BASE + "/api/v1/orders"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"constraints\":[{\"type\":\"NOT_A_TYPE\"}]}"));
        assertThatThrownBy(() -> client.createOrder(PAYLOAD, "dr-1", "PHYSICIAN"))
                .isInstanceOfSatisfying(OrderClientException.class, ex -> {
                    assertThat(ex.isSafetyAlert()).isTrue();
                    assertThat(ex.getConstraints()).isEmpty();
                    assertThat(ex.getMessage()).contains("safety constraints");
                });

        server.reset();
        server.expect(requestTo(BASE + "/api/v1/orders"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Alert\"}"));
        assertThatThrownBy(() -> client.createOrder(PAYLOAD, "dr-1", "PHYSICIAN"))
                .isInstanceOfSatisfying(OrderClientException.class, ex -> assertThat(ex.getConstraints()).isEmpty());
    }

    @Test
    void unreachableServerSaysTheOrderWasNotCreated() {
        server.expect(requestTo(BASE + "/api/v1/orders"))
                .andRespond(withException(new java.net.ConnectException("refused")));

        assertThatThrownBy(() -> client.createOrder(PAYLOAD, "dr-1", "PHYSICIAN"))
                .isInstanceOfSatisfying(OrderClientException.class, ex -> {
                    assertThat(ex.getKind()).isEqualTo(OrderClientException.Kind.SERVER_UNREACHABLE);
                    assertThat(ex.getMessage()).contains("has not been created");
                    assertThat(ex.canRetry()).isTrue();
                    assertThat(ex).hasCauseInstanceOf(ResourceAccessException.class);
                });
    }
}
