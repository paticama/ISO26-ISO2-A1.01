package com.sescam.radiodiagnostics.client.security;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Form of the UC-ADM-02 view. It validates the fields before calling the server so the administrator gets the
 * message without a round trip. The server repeats every check (roles and access rules are decided there).
 */
public class RoleChangeForm {

    static final int MAX_LENGTH = 100;
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9._:@-]+");

    private String userId;
    private String roleName;

    /** Field name → error message; empty when the form is valid. {@code roleRequired} is false for a lookup. */
    public Map<String, String> validate(boolean roleRequired) {
        Map<String, String> errors = new LinkedHashMap<>();
        check(errors, "userId", userId, "Enter the user account to look up.");
        if (roleRequired) {
            check(errors, "roleName", roleName, "Enter the role to assign.");
        }
        return errors;
    }

    private static void check(Map<String, String> errors, String field, String value, String missingMessage) {
        if (value == null || value.isBlank()) {
            errors.put(field, missingMessage);
        } else if (value.trim().length() > MAX_LENGTH) {
            errors.put(field, "Use at most " + MAX_LENGTH + " characters.");
        } else if (!IDENTIFIER.matcher(value.trim()).matches()) {
            errors.put(field, "Use only letters, digits and . _ : @ -");
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId == null ? null : userId.trim();
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName == null ? null : roleName.trim();
    }
}
