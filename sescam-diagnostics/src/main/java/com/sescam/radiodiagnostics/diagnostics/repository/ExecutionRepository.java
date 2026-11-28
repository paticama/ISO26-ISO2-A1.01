package com.sescam.radiodiagnostics.diagnostics.repository;

import com.sescam.radiodiagnostics.diagnostics.domain.ProcedureExecution;

import java.util.List;
import java.util.Optional;

public interface ExecutionRepository {

    /**
     * Stores a new execution record.
     *
     * @throws com.sescam.radiodiagnostics.common.diagnostics.ExecutionAlreadyRecordedException if the order already
     *         has one, also when two requests arrive at the same time
     */
    ProcedureExecution save(ProcedureExecution execution);

    Optional<ProcedureExecution> findById(String executionId);

    Optional<ProcedureExecution> findByOrderId(String orderId);

    List<ProcedureExecution> findAllExecutedPendingReport();

    boolean existsByOrderId(String orderId);

    long count();
}
