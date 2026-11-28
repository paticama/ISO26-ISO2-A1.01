package com.sescam.radiodiagnostics.orders.repository;

import com.sescam.radiodiagnostics.orders.domain.DiagnosticOrder;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, DiagnosticOrder> storage = new ConcurrentHashMap<>();

    @Override
    public DiagnosticOrder save(DiagnosticOrder order) {
        storage.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<DiagnosticOrder> findById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(storage.get(id));
    }

    @Override
    public long count() {
        return storage.size();
    }
}
