package com.sescam.radiodiagnostics.orders.internal;

import com.sescam.radiodiagnostics.common.catalogue.ExamTypeDTO;
import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.orders.SafetyConstraintDTO;

import java.util.List;

/**
 * Compatibility of a patient's active safety constraints with an examination (the rule behind
 * "Verify Patient Safety Constraints and Contrast Allergies" of UC-PHY-01).
 *
 * <ul>
 *   <li>contrast allergy and renal impairment: incompatible with any examination that uses contrast medium;</li>
 *   <li>pregnancy: incompatible with examinations that use ionizing radiation (X-ray, CT, nuclear medicine);</li>
 *   <li>ferromagnetic implant: incompatible with MRI.</li>
 * </ul>
 * These are the rules of the specification examples (NFR-05, NFR-11); the clinical stakeholders must confirm them.
 */
public class SafetyRules {

    private static final List<ModalityType> IONIZING = List.of(
            ModalityType.X_RAY, ModalityType.CT, ModalityType.NUCLEAR_MEDICINE);

    /** Returns the constraints that are incompatible with the examination, in the order given; empty if none. */
    public List<SafetyConstraintDTO> incompatibleConstraints(ExamTypeDTO exam, List<SafetyConstraintDTO> constraints) {
        if (constraints == null) {
            return List.of();
        }
        return constraints.stream().filter(c -> isIncompatible(exam, c)).toList();
    }

    private static boolean isIncompatible(ExamTypeDTO exam, SafetyConstraintDTO constraint) {
        return switch (constraint.type()) {
            case CONTRAST_ALLERGY, RENAL_IMPAIRMENT -> exam.requiresContrast();
            case PREGNANCY -> IONIZING.contains(exam.modality());
            case FERROMAGNETIC_IMPLANT -> exam.modality() == ModalityType.MRI;
        };
    }
}
