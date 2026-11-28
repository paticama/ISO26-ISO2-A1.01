package com.sescam.radiodiagnostics.server.admissions;

import com.sescam.radiodiagnostics.admissions.internal.DefaultBookingPolicyGateway;
import com.sescam.radiodiagnostics.admissions.internal.InMemoryPendingOrderGateway;
import com.sescam.radiodiagnostics.admissions.internal.NoOpBookingEventPublisher;
import com.sescam.radiodiagnostics.admissions.internal.NoOpBookingNotificationGateway;
import com.sescam.radiodiagnostics.admissions.repository.BookingRepository;
import com.sescam.radiodiagnostics.admissions.repository.InMemoryBookingRepository;
import com.sescam.radiodiagnostics.admissions.repository.InMemoryRescheduleRequestRepository;
import com.sescam.radiodiagnostics.admissions.repository.InMemoryWorklistRepository;
import com.sescam.radiodiagnostics.admissions.repository.RescheduleRequestRepository;
import com.sescam.radiodiagnostics.admissions.repository.WorklistRepository;
import com.sescam.radiodiagnostics.admissions.service.BookingServiceImpl;
import com.sescam.radiodiagnostics.admissions.service.PatientAppointmentServiceImpl;
import com.sescam.radiodiagnostics.common.admissions.BookingEventPublisher;
import com.sescam.radiodiagnostics.common.admissions.BookingNotificationGateway;
import com.sescam.radiodiagnostics.common.admissions.BookingPolicyGateway;
import com.sescam.radiodiagnostics.common.admissions.BookingService;
import com.sescam.radiodiagnostics.common.admissions.PatientAppointmentService;
import com.sescam.radiodiagnostics.common.admissions.PatientEligibilityGateway;
import com.sescam.radiodiagnostics.common.admissions.PendingOrderGateway;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeService;
import com.sescam.radiodiagnostics.common.patients.PatientNotFoundException;
import com.sescam.radiodiagnostics.common.patients.PatientService;
import com.sescam.radiodiagnostics.common.patients.PatientStatus;
import com.sescam.radiodiagnostics.common.schedules.ScheduleService;
import com.sescam.radiodiagnostics.common.security.AccessControlService;
import com.sescam.radiodiagnostics.common.security.AuditService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration wiring module C5 (sescam-admissions) into the server application.
 */
@Configuration
public class BookingWiringConfig {

    @Bean
    public BookingRepository bookingRepository() {
        return new InMemoryBookingRepository();
    }

    @Bean
    public WorklistRepository worklistRepository() {
        return new InMemoryWorklistRepository();
    }

    @Bean
    public PendingOrderGateway pendingOrderGateway() {
        return new InMemoryPendingOrderGateway();
    }

    @Bean
    public PatientEligibilityGateway patientEligibilityGateway(PatientService patientService) {
        return patientId -> {
            if (patientId == null || patientId.isBlank()) {
                return false;
            }
            try {
                return patientService.getPatientById(patientId, "admissions-system", "ADMISSIONS_STAFF")
                        .status() == PatientStatus.ACTIVE;
            } catch (PatientNotFoundException ex) {
                return false;
            }
        };
    }

    @Bean
    public BookingPolicyGateway bookingPolicyGateway() {
        return new DefaultBookingPolicyGateway();
    }

    @Bean
    public BookingNotificationGateway bookingNotificationGateway() {
        return new NoOpBookingNotificationGateway();
    }

    @Bean
    public BookingEventPublisher bookingEventPublisher() {
        return new NoOpBookingEventPublisher();
    }

    @Bean
    public BookingService bookingService(BookingRepository bookingRepository,
                                         WorklistRepository worklistRepository,
                                         ScheduleService scheduleService,
                                         PendingOrderGateway pendingOrderGateway,
                                         PatientEligibilityGateway patientEligibilityGateway,
                                         BookingPolicyGateway bookingPolicyGateway,
                                         BookingNotificationGateway bookingNotificationGateway,
                                         AccessControlService accessControlService,
                                         AuditService auditService,
                                         BookingEventPublisher bookingEventPublisher) {
        return new BookingServiceImpl(bookingRepository, worklistRepository, scheduleService,
                pendingOrderGateway, patientEligibilityGateway, bookingPolicyGateway,
                bookingNotificationGateway, accessControlService, auditService, bookingEventPublisher);
    }

    @Bean
    public RescheduleRequestRepository rescheduleRequestRepository() {
        return new InMemoryRescheduleRequestRepository();
    }

    /**
     * UC-PAT-01: the patient's side of the same bookings, so it shares the repositories, gateways and policy
     * of UC-AAD-05. The examination catalogue (C2) is the ExamTypeService bean of the server.
     */
    @Bean
    public PatientAppointmentService patientAppointmentService(BookingRepository bookingRepository,
                                                               RescheduleRequestRepository rescheduleRequestRepository,
                                                               WorklistRepository worklistRepository,
                                                               ScheduleService scheduleService,
                                                               PendingOrderGateway pendingOrderGateway,
                                                               ExamTypeService examTypeService,
                                                               BookingPolicyGateway bookingPolicyGateway,
                                                               BookingNotificationGateway bookingNotificationGateway,
                                                               AccessControlService accessControlService,
                                                               AuditService auditService,
                                                               BookingEventPublisher bookingEventPublisher) {
        return new PatientAppointmentServiceImpl(bookingRepository, rescheduleRequestRepository, worklistRepository,
                scheduleService, pendingOrderGateway, examTypeService, bookingPolicyGateway,
                bookingNotificationGateway, accessControlService, auditService, bookingEventPublisher);
    }
}
