package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * The actor may not log procedure executions. HTTP 403.
 */
public class UnauthorizedExecutionAccessException extends RuntimeException {
    public UnauthorizedExecutionAccessException(String message) {
        super(message);
    }
}
