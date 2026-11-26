package com.sescam.radiodiagnostics.admissions.internal;

import com.sescam.radiodiagnostics.common.admissions.BookingNotificationGateway;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;
import com.sescam.radiodiagnostics.common.admissions.NotificationStatus;

/** Stand-in notification gateway until C8 (sescam-communication) is implemented. */
public class NoOpBookingNotificationGateway implements BookingNotificationGateway {

    @Override
    public NotificationStatus sendBookingNotification(String bookingId, String patientId,
                                                      BookingStatus status, String message) {
        return NotificationStatus.SENT;
    }
}
