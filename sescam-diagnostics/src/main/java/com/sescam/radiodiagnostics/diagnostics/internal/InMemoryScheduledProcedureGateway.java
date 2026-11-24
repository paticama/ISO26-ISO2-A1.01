package com.sescam.radiodiagnostics.diagnostics.internal;

import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureDTO;
import com.sescam.radiodiagnostics.common.diagnostics.ScheduledProcedureGateway;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TEMPORARY stand-in for the bookings of C5 (sescam-admissions, UC-AAD-05), seeded with a few booked procedures so
 * UC-PHY-05 can run end to end. Remove it when C5 implements {@link ScheduledProcedureGateway}.
 */
public class InMemoryScheduledProcedureGateway implements ScheduledProcedureGateway {

    private final Map<String, ScheduledProcedureDTO> booked = new LinkedHashMap<>();

    public InMemoryScheduledProcedureGateway() {
        this(List.of(
                new ScheduledProcedureDTO("ORD-DEMO-XR", "PAT-DEMO-1", "XR-CHEST", "Chest X-ray", ModalityType.X_RAY,
                        Instant.parse("2026-11-24T08:00:00Z"), "XR-ROOM-1", false, false),
                new ScheduledProcedureDTO("ORD-DEMO-CT", "PAT-DEMO-2", "CT-ABD-C", "Abdominal CT with contrast",
                        ModalityType.CT, Instant.parse("2026-11-24T09:00:00Z"), "CT-ROOM-1", true, true),
                new ScheduledProcedureDTO("ORD-DEMO-MR", "PAT-DEMO-3", "MR-KNEE-C", "Knee MRI with contrast",
                        ModalityType.MRI, Instant.parse("2026-11-24T10:00:00Z"), "MR-ROOM-1", true, true),
                new ScheduledProcedureDTO("ORD-DEMO-NM", "PAT-DEMO-4", "NM-BONE", "Bone scintigraphy",
                        ModalityType.NUCLEAR_MEDICINE, Instant.parse("2026-11-24T11:00:00Z"), "NM-ROOM-1", false, true)));
    }

    public InMemoryScheduledProcedureGateway(List<ScheduledProcedureDTO> procedures) {
        procedures.forEach(p -> booked.put(p.orderId(), p));
    }

    @Override
    public List<ScheduledProcedureDTO> listBookedProcedures() {
        return booked.values().stream()
                .sorted(Comparator.comparing(ScheduledProcedureDTO::scheduledAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    public Optional<ScheduledProcedureDTO> findByOrderId(String orderId) {
        return orderId == null ? Optional.empty() : Optional.ofNullable(booked.get(orderId));
    }
}
