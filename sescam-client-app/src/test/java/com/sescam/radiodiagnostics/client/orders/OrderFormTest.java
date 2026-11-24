package com.sescam.radiodiagnostics.client.orders;

import com.sescam.radiodiagnostics.common.orders.PriorityLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderFormTest {

    private static OrderForm valid() {
        OrderForm form = new OrderForm();
        form.setPatientId(" PAT-1 ");
        form.setExamTypeCode(" XR-CHEST ");
        form.setClinicalIndication(" cough ");
        form.setClinicalJustification(" rule out pneumonia ");
        form.setPriority(PriorityLevel.ROUTINE);
        return form;
    }

    @Test
    void validFormHasNoErrorsAndBuildsATrimmedPayload() {
        OrderForm form = valid();
        form.setOverrideJustification("  ");

        assertThat(form.validate()).isEmpty();
        OrderApiClient.OrderPayload payload = form.toPayload();
        assertThat(payload.patientId()).isEqualTo("PAT-1");
        assertThat(payload.examTypeCode()).isEqualTo("XR-CHEST");
        assertThat(payload.clinicalIndication()).isEqualTo("cough");
        assertThat(payload.clinicalJustification()).isEqualTo("rule out pneumonia");
        assertThat(payload.priority()).isEqualTo("ROUTINE");
        assertThat(payload.overrideJustification()).isNull();
    }

    @Test
    void overrideJustificationIsSentTrimmed() {
        OrderForm form = valid();
        form.setOverrideJustification(" MR-conditional pacemaker ");

        assertThat(form.toPayload().overrideJustification()).isEqualTo("MR-conditional pacemaker");
    }

    @Test
    void alternative4aMissingMandatoryDataIsReportedPerField() {
        assertThat(new OrderForm().validate()).containsOnlyKeys(
                "patientId", "examTypeCode", "clinicalIndication", "clinicalJustification", "priority");

        OrderForm form = valid();
        form.setClinicalJustification("   ");
        form.setPriority(null);
        assertThat(form.validate()).containsOnlyKeys("clinicalJustification", "priority");
    }

    @Test
    void tooLongTextIsRejected() {
        OrderForm form = valid();
        form.setClinicalIndication("a".repeat(OrderForm.MAX_TEXT_LENGTH + 1));
        form.setOverrideJustification("b".repeat(OrderForm.MAX_TEXT_LENGTH + 1));

        assertThat(form.validate()).containsOnlyKeys("clinicalIndication", "overrideJustification");
    }

    @Test
    void gettersReturnWhatWasSet() {
        OrderForm form = valid();
        assertThat(form.getPatientId()).isEqualTo(" PAT-1 ");
        assertThat(form.getExamTypeCode()).isEqualTo(" XR-CHEST ");
        assertThat(form.getClinicalIndication()).isEqualTo(" cough ");
        assertThat(form.getClinicalJustification()).isEqualTo(" rule out pneumonia ");
        assertThat(form.getPriority()).isEqualTo(PriorityLevel.ROUTINE);
        assertThat(form.getOverrideJustification()).isNull();
    }
}
