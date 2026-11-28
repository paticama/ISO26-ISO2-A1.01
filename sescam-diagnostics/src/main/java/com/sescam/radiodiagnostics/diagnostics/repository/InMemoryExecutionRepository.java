package com.sescam.radiodiagnostics.diagnostics.repository;

import com.sescam.radiodiagnostics.common.diagnostics.ExecutionAlreadyRecordedException;
import com.sescam.radiodiagnostics.common.diagnostics.ExecutionStatus;
import com.sescam.radiodiagnostics.diagnostics.domain.ProcedureExecution;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory store keyed by order id: one execution record per procedure. */
public class InMemoryExecutionRepository implements ExecutionRepository {

    private final Map<String, ProcedureExecution> byOrderId = new ConcurrentHashMap<>();

    /** Atomic: of two requests for the same order at the same time, only the first is stored. */
    @Override
    public ProcedureExecution save(ProcedureExecution execution) {
        ProcedureExecution existing = byOrderId.putIfAbsent(execution.getOrderId(), execution);
        if (existing != null) {
            throw new ExecutionAlreadyRecordedException(
                    "The execution of order '" + execution.getOrderId() + "' is already recorded.");
        }
        return execution;
    }

    @Override
    public Optional<ProcedureExecution> findById(String executionId) {
        if (executionId == null) {
            return Optional.empty();
        }
        return byOrderId.values().stream()
                .filter(e -> e.getId().equals(executionId))
                .findFirst();
    }

    @Override
    public Optional<ProcedureExecution> findByOrderId(String orderId) {
        return orderId == null ? Optional.empty() : Optional.ofNullable(byOrderId.get(orderId));
    }

    @Override
    public List<ProcedureExecution> findAllExecutedPendingReport() {
        return byOrderId.values().stream()
                .filter(e -> e.getStatus() == ExecutionStatus.EXECUTED_PENDING_REPORT)
                .sorted(Comparator.comparing(ProcedureExecution::getRecordedAt)
                        .thenComparing(ProcedureExecution::getId))
                .toList();
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
