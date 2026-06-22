package com.onelpro.librelog.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeWindowUtilTest {

    @Test
    void normalizeExclusiveEnd_treatsMidnightAsEndOfDay() {
        assertThat(TimeWindowUtil.normalizeExclusiveEnd(0)).isEqualTo(1440);
        assertThat(TimeWindowUtil.normalizeExclusiveEnd(720)).isEqualTo(720);
        assertThat(TimeWindowUtil.normalizeExclusiveEnd((Integer) null)).isNull();
    }

    @Test
    void isMinuteWithin_nullWindowIsAlwaysInside() {
        assertThat(TimeWindowUtil.isMinuteWithin(null, null, 500)).isTrue();
    }

    @Test
    void isMinuteWithin_normalWindowIsHalfOpen() {
        // 06:00 (360) .. 12:00 (720), end exclusive
        assertThat(TimeWindowUtil.isMinuteWithin(360, 720, 360)).isTrue();   // start inclusive
        assertThat(TimeWindowUtil.isMinuteWithin(360, 720, 719)).isTrue();
        assertThat(TimeWindowUtil.isMinuteWithin(360, 720, 720)).isFalse();  // end exclusive
        assertThat(TimeWindowUtil.isMinuteWithin(360, 720, 300)).isFalse();
    }

    @Test
    void isMinuteWithin_wrapsPastMidnight() {
        // 22:00 (1320) .. 06:00 (360)
        assertThat(TimeWindowUtil.isMinuteWithin(1320, 360, 1380)).isTrue(); // 23:00
        assertThat(TimeWindowUtil.isMinuteWithin(1320, 360, 120)).isTrue();  // 02:00
        assertThat(TimeWindowUtil.isMinuteWithin(1320, 360, 720)).isFalse(); // noon
    }

    @Test
    void isInstantWithin_usesStationZone() {
        ZoneId ny = ZoneId.of("America/New_York");
        // 2024-06-01T12:00Z == 08:00 in New York (480 minutes)
        Instant noonUtc = Instant.parse("2024-06-01T12:00:00Z");
        assertThat(TimeWindowUtil.isInstantWithin(360, 720, ny, noonUtc)).isTrue();   // 06:00-12:00 local
        assertThat(TimeWindowUtil.isInstantWithin(540, 720, ny, noonUtc)).isFalse();  // 09:00-12:00 local
        assertThat(TimeWindowUtil.isInstantWithin(360, 720, ZoneOffset.UTC, noonUtc)).isFalse(); // 12:00 UTC excluded
    }

    @Test
    void validateWindowPair_rejectsHalfSetWindow() {
        assertThatThrownBy(() -> TimeWindowUtil.validateWindowPair(360, null, "Spot"))
                .isInstanceOf(IllegalArgumentException.class);
        // both null is fine (no window)
        TimeWindowUtil.validateWindowPair(null, null, "Spot");
    }
}
