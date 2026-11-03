package br.com.fiap.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.library.domain.exception.InvalidDataException;
import br.com.fiap.library.domain.service.LibraryPolicy;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class LibraryPolicyTest {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private final LibraryPolicy policy = new LibraryPolicy(14, 3, Duration.ofHours(48), SAO_PAULO);

    @Test
    void due_date_is_the_end_of_the_local_day_14_days_later() {
        Instant due = policy.dueDate(Instant.parse("2026-11-02T13:00:00Z"));

        assertThat(due.atZone(SAO_PAULO).toString()).startsWith("2026-11-16T23:59:59-03:00");
    }

    @Test
    void due_date_uses_the_local_calendar_near_midnight() {
        // 01:30 UTC do dia 3 ainda é 22:30 do dia 2 em São Paulo.
        Instant due = policy.dueDate(Instant.parse("2026-11-03T01:30:00Z"));

        assertThat(due.atZone(SAO_PAULO).toLocalDate().toString()).isEqualTo("2026-11-16");
    }

    @Test
    void hold_lasts_48_hours() {
        Instant start = Instant.parse("2026-11-02T13:00:00Z");

        assertThat(policy.holdUntil(start)).isEqualTo(Instant.parse("2026-11-04T13:00:00Z"));
    }

    @Test
    void rejects_invalid_configuration() {
        assertThatThrownBy(() -> new LibraryPolicy(0, 3, Duration.ofHours(1), SAO_PAULO))
                .isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> new LibraryPolicy(14, 3, Duration.ZERO, SAO_PAULO))
                .isInstanceOf(InvalidDataException.class);
    }
}
