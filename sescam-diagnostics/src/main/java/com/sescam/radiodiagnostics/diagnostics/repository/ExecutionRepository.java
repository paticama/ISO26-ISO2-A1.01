package com.sescam.radiodiagnostics.diagnostics.repository;

import com.sescam.radiodiagnostics.diagnostics.domain.ProcedureExecution;

import java.util.Optional;

public interface ExecutionRepository {

    ProcedureExecution save(ProcedureExecution execution);

    Optional<ProcedureExecution> findByOrderId(String orderId);

    boolean existsByOrderId(String orderId);

    long count();
}
