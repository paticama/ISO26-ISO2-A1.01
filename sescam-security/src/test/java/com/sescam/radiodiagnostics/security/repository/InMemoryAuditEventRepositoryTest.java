package com.sescam.radiodiagnostics.security.repository;

import com.sescam.radiodiagnostics.common.security.AuditEventDTO;
import com.sescam.radiodiagnostics.common.security.AuditSearchCriteriaDTO;
import com.sescam.radiodiagnostics.security.domain.AuditEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryAuditEventRepositoryTest {

    private static final Instant T = Instant.parse("2026-11-20T10:00:00Z");

    @Test
    void eventsWithTheSameTimestampKeepTheirRecordingOrder() {
        InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository();
        for (String action : new String[]{"A", "B", "C"}) {
            repository.append(AuditEvent.from(new AuditEventDTO(action, "u1", null, null, null, null, T),
                    repository.nextSequenceNumber(), T));
        }

        assertThat(repository.findMatching(new AuditSearchCriteriaDTO(T, T, null, null, null)))
                .extracting(AuditEvent::getAction).containsExactly("A", "B", "C");
        assertThat(repository.count()).isEqualTo(3);
        assertThatThrownBy(() -> repository.append(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void repositoryOffersNoUpdateOrDeleteOperation() {
        assertThat(Arrays.stream(AuditEventRepository.class.getMethods()).map(Method::getName))
                .noneMatch(name -> name.startsWith("delete") || name.startsWith("remove")
                        || name.startsWith("update") || name.startsWith("save"));
    }
}
