package com.sescam.radiodiagnostics.common.catalogue;

import java.util.List;

/**
 * Read-only contract of C2 (sescam-catalogue) for the catalogue of examination types. Used by C4 (orders) in
 * UC-PHY-01 and later by admissions and schedules.
 *
 * <p>Kept apart from {@link CatalogueService} (configuration parameters, UC-ADM-01) so that adding it does not change
 * that contract. Proposed by UC-PHY-01; needs the approval of the C2 module owner (ADR-013).
 */
public interface ExamTypeService {

    /**
     * Lists the active examination types (step 2 of UC-PHY-01).
     */
    List<ExamTypeDTO> listActiveExamTypes();

    /**
     * Returns an active examination type.
     *
     * @throws ExamTypeNotFoundException if the code is unknown or the type is inactive
     */
    ExamTypeDTO getActiveExamType(String code);
}
