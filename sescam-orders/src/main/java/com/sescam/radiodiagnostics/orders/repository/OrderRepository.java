package com.sescam.radiodiagnostics.orders.repository;

import com.sescam.radiodiagnostics.orders.domain.DiagnosticOrder;

import java.util.Optional;

public interface OrderRepository {

    DiagnosticOrder save(DiagnosticOrder order);

    Optional<DiagnosticOrder> findById(String id);

    long count();
}
