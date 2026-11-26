package com.sescam.radiodiagnostics.admissions.service;

import com.sescam.radiodiagnostics.admissions.domain.Booking;
import com.sescam.radiodiagnostics.admissions.domain.RescheduleRequest;
import com.sescam.radiodiagnostics.admissions.domain.WorklistItem;
import com.sescam.radiodiagnostics.admissions.repository.BookingRepository;
import com.sescam.radiodiagnostics.admissions.repository.RescheduleRequestRepository;
import com.sescam.radiodiagnostics.admissions.repository.WorklistRepository;
import com.sescam.radiodiagnostics.common.admissions.AppointmentDetailsDTO;
import com.sescam.radiodiagnostics.common.admissions.AppointmentSummaryDTO;
import com.sescam.radiodiagnostics.common.admissions.BookingChangedEvent;
import com.sescam.radiodiagnostics.common.admissions.BookingConflictException;
import com.sescam.radiodiagnostics.common.admissions.BookingErrorCode;
import com.sescam.radiodiagnostics.common.admissions.BookingEventPublisher;
import com.sescam.radiodiagnostics.common.admissions.BookingException;
import com.sescam.radiodiagnostics.common.admissions.BookingNotFoundException;
import com.sescam.radiodiagnostics.common.admissions.BookingNotificationGateway;
import com.sescam.radiodiagnostics.common.admissions.BookingPolicyGateway;
import com.sescam.radiodiagnostics.common.admissions.BookingStatus;
import com.sescam.radiodiagnostics.common.admissions.CancelAppointmentRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.ConcurrentBookingException;
import com.sescam.radiodiagnostics.common.admissions.ConfirmAppointmentRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.DuplicatePendingRequestException;
import com.sescam.radiodiagnostics.common.admissions.InvalidBookingDataException;
import com.sescam.radiodiagnostics.common.admissions.NotificationStatus;
import com.sescam.radiodiagnostics.common.admissions.PatientAppointmentService;
import com.sescam.radiodiagnostics.common.admissions.PatientRescheduleRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.PendingExamRequestDTO;
import com.sescam.radiodiagnostics.common.admissions.PendingOrderGateway;
import com.sescam.radiodiagnostics.common.admissions.RequestAlreadyProcessedException;
import com.sescam.radiodiagnostics.common.admissions.RescheduleRequestStatusDTO;
import com.sescam.radiodiagnostics.common.admissions.UnauthorizedBookingAccessException;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeDTO;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeNotFoundException;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeService;
import com.sescam.radiodiagnostics.common.orders.ConsentAndPreparationDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleService;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Realization of UC-PAT-01: Manage my appointments (C5 v1.1.0).
 *
 * <p>It works on the same bookings as UC-AAD-05 ({@link BookingServiceImpl}) but from the patient's side:
 * the patient only sees their own appointments (A8), can confirm or cancel them within the booking policy
 * (A5, A7), and can ask Admissions to move them without changing them (A2, A6).</p>
 *
 * <p>Audit (NFR-11): every view and every change is audited. A change is audited <em>before</em> it is applied,
 * so if the audit cannot be recorded nothing changes. Refusals are audited on a best-effort basis, as in
 * UC-AAD-05, so the patient still receives the real error.</p>
 */
public class PatientAppointmentServiceImpl implements PatientAppointmentService {

    private final BookingRepository bookingRepository;
    private final RescheduleRequestRepository rescheduleRequestRepository;
    private final WorklistRepository worklistRepository;
    private final ScheduleService scheduleService;
    private final PendingOrderGateway pendingOrderGateway;
    private final ExamTypeService examTypeService;
    private final AppointmentRules rules;
    private final BookingNotificationGateway notificationGateway;
    private final AccessControlService accessControlService;
    private final AuditService auditService;
    private final BookingEventPublisher eventPublisher;
    private final Clock clock;

    public PatientAppointmentServiceImpl(BookingRepository bookingRepository,
                                         RescheduleRequestRepository rescheduleRequestRepository,
                                         WorklistRepository worklistRepository,
                                         ScheduleService scheduleService,
                                         PendingOrderGateway pendingOrderGateway,
                                         ExamTypeService examTypeService,
                                         BookingPolicyGateway bookingPolicyGateway,
                                         BookingNotificationGateway notificationGateway,
                                         AccessControlService accessControlService,
                                         AuditService auditService,
                                         BookingEventPublisher eventPublisher) {
        this(bookingRepository, rescheduleRequestRepository, worklistRepository, scheduleService,
                pendingOrderGateway, examTypeService, bookingPolicyGateway, notificationGateway,
                accessControlService, auditService, eventPublisher, Clock.systemUTC());
    }

    public PatientAppointmentServiceImpl(BookingRepository bookingRepository,
                                         RescheduleRequestRepository rescheduleRequestRepository,
                                         WorklistRepository worklistRepository,
                                         ScheduleService scheduleService,
                                         PendingOrderGateway pendingOrderGateway,
                                         ExamTypeService examTypeService,
                                         BookingPolicyGateway bookingPolicyGateway,
                                         BookingNotificationGateway notificationGateway,
                                         AccessControlService accessControlService,
                                         AuditService auditService,
                                         BookingEventPublisher eventPublisher,
                                         Clock clock) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
        this.rescheduleRequestRepository = Objects.requireNonNull(rescheduleRequestRepository);
        this.worklistRepository = Objects.requireNonNull(worklistRepository);
        this.scheduleService = Objects.requireNonNull(scheduleService);
        this.pendingOrderGateway = Objects.requireNonNull(pendingOrderGateway);
        this.examTypeService = Objects.requireNonNull(examTypeService);
        this.rules = new AppointmentRules(Objects.requireNonNull(bookingPolicyGateway));
        this.notificationGateway = Objects.requireNonNull(notificationGateway);
        this.accessControlService = Objects.requireNonNull(accessControlService);
        this.auditService = Objects.requireNonNull(auditService);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.clock = Objects.requireNonNull(clock);
    }

    // ------------------------------------------------------------------ steps 1-3 (+A1)

    @Override
    public List<AppointmentSummaryDTO> listMyAppointments(String actorId, String actorRole) {
        ensurePatient(actorId, actorRole, null);
        String patientId = actorId.trim();

        // From the start of today, so an appointment later today (or already done today) is still listed.
        LocalDateTime startOfToday = LocalDate.now(clock).atStartOfDay();
        List<AppointmentSummaryDTO> appointments = bookingRepository.findUpcomingByPatientId(patientId, startOfToday)
                .stream()
                .sorted(Comparator.comparing(Booking::getScheduledStart))
                .map(booking -> toSummary(booking, findOrder(booking)))
                .toList();

        // Clinical data: nothing is returned unless the access is audited. An empty list is A1.
        audit("APPOINTMENTS_VIEWED", patientId, patientId, null, null,
                appointments.size() + " upcoming appointment(s) returned");
        return appointments;
    }

    // ------------------------------------------------------------------ steps 4-6 (+A7, A8)

    @Override
    public AppointmentDetailsDTO getAppointmentDetails(String bookingId, String actorId, String actorRole) {
        Booking booking = loadOwnedBooking(bookingId, actorId, actorRole);
        AppointmentDetailsDTO details = toDetails(booking);
        audit("APPOINTMENT_DETAILS_VIEWED", actorId, booking.getBookingId(), null, null,
                "Status " + booking.getStatus());
        return details;
    }

    // ------------------------------------------------------------------ steps 7-9 (+A5, A7)

    @Override
    public AppointmentDetailsDTO confirmAppointment(ConfirmAppointmentRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        Booking booking = loadOwnedBooking(request.bookingId(), request.actorId(), request.actorRole());
        rejectIfBlocked(rules.checkConfirm(booking, request.expectedBookingVersion(), now()),
                booking, request.actorId(), "confirm");

        audit("APPOINTMENT_CONFIRMED", request.actorId(), booking.getBookingId(),
                booking.getStatus().name(), BookingStatus.CONFIRMED.name(), "Attendance confirmed by the patient");
        Instant now = clock.instant();
        booking.confirm(request.actorId(), now);
        bookingRepository.save(booking);
        publishChange(booking, now);
        return toDetails(booking);
    }

    // ------------------------------------------------------------------ A2 (+A6, A7)

    @Override
    public RescheduleRequestStatusDTO requestRescheduling(PatientRescheduleRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        Booking booking = loadOwnedBooking(request.bookingId(), request.actorId(), request.actorRole());

        List<String> invalid = rules.invalidRescheduleFields(request, LocalDate.now(clock));
        if (!invalid.isEmpty()) {
            throw new InvalidBookingDataException(
                    "Invalid rescheduling request: " + String.join(", ", invalid), invalid);
        }
        // The state of the appointment (A7) is checked first: it tells the patient more than a pending request.
        rejectIfBlocked(rules.checkReschedule(booking, now()), booking, request.actorId(), "request rescheduling");
        if (rescheduleRequestRepository.findPendingByBookingId(booking.getBookingId()).isPresent()) {
            auditRefusal("APPOINTMENT_ACTION_REJECTED", request.actorId(), booking.getBookingId(),
                    "request rescheduling: " + BookingErrorCode.DUPLICATE_PENDING_REQUEST);
            throw new DuplicatePendingRequestException("Appointment '" + booking.getBookingId()
                    + "' already has a rescheduling request waiting for Admissions.");
        }

        // The booking itself is not touched: Admissions decide on the request in UC-AAD-05.
        RescheduleRequest rescheduleRequest = RescheduleRequest.pending(booking.getBookingId(),
                booking.getPatientId(), request.reason().trim(), request.preferredDates(), clock.instant());
        audit("APPOINTMENT_RESCHEDULE_REQUESTED", request.actorId(), booking.getBookingId(), null,
                rescheduleRequest.getRequestId(), "Reason: " + request.reason().trim()
                        + "; preferred dates: " + request.preferredDates());
        rescheduleRequestRepository.save(rescheduleRequest);
        return rescheduleRequest.toStatusDTO();
    }

    // ------------------------------------------------------------------ A3, A4 (+A5, A7)

    @Override
    public AppointmentDetailsDTO cancelAppointment(CancelAppointmentRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");
        Booking booking = loadOwnedBooking(request.bookingId(), request.actorId(), request.actorRole());
        rejectIfBlocked(rules.checkCancel(booking, request.expectedBookingVersion(), now()),
                booking, request.actorId(), "cancel");

        String reason = isBlank(request.reason())
                ? "Cancelled by the patient"
                : "Cancelled by the patient: " + request.reason().trim();
        // A4: the priority and whether the patient saw the warning make the cancellation of an
        // urgent or preferential examination traceable for the ordering physician.
        String priority = findOrder(booking).map(order -> String.valueOf(order.priority())).orElse("unknown");
        audit("APPOINTMENT_CANCELLED", request.actorId(), booking.getBookingId(),
                booking.getStatus().name(), BookingStatus.CANCELLED.name(),
                reason + "; priority: " + priority
                        + "; priority warning acknowledged: " + request.priorityWarningAcknowledged());

        // Same steps as a staff cancellation (BookingServiceImpl.cancelBooking, UC-AAD-05 A3), so Admissions see
        // the free slot, the order goes back to pending and the worklist no longer counts the appointment.
        String releasedSlotId = booking.getSlotId();
        Instant now = clock.instant();
        booking.cancel(reason, request.actorId(), now);
        bookingRepository.save(booking);
        scheduleService.releaseSlot(releasedSlotId, booking.getBookingId());
        pendingOrderGateway.markOrderPending(booking.getOrderId());
        worklistRepository.save(WorklistItem.forBooking(booking));
        notifyPatient(booking, "Your appointment of " + booking.getScheduledStart() + " has been cancelled.");
        publishChange(booking, now);
        return toDetails(booking);
    }

    // ------------------------------------------------------------------ access checks (A8)

    /** Role check, then existence, then ownership (A8). Used by every operation on one appointment. */
    private Booking loadOwnedBooking(String bookingId, String actorId, String actorRole) {
        ensurePatient(actorId, actorRole, bookingId);
        if (isBlank(bookingId)) {
            throw new InvalidBookingDataException("bookingId is mandatory.", List.of("bookingId"));
        }
        Booking booking = bookingRepository.findById(bookingId.trim())
                .orElseThrow(() -> new BookingNotFoundException("Appointment '" + bookingId + "' was not found."));
        if (!booking.isOwnedBy(actorId)) {
            auditRefusal("APPOINTMENT_ACCESS_DENIED", actorId, booking.getBookingId(),
                    "The appointment belongs to another patient");
            throw new UnauthorizedBookingAccessException("You can only access your own appointments.");
        }
        return booking;
    }

    private void ensurePatient(String actorId, String actorRole, String targetId) {
        if (!accessControlService.canManageOwnAppointments(actorId, actorRole)) {
            auditRefusal("APPOINTMENT_ACCESS_DENIED", actorId, targetId, "Role: " + actorRole);
            throw new UnauthorizedBookingAccessException(
                    "Role '" + actorRole + "' is not allowed to manage patient appointments.");
        }
    }

    /** Audits the refusal and throws the exception that matches the code returned by {@link AppointmentRules}. */
    private void rejectIfBlocked(Optional<BookingErrorCode> blocking, Booking booking, String actorId, String action) {
        if (blocking.isEmpty()) {
            return;
        }
        BookingErrorCode code = blocking.get();
        auditRefusal("APPOINTMENT_ACTION_REJECTED", actorId, booking.getBookingId(), action + ": " + code);
        String id = booking.getBookingId();
        BookingException error = switch (code) {
            case REQUEST_ALREADY_PROCESSED -> new RequestAlreadyProcessedException(
                    "Appointment '" + id + "' is " + booking.getStatus() + "; it cannot be changed (" + action + ").");
            case CONCURRENT_BOOKING -> new ConcurrentBookingException(
                    "Appointment '" + id + "' was changed meanwhile. Reload it and try again.");
            case BOOKING_CONFLICT -> new BookingConflictException(
                    "The deadline to " + action + " appointment '" + id + "' has passed. " + rules.serviceContact());
            default -> throw new IllegalStateException("Unexpected rule result " + code);
        };
        throw error;
    }

    // ------------------------------------------------------------------ mapping

    private AppointmentSummaryDTO toSummary(Booking booking, Optional<PendingExamRequestDTO> order) {
        return new AppointmentSummaryDTO(booking.getBookingId(), booking.getScheduledStart(),
                booking.getScheduledEnd(), order.map(PendingExamRequestDTO::examTypeName).orElse(null),
                booking.getModality(), booking.getLocationId(), booking.getResourceId(), booking.getStatus());
    }

    private AppointmentDetailsDTO toDetails(Booking booking) {
        Optional<PendingExamRequestDTO> order = findOrder(booking);
        Optional<RescheduleRequest> pending = rescheduleRequestRepository.findPendingByBookingId(booking.getBookingId());
        return new AppointmentDetailsDTO(
                toSummary(booking, order),
                order.map(PendingExamRequestDTO::examTypeCode).orElse(null),
                order.map(PendingExamRequestDTO::priority).orElse(null),
                order.map(o -> findPreparation(o.examTypeCode())).orElse(null),
                rules.allowedActions(booking, pending.isPresent(), now()),
                pending.map(RescheduleRequest::toStatusDTO).orElse(null),
                rules.serviceContact(),
                booking.getVersion());
    }

    /** The examination type and priority are not stored in the booking: they come from the order (C4). */
    private Optional<PendingExamRequestDTO> findOrder(Booking booking) {
        return pendingOrderGateway.findByOrderId(booking.getOrderId());
    }

    /**
     * Preparation instructions of the examination type (C2). If the catalogue does not know the type, the
     * details are still shown without instructions instead of failing the whole page.
     */
    private ConsentAndPreparationDTO findPreparation(String examTypeCode) {
        if (isBlank(examTypeCode)) {
            return null;
        }
        try {
            ExamTypeDTO type = examTypeService.getActiveExamType(examTypeCode);
            return new ConsentAndPreparationDTO(type.requiresInformedConsent(), type.consentTemplateId(),
                    type.preparationGuide(), type.requiresFasting(), type.requiresContrast());
        } catch (ExamTypeNotFoundException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------ side effects

    /** As in UC-AAD-05 A4: a failed notification never undoes the change; it stays pending for retry. */
    private void notifyPatient(Booking booking, String message) {
        try {
            NotificationStatus status = notificationGateway.sendBookingNotification(
                    booking.getBookingId(), booking.getPatientId(), booking.getStatus(), message);
            booking.updateNotificationStatus(status != null ? status : NotificationStatus.PENDING_RETRY);
        } catch (RuntimeException ex) {
            booking.updateNotificationStatus(NotificationStatus.PENDING_RETRY);
            auditRefusal("BOOKING_NOTIFICATION_FAILED", "SYSTEM", booking.getBookingId(), ex.getMessage());
        }
        bookingRepository.save(booking);
    }

    /** Lets C8 (UC-PAT-03) notify and C4 (UC-PHY-03) update the order status. */
    private void publishChange(Booking booking, Instant now) {
        eventPublisher.publish(new BookingChangedEvent(booking.getBookingId(), booking.getOrderId(),
                booking.getPatientId(), booking.getSlotId(), booking.getStatus(), booking.getScheduledStart(), now));
    }

    private void audit(String action, String actorId, String targetId,
                       String previousValue, String newValue, String details) {
        auditService.recordEvent(new AuditEventDTO(action, actorId, targetId, previousValue, newValue, details,
                clock.instant()));
    }

    private void auditRefusal(String action, String actorId, String targetId, String details) {
        try {
            audit(action, actorId, targetId, null, null, details);
        } catch (RuntimeException ignored) {
            // Best-effort refusal audit so the real error still reaches the patient.
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
