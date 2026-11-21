package com.sescam.radiodiagnostics.catalogue.service;

import com.sescam.radiodiagnostics.catalogue.domain.ConfigurationParameter;
import com.sescam.radiodiagnostics.catalogue.repository.ConfigurationParameterRepository;
import com.sescam.radiodiagnostics.common.catalogue.PolicyService;

import java.util.Objects;
import java.util.Optional;

/**
 * Policy lookup service exposing configured parameter values to collaborating domain modules.
 */
public class PolicyServiceImpl implements PolicyService {

    private final ConfigurationParameterRepository repository;

    public PolicyServiceImpl(ConfigurationParameterRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Optional<String> getPolicyValue(String parameterKey) {
        if (parameterKey == null || parameterKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findByKey(parameterKey).map(ConfigurationParameter::getCurrentValue);
    }

    @Override
    public int getIntPolicy(String parameterKey, int defaultValue) {
        return getPolicyValue(parameterKey)
                .map(val -> {
                    try {
                        return Integer.parseInt(val.trim());
                    } catch (NumberFormatException ex) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }
}
