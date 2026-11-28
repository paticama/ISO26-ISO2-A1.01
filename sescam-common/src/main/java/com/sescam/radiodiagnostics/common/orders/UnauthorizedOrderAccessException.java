package com.sescam.radiodiagnostics.common.orders;

/**
 * The actor's role may not order diagnostic procedures. HTTP 403.
 */
public class UnauthorizedOrderAccessException extends RuntimeException {
    public UnauthorizedOrderAccessException(String message) {
        super(message);
    }
}
