package com.sescam.radiodiagnostics.server.orders;

import com.sescam.radiodiagnostics.common.catalogue.ExamTypeDTO;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeService;
import com.sescam.radiodiagnostics.common.orders.CreateOrderDTO;
import com.sescam.radiodiagnostics.common.orders.OrderResponseDTO;
import com.sescam.radiodiagnostics.common.orders.OrderService;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for UC-PHY-01: Order and Prescribe Diagnostic Procedure.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderRestController {

    private final OrderService orderService;
    private final ExamTypeService examTypeService;

    public OrderRestController(OrderService orderService, ExamTypeService examTypeService) {
        this.orderService = orderService;
        this.examTypeService = examTypeService;
    }

    /** Step 2: active examination types of the catalogue. */
    @GetMapping("/exam-types")
    public ResponseEntity<List<ExamTypeDTO>> listExamTypes() {
        return ResponseEntity.ok(examTypeService.listActiveExamTypes());
    }

    /**
     * Steps 5-8 and alternatives 6a/6b. To override a safety alert (A6b) the client sends the same payload again with
     * {@code overrideJustification}.
     */
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
            @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole,
            @RequestBody CreateOrderPayload payload) {
        CreateOrderDTO request = new CreateOrderDTO(payload.patientId(), payload.examTypeCode(),
                payload.clinicalIndication(), payload.clinicalJustification(), payload.priority(),
                payload.overrideJustification(), payload.acknowledgedConstraintIds(), actorId, actorRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    /** Request body: the actor comes from the headers, never from the body. */
    public record CreateOrderPayload(
            String patientId,
            String examTypeCode,
            String clinicalIndication,
            String clinicalJustification,
            PriorityLevel priority,
            String overrideJustification,
            List<String> acknowledgedConstraintIds
    ) {
        public CreateOrderPayload(String patientId, String examTypeCode, String clinicalIndication,
                                  String clinicalJustification, PriorityLevel priority,
                                  String overrideJustification) {
            this(patientId, examTypeCode, clinicalIndication, clinicalJustification, priority,
                    overrideJustification, List.of());
        }
    }
}
