package com.sescam.radiodiagnostics.admissions.internal;

import com.sescam.radiodiagnostics.common.admissions.BookingChangedEvent;
import com.sescam.radiodiagnostics.common.admissions.BookingEventPublisher;

/** Stand-in event publisher until external subscribers are wired. */
public class NoOpBookingEventPublisher implements BookingEventPublisher {

    @Override
    public void publish(BookingChangedEvent event) {
        // No-op stand-in.
    }
}
