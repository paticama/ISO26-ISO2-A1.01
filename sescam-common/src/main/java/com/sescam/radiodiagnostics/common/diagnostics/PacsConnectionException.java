package com.sescam.radiodiagnostics.common.diagnostics;

/**
 * Departmental RIS/PACS archive failed or was unreachable while fetching DICOM study metadata (2a of UC-PHY-04). HTTP 502.
 */
public class PacsConnectionException extends RuntimeException {

    public PacsConnectionException(String message) {
        super(message);
    }

    public PacsConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
