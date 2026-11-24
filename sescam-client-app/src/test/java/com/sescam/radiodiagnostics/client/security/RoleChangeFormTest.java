package com.sescam.radiodiagnostics.client.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleChangeFormTest {

    private static RoleChangeForm form(String userId, String roleName) {
        RoleChangeForm form = new RoleChangeForm();
        form.setUserId(userId);
        form.setRoleName(roleName);
        return form;
    }

    @Test
    void validFormHasNoErrorsAndTrimsTheFields() {
        RoleChangeForm form = form("  physician-7 ", " PHYSICIAN ");

        assertThat(form.validate(true)).isEmpty();
        assertThat(form.getUserId()).isEqualTo("physician-7");
        assertThat(form.getRoleName()).isEqualTo("PHYSICIAN");
    }

    @Test
    void lookupOnlyNeedsTheUser() {
        assertThat(form("admin-2", null).validate(false)).isEmpty();
        assertThat(form("admin-2", null).validate(true)).containsOnlyKeys("roleName");
    }

    @Test
    void missingFieldsAreReportedPerField() {
        assertThat(form(null, "  ").validate(true)).containsOnlyKeys("userId", "roleName");
    }

    @Test
    void tooLongOrUnsafeValuesAreRejected() {
        assertThat(form("a".repeat(101), "R").validate(true)).containsKey("userId");
        assertThat(form("user 1", "R").validate(true)).containsKey("userId");
        assertThat(form("u1", "ADMIN;DROP").validate(true)).containsKey("roleName");
    }
}
