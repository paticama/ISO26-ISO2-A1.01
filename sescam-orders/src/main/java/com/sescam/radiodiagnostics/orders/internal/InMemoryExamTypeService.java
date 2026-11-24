package com.sescam.radiodiagnostics.orders.internal;

import com.sescam.radiodiagnostics.common.catalogue.ExamTypeDTO;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeNotFoundException;
import com.sescam.radiodiagnostics.common.catalogue.ExamTypeService;
import com.sescam.radiodiagnostics.common.catalogue.ModalityType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TEMPORARY stand-in for the examination-type catalogue of C2 (sescam-catalogue), seeded with a few examinations so
 * UC-PHY-01 can run end to end. Remove it when C2 implements {@link ExamTypeService}.
 */
public class InMemoryExamTypeService implements ExamTypeService {

    private final Map<String, ExamTypeDTO> catalogue = new LinkedHashMap<>();

    public InMemoryExamTypeService() {
        add(new ExamTypeDTO("XR-CHEST", "Chest X-ray", ModalityType.X_RAY, 10,
                "Remove metallic objects from the chest area.", false, false, false, null, true));
        add(new ExamTypeDTO("CT-ABD-C", "Abdominal CT with contrast", ModalityType.CT, 30,
                "Fast for 6 hours. Drink water before the examination.", true, true, true, "CONSENT-CONTRAST", true));
        add(new ExamTypeDTO("MR-BRAIN", "Brain MRI", ModalityType.MRI, 45,
                "Remove all metallic objects and electronic devices.", false, false, false, null, true));
        add(new ExamTypeDTO("MR-KNEE-C", "Knee MRI with contrast", ModalityType.MRI, 40,
                "Remove all metallic objects.", false, true, true, "CONSENT-CONTRAST", true));
        add(new ExamTypeDTO("US-ABD", "Abdominal ultrasound", ModalityType.ULTRASOUND, 20,
                "Fast for 6 hours.", true, false, false, null, true));
        add(new ExamTypeDTO("XR-OLD", "Retired examination", ModalityType.X_RAY, 10, null, false, false, false, null, false));
    }

    private void add(ExamTypeDTO exam) {
        catalogue.put(exam.code(), exam);
    }

    @Override
    public List<ExamTypeDTO> listActiveExamTypes() {
        return catalogue.values().stream().filter(ExamTypeDTO::active).toList();
    }

    @Override
    public ExamTypeDTO getActiveExamType(String code) {
        ExamTypeDTO exam = code == null ? null : catalogue.get(code.trim().toUpperCase(Locale.ROOT));
        if (exam == null || !exam.active()) {
            throw new ExamTypeNotFoundException("Examination type '" + code + "' does not exist or is not active.");
        }
        return exam;
    }
}
