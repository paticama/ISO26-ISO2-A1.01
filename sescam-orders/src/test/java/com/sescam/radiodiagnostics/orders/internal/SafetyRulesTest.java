package com.sescam.radiodiagnostics.orders.internal;

import com.sescam.radiodiagnostics.common.catalogue.ExamTypeDTO;
import com.sescam.radiodiagnostics.common.catalogue.ModalityType;
import com.sescam.radiodiagnostics.common.orders.ConstraintType;
import com.sescam.radiodiagnostics.common.orders.SafetyConstraintDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SafetyRulesTest {

    private final SafetyRules rules = new SafetyRules();

    private static ExamTypeDTO exam(ModalityType modality, boolean contrast) {
        return new ExamTypeDTO("E", "Exam", modality, 10, null, false, contrast, false, null, true);
    }

    private static SafetyConstraintDTO constraint(ConstraintType type) {
        return new SafetyConstraintDTO("c-" + type, type, false, type.name());
    }

    @Test
    void contrastAllergyAndRenalImpairmentBlockContrastExaminations() {
        List<SafetyConstraintDTO> both = List.of(constraint(ConstraintType.CONTRAST_ALLERGY),
                constraint(ConstraintType.RENAL_IMPAIRMENT));

        assertThat(rules.incompatibleConstraints(exam(ModalityType.CT, true), both)).hasSize(2);
        assertThat(rules.incompatibleConstraints(exam(ModalityType.CT, false), both)).isEmpty();
    }

    @Test
    void pregnancyBlocksIonizingRadiationOnly() {
        List<SafetyConstraintDTO> pregnancy = List.of(constraint(ConstraintType.PREGNANCY));

        for (ModalityType m : List.of(ModalityType.X_RAY, ModalityType.CT, ModalityType.NUCLEAR_MEDICINE)) {
            assertThat(rules.incompatibleConstraints(exam(m, false), pregnancy)).hasSize(1);
        }
        assertThat(rules.incompatibleConstraints(exam(ModalityType.MRI, false), pregnancy)).isEmpty();
        assertThat(rules.incompatibleConstraints(exam(ModalityType.ULTRASOUND, false), pregnancy)).isEmpty();
    }

    @Test
    void ferromagneticImplantBlocksMriOnly() {
        List<SafetyConstraintDTO> implant = List.of(constraint(ConstraintType.FERROMAGNETIC_IMPLANT));

        assertThat(rules.incompatibleConstraints(exam(ModalityType.MRI, false), implant)).hasSize(1);
        assertThat(rules.incompatibleConstraints(exam(ModalityType.CT, false), implant)).isEmpty();
    }

    @Test
    void keepsOnlyTheIncompatibleOnesAndToleratesNull() {
        SafetyConstraintDTO implant = constraint(ConstraintType.FERROMAGNETIC_IMPLANT);
        SafetyConstraintDTO allergy = constraint(ConstraintType.CONTRAST_ALLERGY);

        assertThat(rules.incompatibleConstraints(exam(ModalityType.MRI, false), List.of(allergy, implant)))
                .containsExactly(implant);
        assertThat(rules.incompatibleConstraints(exam(ModalityType.MRI, false), null)).isEmpty();
    }
}
