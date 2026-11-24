package com.sescam.radiodiagnostics.common.orders;

/**
 * Informed-consent document and patient preparation attached to an order (A6a). Built from the examination type of the
 * catalogue; nothing is generated beyond the references stored there.
 *
 * @param informedConsentRequired {@code true} if the patient must sign a consent before the examination
 * @param consentTemplateId       id of the consent template in the catalogue; {@code null} if none is required
 * @param preparationGuide        patient preparation text; may be {@code null}
 * @param fastingRequired         {@code true} if the patient must fast
 * @param contrastRequired        {@code true} if contrast medium is used
 */
public record ConsentAndPreparationDTO(
        boolean informedConsentRequired,
        String consentTemplateId,
        String preparationGuide,
        boolean fastingRequired,
        boolean contrastRequired
) {
}
