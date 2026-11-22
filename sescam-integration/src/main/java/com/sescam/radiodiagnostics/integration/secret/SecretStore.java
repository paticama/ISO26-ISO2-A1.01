package com.sescam.radiodiagnostics.integration.secret;

import java.util.Optional;

/**
 * The approved secure mechanism for credentials (UC-ADM-04, step 4 and A4). Credentials go in and are only
 * read back by the server to run a connection check; they are never returned to the client.
 */
public interface SecretStore {

    void put(String integrationId, String secret);

    Optional<String> get(String integrationId);

    boolean contains(String integrationId);
}
