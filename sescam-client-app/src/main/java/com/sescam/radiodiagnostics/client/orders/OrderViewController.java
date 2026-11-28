package com.sescam.radiodiagnostics.client.orders;

import com.sescam.radiodiagnostics.common.orders.OrderResponseDTO;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

/**
 * View of UC-PHY-01 in the client app: a physician orders a diagnostic procedure.
 *
 * <p>Flow: {@code GET /orders/new} shows the catalogue (steps 1-3) → {@code POST /orders} validates the form on the
 * client (4a) and sends it (steps 5-7) → the page shows the order number (step 8), or the safety alert (6b) with the
 * constraints and a field for the override justification. The physician either resubmits with the justification or
 * cancels, which discards the form. The actor comes from the {@code X-Actor-Id}/{@code X-Actor-Role} headers until
 * the client has login, as in the other views.
 */
@Controller
@RequestMapping("/orders")
public class OrderViewController {

    static final String VIEW = "orders/new";

    private final OrderApiClient apiClient;

    public OrderViewController(OrderApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /** Steps 1-3: open the form with the catalogue of active examination types. */
    @GetMapping("/new")
    public String newOrder(Model model) {
        prepare(model, new OrderForm());
        return VIEW;
    }

    /** Steps 4-8, 4a and 6b. */
    @PostMapping
    public String submit(@ModelAttribute("form") OrderForm form, Model model,
                         @RequestHeader(value = "X-Actor-Id", defaultValue = "") String actorId,
                         @RequestHeader(value = "X-Actor-Role", defaultValue = "") String actorRole) {
        prepare(model, form);
        Map<String, String> errors = form.validate();
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            return VIEW;
        }
        try {
            OrderResponseDTO order = apiClient.createOrder(form.toPayload(), actorId, actorRole);
            model.addAttribute("order", order);
        } catch (OrderClientException ex) {
            model.addAttribute("failure", ex);
            if (ex.getKind() == OrderClientException.Kind.INVALID_DATA) {
                model.addAttribute("errors", serverFieldErrors(ex.getInvalidFields()));
            }
        }
        return VIEW;
    }

    /** Cancels after a safety alert: nothing was stored, so the physician just gets an empty form. */
    @PostMapping("/cancel")
    public String cancel(Model model) {
        prepare(model, new OrderForm());
        model.addAttribute("cancelled", true);
        return VIEW;
    }

    private void prepare(Model model, OrderForm form) {
        model.addAttribute("form", form);
        model.addAttribute("priorities", PriorityLevel.values());
        try {
            model.addAttribute("examTypes", apiClient.listExamTypes());
        } catch (OrderClientException ex) {
            model.addAttribute("examTypes", List.of());
            model.addAttribute("catalogueFailure", ex.getMessage());
        }
    }

    private static Map<String, String> serverFieldErrors(List<String> fields) {
        Map<String, String> errors = new java.util.LinkedHashMap<>();
        fields.forEach(f -> errors.put(f, "This field is mandatory or not valid."));
        return errors;
    }
}
