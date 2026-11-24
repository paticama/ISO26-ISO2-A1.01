package com.sescam.radiodiagnostics.orders.internal;

import com.sescam.radiodiagnostics.common.orders.OrderCreatedEvent;
import com.sescam.radiodiagnostics.common.orders.OrderEventPublisher;

/**
 * Stand-in for the subscriber in C5 (sescam-admissions): drops the event. Replace it when admissions exists.
 */
public class NoOpOrderEventPublisher implements OrderEventPublisher {

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        // intentionally empty
    }
}
