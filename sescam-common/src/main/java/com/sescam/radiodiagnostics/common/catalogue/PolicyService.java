package com.sescam.radiodiagnostics.common.catalogue;

import java.util.Optional;

/**
 * Public policy contract offered by C2 (sescam-catalogue) to other domain modules
 * (e.g., cancellation deadlines, triage response windows, notification retry limits).
 */
public interface PolicyService {

    /**
     * Returns the current string value of a configured policy parameter.
     */
    Optional<String> getPolicyValue(String parameterKey);

    /**
     * Returns an integer policy value or the supplied fallback if not configured.
     */
    int getIntPolicy(String parameterKey, int defaultValue);
}
