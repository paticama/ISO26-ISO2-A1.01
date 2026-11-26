package com.sescam.radiodiagnostics.common.admissions;

import com.sescam.radiodiagnostics.common.schedules.SlotDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Public contract of C5 (sescam-admissions), UC-AAD-05: Book Requested Examination.
 */
public interface BookingService {

    /** Step 1: pending examination requests ordered by clinical priority and request time. */
    List<PendingExamRequestDTO> listPendingRequests(String actorId, String actorRole);

    /** Step 2: compatible bookable slots in C3 for the order's modality within {@code [from, to)}. */
    List<SlotDTO> findCompatibleSlots(String orderId, LocalDateTime from, LocalDateTime to,
                                      String actorId, String actorRole);

    /** Steps 4-8: atomically reserves a slot, creates a booking, marks the order Booked, updates worklist, and notifies. */
    BookingDTO createBooking(CreateBookingRequestDTO request);

    /** Retrieves one booking with its full transition history. */
    BookingDTO getBooking(String bookingId, String actorId, String actorRole);

    /** A2: reschedules a booking to a new slot while preserving history and updating the worklist. */
    BookingDTO rescheduleBooking(RescheduleBookingRequestDTO request);

    /** A3: cancels a booking with an explicit reason, releasing capacity according to policy. */
    BookingDTO cancelBooking(CancelBookingRequestDTO request);

    /** Operational daily worklist items for the given date. */
    List<WorklistItemDTO> listDailyWorklist(LocalDate date, String actorId, String actorRole);
}
