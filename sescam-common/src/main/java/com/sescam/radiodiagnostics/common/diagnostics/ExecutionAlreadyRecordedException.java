package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * The procedure already has an execution record. HTTP 409.
 */
public class ExecutionAlreadyRecordedException extends RuntimeException {
    public ExecutionAlreadyRecordedException(String message) {
        super(message);
    }
}
