package com.sescam.radiodiagnostics.catalogue.repository;

import com.sescam.radiodiagnostics.catalogue.domain.ConfigurationParameter;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for configuration parameters owned by C2 (sescam-catalogue).
 */
public interface ConfigurationParameterRepository {

    List<ConfigurationParameter> findAll();

    Optional<ConfigurationParameter> findByKey(String key);

    ConfigurationParameter save(ConfigurationParameter parameter);
}
