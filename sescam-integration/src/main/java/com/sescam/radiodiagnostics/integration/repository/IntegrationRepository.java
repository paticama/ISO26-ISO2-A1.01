package com.sescam.radiodiagnostics.integration.repository;

import com.sescam.radiodiagnostics.integration.domain.Integration;

import java.util.List;
import java.util.Optional;

public interface IntegrationRepository {

    List<Integration> findAll();

    Optional<Integration> findById(String id);

    Integration save(Integration integration);
}
