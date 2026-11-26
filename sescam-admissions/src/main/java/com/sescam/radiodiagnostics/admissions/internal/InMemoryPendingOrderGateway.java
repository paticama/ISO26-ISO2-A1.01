package com.sescam.radiodiagnostics.admissions.internal;

import com.sescam.radiodiagnostics.common.admissions.PendingExamRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.PendingOrderGateway;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Stand-in pending orders gateway until C4 (sescam-orders) exposes pending-order queries. */
public class InMemoryPendingOrderGateway implements PendingOrderGateway {

    private final Map<String, PendingExamRequestDTO> orders = new ConcurrentHashMap<>();
    private final Set<String> bookedOrderIds = ConcurrentHashMap.newKeySet();

    public InMemoryPendingOrderGateway() {
        seed(new PendingExamRequestDTO("ORD-1001", "PHY-2026-1001", "PAT-001", "CT-CHEST",
                "Chest CT with Contrast", "CT", PriorityLevel.STAT_URGENT, Instant.parse("2026-11-25T09:00:00Z")));
        seed(new PendingExamRequestDTO("ORD-1002", "PHY-2026-1002", "PAT-002", "MRI-BRAIN",
                "Brain MRI", "MRI", PriorityLevel.PREFERENTIAL, Instant.parse("2026-11-25T09:30:00Z")));
    }

    public void seed(PendingExamRequestDTO order) {
        orders.put(order.orderId(), order);
    }

    @Override
    public List<PendingExamRequestDTO> listPendingOrders() {
        return orders.values().stream()
                .filter(o -> !bookedOrderIds.contains(o.orderId()))
                .toList();
    }

    @Override
    public Optional<PendingExamRequestDTO> findByOrderId(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    @Override
    public void markOrderBooked(String orderId) {
        bookedOrderIds.add(orderId);
    }

    @Override
    public void markOrderPending(String orderId) {
        bookedOrderIds.remove(orderId);
    }
}
