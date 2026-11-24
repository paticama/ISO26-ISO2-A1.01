package com.sescam.radiodiagnostics.diagnostics.repository;

import com.sescam.radiodiagnostics.diagnostics.domain.ProcedureExecution;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory store keyed by order id: one execution record per procedure. */
public class InMemoryExecutionRepository implements ExecutionRepository {

    private final Map<String, ProcedureExecution> byOrderId = new ConcurrentHashMap<>();

    @Override
    public ProcedureExecution save(ProcedureExecution execution) {
        byOrderId.put(execution.getOrderId(), execution);
        return execution;
    }

    @Override
    public Optional<ProcedureExecution> findByOrderId(String orderId) {
        return orderId == null ? Optional.empty() : Optional.ofNullable(byOrderId.get(orderId));
    }

    @Override
    public boolean existsByOrderId(String orderId) {
        return orderId != null && byOrderId.containsKey(orderId);
    }

    @Override
    public long count() {
        return byOrderId.size();
    }
}
