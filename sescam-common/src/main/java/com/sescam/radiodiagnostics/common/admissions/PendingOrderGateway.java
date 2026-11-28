package com.sescam.radiodiagnostics.common.admissions;

import java.util.List;
import java.util.Optional;

/** Outbound port to C4 (sescam-orders) for pending diagnostic orders awaiting scheduling. */
public interface PendingOrderGateway {

    List<PendingExamRequestDTO> listPendingOrders();

    Optional<PendingExamRequestDTO> findByOrderId(String orderId);

    void markOrderBooked(String orderId);

    void markOrderPending(String orderId);
}
