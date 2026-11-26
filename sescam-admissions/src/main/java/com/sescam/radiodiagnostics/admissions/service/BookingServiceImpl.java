package com.sescam.radiodiagnostics.admissions.service;

import com.sescam.radiodiagnostics.admissions.domain.Booking;
import com.sescam.radiodiagnostics.admissions.domain.WorklistItem;
import com.sescam.radiodiagnostics.admissions.repository.BookingRepository;
import com.sescam.radiodiagnostics.admissions.repository.WorklistRepository;
import com.sescam.radiodiagnostics.common.admissions.BookingChangedEvent;
import com.sescam.radiodiagnostics.common.admissions.BookingConflictException;
import com.sescam.radiodiagnostics.common.admissions.BookingDTO;
import com.sescam.radiodiagnostics.common.admissions.BookingEventPublisher;
import com.sescam.radiodiagnostics.common.admissions.BookingNotFoundException;
import com.sescam.radiodiagnostics.common.admissions.BookingNotificationGateway;
import com.sescam.radiodiagnostics.common.admissions.BookingPolicyGateway;
import com.sescam.radiodiagnostics.common.admissions.BookingService;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;
import com.sescam.radiodiagnostics.common.admissions.CancelBookingRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.ConcurrentBookingException;
import com.sescam.radiodiagnostics.common.admissions.CreateBookingRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.InvalidBookingDataException;
import com.sescam.radiodiagnostics.common.admissions.NotificationStatus;
import com.sescam.radiodiagnostics.common.admissions.PatientEligibilityGateway;
import com.sescam.radiodiagnostics.common.admissions.PatientNotEligibleException;
import com.sescam.radiodiagnostics.common.admissions.PendingExamRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.PendingOrderGateway;
import com.sescam.radiodiagnostics.common.admissions.RequestAlreadyProcessedException;
import com.sescam.radiodiagnostics.common.admissions.RescheduleBookingRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.SlotUnavailableBookingException;
import com.sescam.radiodiagnostics.common.admissions.UnauthorizedBookingAccessException;
import com.sescam.radiodiagnostics.common.admissions.WorklistItemDTO;
import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import com.sescam.radiodiagnostics.common.schedules.ConcurrentScheduleChangeException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleNotFoundException;
import com.sescam.radiodiagnostics.common.schedules.ScheduleService;
import com.sescam.radiodiagnostics.common.schedules.SlotDTO;
import com.sescam.radiodiagnostics.common.schedules.SlotUnavailableException;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Realization of UC-AAD-05: Book Requested Examination (C5 v1.0.0). */
public class BookingServiceImpl implements BookingService {

    private static final int DEFAULT_SLOT_LOOKAHEAD_DAYS = 14;

    private final BookingRepository bookingRepository;
    private final WorklistRepository worklistRepository;
    private final ScheduleService scheduleService;
    private final PendingOrderGateway pendingOrderGateway;
    private final PatientEligibilityGateway patientEligibilityGateway;
    private final BookingPolicyGateway bookingPolicyGateway;
    private final BookingNotificationGateway notificationGateway;
    private final AccessControlService accessControlService;
    private final AuditService auditService;
    private final BookingEventPublisher eventPublisher;
    private final Clock clock;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              WorklistRepository worklistRepository,
                              ScheduleService scheduleService,
                              PendingOrderGateway pendingOrderGateway,
                              PatientEligibilityGateway patientEligibilityGateway,
                              BookingPolicyGateway bookingPolicyGateway,
                              BookingNotificationGateway notificationGateway,
                              AccessControlService accessControlService,
                              AuditService auditService,
                              BookingEventPublisher eventPublisher) {
        this(bookingRepository, worklistRepository, scheduleService, pendingOrderGateway,
                patientEligibilityGateway, bookingPolicyGateway, notificationGateway,
                accessControlService, auditService, eventPublisher, Clock.systemUTC());
    }

    public BookingServiceImpl(BookingRepository bookingRepository,
                              WorklistRepository worklistRepository,
                              ScheduleService scheduleService,
                              PendingOrderGateway pendingOrderGateway,
                              PatientEligibilityGateway patientEligibilityGateway,
                              BookingPolicyGateway bookingPolicyGateway,
                              BookingNotificationGateway notificationGateway,
                              AccessControlService accessControlService,
                              AuditService auditService,
                              BookingEventPublisher eventPublisher,
                              Clock clock) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
        this.worklistRepository = Objects.requireNonNull(worklistRepository);
        this.scheduleService = Objects.requireNonNull(scheduleService);
        this.pendingOrderGateway = Objects.requireNonNull(pendingOrderGateway);
        this.patientEligibilityGateway = Objects.requireNonNull(patientEligibilityGateway);
        this.bookingPolicyGateway = Objects.requireNonNull(bookingPolicyGateway);
        this.notificationGateway = Objects.requireNonNull(notificationGateway);
        this.accessControlService = Objects.requireNonNull(accessControlService);
        this.auditService = Objects.requireNonNull(auditService);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public List<PendingExamRequestDTO> listPendingRequests(String actorId, String actorRole) {
        ensureAuthorized(actorId, actorRole, null);
        return pendingOrderGateway.listPendingOrders().stream()
                .filter(o -> !bookingRepository.existsActiveByOrderId(o.orderId()))
                .sorted(Comparator.comparingInt((PendingExamRequestDTO o) -> priorityRank(o.priority()))
                        .thenComparing(PendingExamRequestDTO::orderedAt))
                .toList();
    }

    @Override
    public List<SlotDTO> findCompatibleSlots(String orderId, LocalDateTime from, LocalDateTime to,
                                             String actorId, String actorRole) {
        ensureAuthorized(actorId, actorRole, orderId);
        if (isBlank(orderId)) {
            throw new InvalidBookingDataException("orderId is mandatory.", List.of("orderId"));
        }
        PendingExamRequestDTO order = pendingOrderGateway.findByOrderId(orderId.trim())
                .orElseThrow(() -> new BookingNotFoundException("Order '" + orderId + "' was not found."));
        LocalDateTime windowStart = from != null ? from : LocalDateTime.now(clock);
        LocalDateTime windowEnd = to != null ? to : windowStart.plusDays(DEFAULT_SLOT_LOOKAHEAD_DAYS);
        return scheduleService.findCompatibleSlots(order.modality(), windowStart, windowEnd);
    }

    @Override
    public BookingDTO createBooking(CreateBookingRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        ensureAuthorized(request.actorId(), request.actorRole(), request.orderId());
        validateCreate(request);

        String orderId = request.orderId().trim();
        PendingExamRequestDTO order = pendingOrderGateway.findByOrderId(orderId).orElse(null);
        if (order == null) {
            auditRefusal("BOOKING_REJECTED", request.actorId(), orderId, "Order not found");
            throw new BookingNotFoundException("Order '" + orderId + "' was not found.");
        }
        if (bookingRepository.existsActiveByOrderId(orderId)) {
            auditRefusal("BOOKING_REJECTED", request.actorId(), orderId, "Order already booked");
            throw new RequestAlreadyProcessedException("Order '" + orderId + "' already has an active booking.");
        }
        if (!patientEligibilityGateway.isPatientEligible(order.patientId())) {
            auditRefusal("BOOKING_REJECTED", request.actorId(), orderId, "Patient not eligible: " + order.patientId());
            throw new PatientNotEligibleException("Patient '" + order.patientId() + "' is not eligible for booking.");
        }

        String bookingId = "BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        SlotDTO reservedSlot = reserveSlot(bookingId, order.patientId(), order.modality(),
                request.slotId().trim(), request.expectedSlotVersion(), request.actorId());

        int maxPerDay = bookingPolicyGateway.maxBookingsPerPatientPerDay();
        long sameDayCount = bookingRepository
                .findActiveByPatientAndDate(order.patientId(), reservedSlot.start().toLocalDate()).size();
        if (sameDayCount >= maxPerDay) {
            scheduleService.releaseSlot(reservedSlot.slotId(), bookingId);
            auditRefusal("BOOKING_REJECTED", request.actorId(), orderId, "Daily patient booking limit exceeded");
            throw new BookingConflictException("Patient '" + order.patientId() + "' exceeds daily booking limit.");
        }

        Instant now = clock.instant();
        Booking booking = new Booking(bookingId, orderId, order.patientId(), order.modality(),
                reservedSlot, request.actorId(), now);

        audit("BOOKING_CREATED", request.actorId(), bookingId,
                "Order " + orderId + " booked into slot " + reservedSlot.slotId());
        persistBookingWithRollback(booking, reservedSlot.slotId(), request.actorId());

        pendingOrderGateway.markOrderBooked(orderId);
        worklistRepository.save(toWorklistItem(booking, reservedSlot.resourceId(), order.modality()));
        notifyAndUpdateStatus(booking, "Appointment booked for " + reservedSlot.start());
        eventPublisher.publish(new BookingChangedEvent(bookingId, orderId, order.patientId(),
                reservedSlot.slotId(), booking.getStatus(), reservedSlot.start(), now));
        return booking.toDTO();
    }

    @Override
    public BookingDTO getBooking(String bookingId, String actorId, String actorRole) {
        ensureAuthorized(actorId, actorRole, bookingId);
        return findBookingOrThrow(bookingId).toDTO();
    }

    @Override
    public BookingDTO rescheduleBooking(RescheduleBookingRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        ensureAuthorized(request.actorId(), request.actorRole(), request.bookingId());

        List<String> invalid = new ArrayList<>();
        if (isBlank(request.bookingId())) invalid.add("bookingId");
        if (isBlank(request.newSlotId())) invalid.add("newSlotId");
        if (isBlank(request.reason())) invalid.add("reason");
        if (!invalid.isEmpty()) {
            throw new InvalidBookingDataException("Missing reschedule data: " + String.join(", ", invalid), invalid);
        }

        Booking booking = findBookingOrThrow(request.bookingId().trim());
        if (booking.getVersion() != request.expectedBookingVersion()) {
            auditRefusal("BOOKING_CONCURRENT_CONFLICT", request.actorId(), booking.getBookingId(), "Stale booking version");
            throw new ConcurrentBookingException("Booking '" + booking.getBookingId() + "' was modified concurrently.");
        }
        String oldSlotId = booking.getSlotId();
        String modality = booking.toDTO().modality();
        SlotDTO newSlot = reserveSlot(booking.getBookingId(), booking.getPatientId(), modality,
                request.newSlotId().trim(), request.expectedSlotVersion(), request.actorId());

        Instant now = clock.instant();
        booking.reschedule(newSlot, request.reason().trim(), request.actorId(), now);
        audit("BOOKING_RESCHEDULED", request.actorId(), booking.getBookingId(),
                "Moved from slot " + oldSlotId + " to " + newSlot.slotId());
        persistBookingWithRollback(booking, newSlot.slotId(), request.actorId());

        scheduleService.releaseSlot(oldSlotId, booking.getBookingId());
        worklistRepository.save(toWorklistItem(booking, newSlot.resourceId(), modality));
        notifyAndUpdateStatus(booking, "Appointment rescheduled to " + newSlot.start());
        eventPublisher.publish(new BookingChangedEvent(booking.getBookingId(), booking.getOrderId(),
                booking.getPatientId(), newSlot.slotId(), booking.getStatus(), newSlot.start(), now));
        return booking.toDTO();
    }

    @Override
    public BookingDTO cancelBooking(CancelBookingRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        ensureAuthorized(request.actorId(), request.actorRole(), request.bookingId());

        List<String> invalid = new ArrayList<>();
        if (isBlank(request.bookingId())) invalid.add("bookingId");
        if (isBlank(request.reason())) invalid.add("reason");
        if (!invalid.isEmpty()) {
            throw new InvalidBookingDataException("Missing cancellation data: " + String.join(", ", invalid), invalid);
        }

        Booking booking = findBookingOrThrow(request.bookingId().trim());
        if (booking.getVersion() != request.expectedBookingVersion()) {
            auditRefusal("BOOKING_CONCURRENT_CONFLICT", request.actorId(), booking.getBookingId(), "Stale booking version");
            throw new ConcurrentBookingException("Booking '" + booking.getBookingId() + "' was modified concurrently.");
        }

        int minHours = bookingPolicyGateway.minHoursBeforeCancellation();
        LocalDateTime nowLocal = LocalDateTime.now(clock);
        if (minHours > 0 && ChronoUnit.HOURS.between(nowLocal, booking.getScheduledStart()) < minHours) {
            auditRefusal("BOOKING_REJECTED", request.actorId(), booking.getBookingId(), "Cancellation policy window violated");
            throw new BookingConflictException("Booking cannot be cancelled within " + minHours + " hours of start.");
        }

        String releasedSlotId = booking.getSlotId();
        Instant now = clock.instant();
        booking.cancel(request.reason().trim(), request.actorId(), now);
        audit("BOOKING_CANCELLED", request.actorId(), booking.getBookingId(), "Reason: " + request.reason().trim());
        bookingRepository.save(booking);

        scheduleService.releaseSlot(releasedSlotId, booking.getBookingId());
        pendingOrderGateway.markOrderPending(booking.getOrderId());
        BookingDTO dto = booking.toDTO();
        worklistRepository.save(toWorklistItem(booking, dto.resourceId(), dto.modality()));
        notifyAndUpdateStatus(booking, "Appointment cancelled: " + request.reason().trim());
        eventPublisher.publish(new BookingChangedEvent(booking.getBookingId(), booking.getOrderId(),
                booking.getPatientId(), releasedSlotId, booking.getStatus(), booking.getScheduledStart(), now));
        return booking.toDTO();
    }

    @Override
    public List<WorklistItemDTO> listDailyWorklist(LocalDate date, String actorId, String actorRole) {
        ensureAuthorized(actorId, actorRole, null);
        LocalDate target = date != null ? date : LocalDate.now(clock);
        return worklistRepository.findByDate(target).stream().map(WorklistItem::toDTO).toList();
    }

    private SlotDTO reserveSlot(String bookingId, String patientId, String modality,
                                String slotId, long expectedVersion, String actorId) {
        try {
            return scheduleService.reserveSlotAtomically(slotId, expectedVersion, bookingId);
        } catch (SlotUnavailableException ex) {
            LocalDateTime from = LocalDateTime.now(clock);
            List<SlotDTO> alternatives = scheduleService.findCompatibleSlots(
                    modality, from, from.plusDays(DEFAULT_SLOT_LOOKAHEAD_DAYS));
            auditRefusal("BOOKING_SLOT_UNAVAILABLE", actorId, bookingId, "Slot " + slotId + " unavailable");
            throw new SlotUnavailableBookingException(ex.getMessage(), alternatives);
        } catch (ConcurrentScheduleChangeException ex) {
            auditRefusal("BOOKING_CONCURRENT_CONFLICT", actorId, bookingId, "Stale slot " + slotId);
            throw new ConcurrentBookingException(ex.getMessage());
        } catch (ScheduleNotFoundException ex) {
            auditRefusal("BOOKING_REJECTED", actorId, bookingId, "Unknown slot " + slotId + " for patient " + patientId);
            throw new BookingNotFoundException(ex.getMessage());
        }
    }

    private void persistBookingWithRollback(Booking booking, String reservedSlotId, String actorId) {
        try {
            bookingRepository.save(booking);
        } catch (RuntimeException ex) {
            scheduleService.releaseSlot(reservedSlotId, booking.getBookingId());
            auditRefusal("BOOKING_CREATION_FAILED", actorId, booking.getBookingId(),
                    "Booking not stored: " + ex.getClass().getSimpleName());
            throw ex;
        }
    }

    private void notifyAndUpdateStatus(Booking booking, String message) {
        try {
            NotificationStatus status = notificationGateway.sendBookingNotification(
                    booking.getBookingId(), booking.getPatientId(), booking.getStatus(), message);
            booking.updateNotificationStatus(status != null ? status : NotificationStatus.PENDING_RETRY);
        } catch (RuntimeException ex) {
            // A4: booking remains persisted and auditable; notification is queued for retry.
            booking.updateNotificationStatus(NotificationStatus.PENDING_RETRY);
            auditRefusal("BOOKING_NOTIFICATION_FAILED", "SYSTEM", booking.getBookingId(), ex.getMessage());
        }
        bookingRepository.save(booking);
    }

    private Booking findBookingOrThrow(String bookingId) {
        if (isBlank(bookingId)) {
            throw new InvalidBookingDataException("bookingId is mandatory.", List.of("bookingId"));
        }
        return bookingRepository.findById(bookingId.trim())
                .orElseThrow(() -> new BookingNotFoundException("Booking '" + bookingId + "' was not found."));
    }

    private void ensureAuthorized(String actorId, String actorRole, String targetId) {
        if (isBlank(actorId) || !accessControlService.canManageBookings(actorId, actorRole)) {
            auditRefusal("BOOKING_ACCESS_DENIED", actorId, targetId, "Role: " + actorRole);
            throw new UnauthorizedBookingAccessException(
                    "Role '" + actorRole + "' is not authorized to manage examination bookings.");
        }
    }

    private void validateCreate(CreateBookingRequestDTO request) {
        List<String> invalid = new ArrayList<>();
        if (isBlank(request.orderId())) invalid.add("orderId");
        if (isBlank(request.slotId())) invalid.add("slotId");
        if (!invalid.isEmpty()) {
            auditRefusal("BOOKING_REJECTED", request.actorId(), request.orderId(), "Missing fields: " + invalid);
            throw new InvalidBookingDataException("Mandatory booking data is missing: " + String.join(", ", invalid), invalid);
        }
    }

    private static WorklistItem toWorklistItem(Booking booking, String resourceId, String modality) {
        BookingDTO dto = booking.toDTO();
        return new WorklistItem(
                "WL-" + booking.getBookingId(),
                booking.getBookingId(),
                booking.getOrderId(),
                booking.getPatientId(),
                resourceId,
                modality,
                dto.scheduledStart(),
                dto.scheduledEnd(),
                booking.getStatus()
        );
    }

    private static int priorityRank(PriorityLevel priority) {
        if (priority == null) return 3;
        return switch (priority) {
            case STAT_URGENT -> 0;
            case PREFERENTIAL -> 1;
            case ROUTINE -> 2;
        };
    }

    private void audit(String action, String actorId, String targetId, String details) {
        auditService.recordEvent(new AuditEventDTO(action, actorId, targetId, null, null, details, clock.instant()));
    }

    private void auditRefusal(String action, String actorId, String targetId, String details) {
        try {
            audit(action, actorId, targetId, details);
        } catch (RuntimeException ignored) {
            // Best-effort refusal audit so the domain error still reaches the caller.
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
