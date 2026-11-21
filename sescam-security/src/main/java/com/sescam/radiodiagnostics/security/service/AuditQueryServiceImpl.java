package com.sescam.radiodiagnostics.security.service;

import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditQueryFailedException;
import com.sescam.radiodiagnostics.common.security.AuditQueryService;
import com.sescam.radiodiagnostics.common.security.AuditQueryTooBroadException;
import com.sescam.radiodiagnostics.common.security.AuditRecordDTO;
import com.sescam.radiodiagnostics.common.security.AuditResultStatus;
import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.security.AuditSearchResultDTO;
import com.sescam.radiodiagnostics.common.security.AuditService;
import com.sescam.radiodiagnostics.common.security.InvalidAuditQueryException;
import com.sescam.radiodiagnostics.common.security.UnauthorizedAuditAccessException;
import com.sescam.radiodiagnostics.security.domain.AuditEvent;
import com.sescam.radiodiagnostics.security.internal.AuditAccessPolicy;
import com.sescam.radiodiagnostics.security.repository.AuditEventRepository;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * UC-ADM-03 – Audit and compliance management (C1 v1.1.0).
 * <p>
 * Searching never changes the stored events. Every access to the audit trail (allowed or denied)
 * is itself appended as a new audit event, because audit records describe access to clinical data.
 */
public class AuditQueryServiceImpl implements AuditQueryService {

    public static final String ACTION_QUERIED = "AUDIT_RECORDS_QUERIED";
    public static final String ACTION_EXPORTED = "AUDIT_RECORDS_EXPORTED";
    public static final String ACTION_ACCESS_DENIED = "AUDIT_ACCESS_DENIED";

    public static final Duration DEFAULT_MAX_RANGE = Duration.ofDays(31);
    public static final int DEFAULT_MAX_RESULTS = 500;

    private static final String CSV_HEADER =
            "eventId,sequenceNumber,timestamp,actorId,action,resourceKey,previousValue,newValue,details";

    private final AuditEventRepository repository;
    private final AuditService auditService;
    private final AuditAccessPolicy accessPolicy;
    private final Clock clock;
    private final Duration maxRange;
    private final int maxResults;

    public AuditQueryServiceImpl(AuditEventRepository repository, AuditService auditService,
                                 AuditAccessPolicy accessPolicy, Clock clock) {
        this(repository, auditService, accessPolicy, clock, DEFAULT_MAX_RANGE, DEFAULT_MAX_RESULTS);
    }

    public AuditQueryServiceImpl(AuditEventRepository repository, AuditService auditService,
                                 AuditAccessPolicy accessPolicy, Clock clock,
                                 Duration maxRange, int maxResults) {
        this.repository = Objects.requireNonNull(repository);
        this.auditService = Objects.requireNonNull(auditService);
        this.accessPolicy = Objects.requireNonNull(accessPolicy);
        this.clock = Objects.requireNonNull(clock);
        this.maxRange = Objects.requireNonNull(maxRange);
        this.maxResults = maxResults;
    }

    @Override
    public AuditSearchResultDTO searchEvents(AuditSearchCriteriaDTO criteria, String requesterId,
                                             String requesterRole) {
        List<AuditRecordDTO> records = runQuery(criteria, requesterId, requesterRole);
        recordAccess(ACTION_QUERIED, requesterId, criteria, records.size());
        AuditResultStatus status = records.isEmpty() ? AuditResultStatus.NO_MATCHES : AuditResultStatus.MATCHES_FOUND;
        return new AuditSearchResultDTO(criteria, status, records.size(), records);
    }

    @Override
    public String exportEventsAsCsv(AuditSearchCriteriaDTO criteria, String requesterId, String requesterRole) {
        List<AuditRecordDTO> records = runQuery(criteria, requesterId, requesterRole);
        recordAccess(ACTION_EXPORTED, requesterId, criteria, records.size());
        StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
        for (AuditRecordDTO r : records) {
            csv.append(String.join(",",
                    csv(r.eventId()), String.valueOf(r.sequenceNumber()), csv(String.valueOf(r.timestamp())),
                    csv(r.actorId()), csv(r.action()), csv(r.resourceKey()), csv(r.previousValue()),
                    csv(r.newValue()), csv(r.details()))).append('\n');
        }
        return csv.toString();
    }

    private List<AuditRecordDTO> runQuery(AuditSearchCriteriaDTO criteria, String requesterId, String requesterRole) {
        checkAccess(criteria, requesterId, requesterRole);
        validate(criteria);
        List<AuditEvent> matches;
        try {
            matches = repository.findMatching(criteria);
        } catch (RuntimeException ex) {
            throw new AuditQueryFailedException("The audit records could not be queried. Please retry.", ex);
        }
        if (matches.size() > maxResults) {
            throw new AuditQueryTooBroadException(
                    "The search returns " + matches.size() + " events (maximum " + maxResults
                            + "). Narrow the time range or add filters.", maxResults, matches.size());
        }
        return matches.stream().map(AuditEvent::toRecord).toList();
    }

    private void checkAccess(AuditSearchCriteriaDTO criteria, String requesterId, String requesterRole) {
        if (!accessPolicy.canViewAuditRecords(requesterId, requesterRole)) {
            String actor = requesterId == null || requesterId.isBlank() ? "anonymous" : requesterId;
            auditService.recordEvent(new AuditEventDTO(ACTION_ACCESS_DENIED, actor, "audit-trail", null, null,
                    "role=" + requesterRole + "; " + describe(criteria), clock.instant()));
            throw new UnauthorizedAuditAccessException("You are not authorized to view audit records.");
        }
    }

    private void validate(AuditSearchCriteriaDTO criteria) {
        if (criteria == null || criteria.from() == null || criteria.to() == null) {
            throw new InvalidAuditQueryException("A time range (from and to) is required.");
        }
        if (criteria.from().isAfter(criteria.to())) {
            throw new InvalidAuditQueryException("The start of the time range must not be after its end.");
        }
        Duration range = Duration.between(criteria.from(), criteria.to());
        if (range.compareTo(maxRange) > 0) {
            throw new AuditQueryTooBroadException(
                    "The time range is " + range.toDays() + " days (maximum " + maxRange.toDays()
                            + "). Narrow the time range.", maxRange.toDays(), range.toDays());
        }
    }

    private void recordAccess(String action, String requesterId, AuditSearchCriteriaDTO criteria, int matches) {
        try {
            auditService.recordEvent(new AuditEventDTO(action, requesterId, "audit-trail", null, null,
                    describe(criteria) + "; matches=" + matches, clock.instant()));
        } catch (RuntimeException ex) {
            // Access to audit data that cannot be traced must not be granted (ADR-008).
            throw new AuditQueryFailedException("The audit access could not be recorded. Please retry.", ex);
        }
    }

    private static String describe(AuditSearchCriteriaDTO c) {
        if (c == null) {
            return "criteria=none";
        }
        return "from=" + c.from() + ", to=" + c.to() + ", performedBy=" + c.performedBy()
                + ", action=" + c.action() + ", resourceKey=" + c.resourceKey();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
