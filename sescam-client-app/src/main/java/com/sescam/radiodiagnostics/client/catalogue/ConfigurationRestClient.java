package com.sescam.radiodiagnostics.client.catalogue;

import com.sescam.radiodiagnostics.common.catalogue.ConfigurationParameterDTO;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterRequestDTO;
import com.sescam.radiodiagnostics.common.catalogue.UpdateParameterResponseDTO;
import com.sescam.radiodiagnostics.common.catalogue.ValidationResultDTO;

import java.util.List;

/**
 * Client-side REST port communicating with sescam-server-app using sescam-common DTOs.
 */
public interface ConfigurationRestClient {

    List<ConfigurationParameterDTO> fetchAllParameters(String actorId, String actorRole);

    ValidationResultDTO validateRemotely(String parameterKey, String proposedValue, String actorId, String actorRole);

    UpdateParameterResponseDTO updateParameter(UpdateParameterRequestDTO request);
}
