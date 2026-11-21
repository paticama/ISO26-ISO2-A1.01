package com.sescam.radiodiagnostics.catalogue.repository;

import com.sescam.radiodiagnostics.catalogue.domain.ConfigurationParameter;
import com.sescam.radiodiagnostics.common.catalogue.ActivationMode;
import com.sescam.radiodiagnostics.common.catalogue.ParameterValueType;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory repository seeded with default SESCAM Radiodiagnostics parameters
 * realizing FR-ADM-01 through FR-ADM-06.
 */
public class InMemoryConfigurationParameterRepository implements ConfigurationParameterRepository {

    private final Map<String, ConfigurationParameter> store = new ConcurrentHashMap<>();

    public InMemoryConfigurationParameterRepository() {
        seedDefaults();
    }

    private void seedDefaults() {
        Instant seedTime = Instant.parse("2026-11-18T00:00:00Z");
        putDefault(new ConfigurationParameter(
                "exam.default.duration.minutes",
                "Default Examination Duration (minutes)",
                "EXAM_CATALOGUE",
                "Standard slot duration in minutes for newly created imaging examination types (FR-ADM-01).",
                "30",
                ParameterValueType.INTEGER,
                5.0,
                240.0,
                List.of(),
                ActivationMode.IMMEDIATE,
                1L,
                "system",
                seedTime
        ));
        putDefault(new ConfigurationParameter(
                "triage.urgent.sla.hours",
                "Urgent Triage Target Response Window (hours)",
                "TRIAGE_POLICIES",
                "Maximum response window in hours for STAT/Urgent imaging orders (FR-ADM-02).",
                "4",
                ParameterValueType.INTEGER,
                1.0,
                48.0,
                List.of(),
                ActivationMode.IMMEDIATE,
                1L,
                "system",
                seedTime
        ));
        putDefault(new ConfigurationParameter(
                "booking.cancellation.min.hours",
                "Minimum Patient Cancellation Window (hours)",
                "BOOKING_POLICIES",
                "Minimum hours before a scheduled appointment that a patient can self-cancel (FR-ADM-03).",
                "24",
                ParameterValueType.INTEGER,
                1.0,
                168.0,
                List.of(),
                ActivationMode.IMMEDIATE,
                1L,
                "system",
                seedTime
        ));
        putDefault(new ConfigurationParameter(
                "protocol.contrast.fasting.hours",
                "Default Contrast Fasting Period (hours)",
                "CLINICAL_PROTOCOLS",
                "Standard pre-procedure fasting window for contrast-enhanced procedures (FR-ADM-04, FR-ADM-05).",
                "6",
                ParameterValueType.INTEGER,
                0.0,
                24.0,
                List.of(),
                ActivationMode.REQUIRES_RELOAD,
                1L,
                "system",
                seedTime
        ));
        putDefault(new ConfigurationParameter(
                "notification.max.retries",
                "Notification Dispatch Maximum Retries",
                "NOTIFICATIONS",
                "Maximum retry attempts for failed SMS/email patient reminders (FR-ADM-06).",
                "3",
                ParameterValueType.INTEGER,
                0.0,
                10.0,
                List.of(),
                ActivationMode.REQUIRES_RELOAD,
                1L,
                "system",
                seedTime
        ));
        putDefault(new ConfigurationParameter(
                "notification.quiet.hours.enabled",
                "Notification Quiet Hours Enabled",
                "NOTIFICATIONS",
                "Prevents non-urgent patient notifications during overnight quiet hours (FR-ADM-06).",
                "true",
                ParameterValueType.BOOLEAN,
                null,
                null,
                List.of(),
                ActivationMode.IMMEDIATE,
                1L,
                "system",
                seedTime
        ));
        putDefault(new ConfigurationParameter(
                "system.pacs.transfer.mode",
                "RIS/PACS Transfer Protocol Mode",
                "SYSTEM_CORE",
                "Primary DICOM retrieval protocol used by the integrated diagnostic viewer.",
                "WADO-RS",
                ParameterValueType.ENUM,
                null,
                null,
                List.of("WADO-RS", "WADO-URI", "C-MOVE"),
                ActivationMode.REQUIRES_RESTART,
                1L,
                "system",
                seedTime
        ));
    }

    private void putDefault(ConfigurationParameter parameter) {
        store.put(parameter.getKey(), parameter);
    }

    @Override
    public List<ConfigurationParameter> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(ConfigurationParameter::getKey))
                .toList();
    }

    @Override
    public Optional<ConfigurationParameter> findByKey(String key) {
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public ConfigurationParameter save(ConfigurationParameter parameter) {
        store.put(parameter.getKey(), parameter);
        return parameter;
    }
}
