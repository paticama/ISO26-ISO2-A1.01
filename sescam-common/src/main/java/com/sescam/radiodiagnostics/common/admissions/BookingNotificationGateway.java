package com.sescam.radiodiagnostics.common.admissions;

/** Outbound port to C8 (sescam-communication) for patient booking notifications (A4). */
public interface BookingNotificationGateway {

    NotificationStatus sendBookingNotification(String bookingId, String patientId,
                                               BookingStatus status, String message);
}
