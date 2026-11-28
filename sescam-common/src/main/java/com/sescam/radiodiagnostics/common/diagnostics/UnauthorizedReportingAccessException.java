package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * Actor lacks physician/radiologist privileges to access DICOM studies or author reports. HTTP 403.
 */
public class UnauthorizedReportingAccessException extends RuntimeException {

    public UnauthorizedReportingAccessException(String message) {
        super(message);
    }
}
