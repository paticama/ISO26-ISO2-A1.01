package com.sescam.radiodiagnostics.common.diagnostics;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port of C7 to read booked procedures (an order of C4 with its slot in C5). Precondition of UC-PHY-05:
 * the procedure was ordered (UC-PHY-01) and booked (UC-AAD-05).
 *
 * <p>To be implemented by C5 (sescam-admissions) when bookings exist; until then C7 wires a stand-in.
 */
public interface ScheduledProcedureGateway {

    /** Booked procedures (any execution state), oldest scheduled first. */
    List<ScheduledProcedureDTO> listBookedProcedures();

    /** The booked procedure of an order, if it is booked. */
    Optional<ScheduledProcedureDTO> findByOrderId(String orderId);
}
