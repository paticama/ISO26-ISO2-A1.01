package com.sescam.radiodiagnostics.common.catalogue;

/**
 * Defines how a configuration parameter change becomes effective in the system.
 */
public enum ActivationMode {
    IMMEDIATE,
    REQUIRES_RELOAD,
    REQUIRES_RESTART
}
