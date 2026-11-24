package com.sescam.radiodiagnostics.common.catalogue;

/**
 * Read view of an examination type of the catalogue (entity ExamType, owned by C2).
 *
 * @param code                    unique procedure code
 * @param estimatedDurationMinutes duration used by admissions to size the slot
 * @param consentTemplateId       informed-consent template; {@code null} if none is needed
 */
public record ExamTypeDTO(
        String code,
        String name,
        ModalityType modality,
        int estimatedDurationMinutes,
        String preparationGuide,
        boolean requiresFasting,
        boolean requiresContrast,
        boolean requiresInformedConsent,
        String consentTemplateId,
        boolean active
) {
}
