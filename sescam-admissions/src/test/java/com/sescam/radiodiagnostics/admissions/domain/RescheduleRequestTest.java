package com.sescam.radiodiagnostics.admissions.domain;

import com.sescam.radiodiagnostics.common.admissions.RescheduleRequestStatus;
import com.sescam.radiodiagnostics.common.admissions.RescheduleRequestStatusDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RescheduleRequestTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");

    @Test
    void aNewRequestIsPendingReviewAndKeepsWhatThePatientAsked() {
        List<LocalDate> dates = List.of(LocalDate.of(2026, 12, 10));

        RescheduleRequest request = RescheduleRequest.pending("BKG-1", "PAT-001", "Work trip", dates, NOW);

        assertTrue(request.isPending());
        assertTrue(request.getRequestId().startsWith("RRQ-"));
        RescheduleRequestStatusDTO dto = request.toStatusDTO();
        assertEquals("BKG-1", dto.bookingId());
        assertEquals(RescheduleRequestStatus.PENDING_REVIEW, dto.status());
        assertEquals("Work trip", dto.reason());
        assertEquals(dates, dto.preferredDates());
        assertEquals(NOW, dto.requestedAt());
        assertNull(dto.reviewedAt());
    }
}
