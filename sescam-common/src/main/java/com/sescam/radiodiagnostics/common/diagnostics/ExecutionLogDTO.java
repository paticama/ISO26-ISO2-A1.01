package com.sescam.radiodiagnostics.common.diagnostics;

import java.time.Instant;
import java.util.List;

/**
 * Execution log submitted by the client (step 3 of UC-PHY-05).
 *
 * @param orderId                 booked procedure; mandatory
 * @param startedAt               actual start; mandatory
 * @param endedAt                 actual end (or abort time in 2b); mandatory, not before {@code startedAt}
 * @param roomEquipmentId         room/equipment used; mandatory
 * @param contrastBatch           contrast medium batch (lot); mandatory when {@code contrastVolumeMl > 0}
 * @param contrastVolumeMl        contrast administered, in ml; {@code null} if none
 * @param doseDlp                 dose-length product, mGy·cm; mandatory for CT unless aborted
 * @param doseCtdiVol             CTDIvol, mGy; mandatory for CT unless aborted
 * @param radiotracerActivityMBq  administered activity, MBq; mandatory for nuclear medicine unless aborted
 * @param informedConsentVerified consent checked before the examination; must be true when consent is required
 * @param aborted                 2b: the incident prevented completing the examination
 * @param abortReason             2b: mandatory when {@code aborted}
 * @param incidents               2a: clinical or operational incidents; at least one when {@code aborted};
 *                                {@code null} is read as none
 */
public record ExecutionLogDTO(
        String orderId,
        Instant startedAt,
        Instant endedAt,
        String roomEquipmentId,
        String contrastBatch,
        Double contrastVolumeMl,
        Double doseDlp,
        Double doseCtdiVol,
        Double radiotracerActivityMBq,
        boolean informedConsentVerified,
        boolean aborted,
        String abortReason,
        List<IncidentDTO> incidents,
        String actorId,
        String actorRole
) {
    public ExecutionLogDTO {
        incidents = incidents == null ? List.of() : List.copyOf(incidents);
    }
}
