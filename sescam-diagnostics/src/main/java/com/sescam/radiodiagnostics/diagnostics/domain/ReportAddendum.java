package com.sescam.radiodiagnostics.diagnostics.domain;

import com.sescam.radiodiagnostics.common.diagnostics.ReportAddendumDTO;

import java.time.Instant;
import java.util.Objects;

/**
 * Post-signature amendment appended to a finalized diagnostic report (6a of UC-PHY-04).
 */
public class ReportAddendum {

    private final String id;
    private final String reason;
    private final String content;
    private final String authorPhysicianId;
    private final String digitalSignature;
    private final Instant createdAt;

    public ReportAddendum(String id, String reason, String content,
                          String authorPhysicianId, String digitalSignature, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.reason = Objects.requireNonNull(reason, "reason");
        this.content = Objects.requireNonNull(content, "content");
        this.authorPhysicianId = Objects.requireNonNull(authorPhysicianId, "authorPhysicianId");
        this.digitalSignature = Objects.requireNonNull(digitalSignature, "digitalSignature");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public ReportAddendumDTO toDTO() {
        return new ReportAddendumDTO(id, reason, content, authorPhysicianId, digitalSignature, createdAt);
    }

    public String getId() {
        return id;
    }
}
