package com.sescam.radiodiagnostics.common.orders;

/**
 * Outbound port of C4 (sescam-orders) for the clinical hand-off order -> booking (ADR-014).
 *
 * <p>Orders publishes the event instead of calling admissions. C5 (sescam-admissions) subscribes to it to feed its
 * worklist; until it exists, the server wires a no-op implementation.
 */
public interface OrderEventPublisher {

    /**
     * Announces that a new order is waiting for slot allocation (postcondition 2 of UC-PHY-01).
     */
    void publishOrderCreated(OrderCreatedEvent event);
}
