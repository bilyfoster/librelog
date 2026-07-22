package com.onelpro.librelog.rumble.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JazzHandoffServiceTest {

    private final JazzHandoffService service = new JazzHandoffService(
            Clock.fixed(Instant.parse("2026-07-20T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void blocksTodayAndPastDates() {
        assertThatThrownBy(() -> service.assertSafeHandoffDate(LocalDate.parse("2026-07-20")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("active 24-hour playout buffers");
        assertThatThrownBy(() -> service.assertSafeHandoffDate(LocalDate.parse("2026-07-19")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsTomorrowAndReturnsPlan() {
        JazzHandoffService.HandoffPlan plan = service.commitLogToJazz(LocalDate.parse("2026-07-21"), List.of());

        assertThat(plan.targetDate()).isEqualTo(LocalDate.parse("2026-07-21"));
        assertThat(plan.entryCount()).isZero();
    }
}
