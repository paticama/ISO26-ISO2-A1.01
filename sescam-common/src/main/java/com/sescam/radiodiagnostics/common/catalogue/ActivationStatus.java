package com.sescam.radiodiagnostics.common.catalogue;

/**
 * Reports whether a saved configuration change is already active or pending a reload/restart.
 */
public enum ActivationStatus {
    ACTIVE,
    PENDING_RELOAD,
    PENDING_RESTART
}
