package com.sescam.radiodiagnostics.integration.repository;

import com.sescam.radiodiagnostics.common.integration.IntegrationType;
import com.sescam.radiodiagnostics.integration.domain.Integration;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory repository (no database decision yet), seeded with the four external systems of
 * FR-ADM-16 to FR-ADM-21, all not configured.
 */
public class InMemoryIntegrationRepository implements IntegrationRepository {

    private final Map<String, Integration> store = new ConcurrentHashMap<>();

    public InMemoryIntegrationRepository() {
        Instant seedTime = Instant.parse("2026-11-21T00:00:00Z");
        seed(Integration.notConfigured("sso", IntegrationType.SSO, "Single sign-on (SSO)", seedTime));
        seed(Integration.notConfigured("ehr", IntegrationType.EHR, "Electronic health record (EHR/HCE)", seedTime));
        seed(Integration.notConfigured("ris-pacs", IntegrationType.RIS_PACS, "RIS/PACS (DICOM)", seedTime));
        seed(Integration.notConfigured("messaging", IntegrationType.MESSAGING, "Messaging (SMS and email)", seedTime));
    }

    private void seed(Integration integration) {
        store.put(integration.getId(), integration);
    }

    @Override
    public List<Integration> findAll() {
        return store.values().stream().sorted(Comparator.comparing(Integration::getId)).toList();
    }

    @Override
    public Optional<Integration> findById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(store.get(id));
    }

    @Override
    public Integration save(Integration integration) {
        store.put(integration.getId(), integration);
        return integration;
    }
}
