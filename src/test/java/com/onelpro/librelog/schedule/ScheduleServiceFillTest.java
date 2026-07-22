package com.onelpro.librelog.schedule;

import com.onelpro.librelog.carts.ClockTemplateSlot;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for fill-block expansion when a clock is applied. */
class ScheduleServiceFillTest {

    private ClockTemplateSlot slot(String kind, String fillMode, Integer count, Integer seconds, Integer defaultLen) {
        return ClockTemplateSlot.builder()
                .kind(kind).fillMode(fillMode)
                .fillTargetCount(count).fillTargetSeconds(seconds)
                .defaultLengthSeconds(defaultLen)
                .build();
    }

    @Test
    void plainSlotIsOneUnit() {
        assertThat(ScheduleService.fillUnitCount(slot("COMMERCIAL_CART", null, null, null, null))).isEqualTo(1);
    }

    @Test
    void countModeUsesConfiguredCount() {
        assertThat(ScheduleService.fillUnitCount(slot("COMMERCIAL_CART", "COUNT", 4, null, null))).isEqualTo(4);
        // capped at 50
        assertThat(ScheduleService.fillUnitCount(slot("COMMERCIAL_CART", "COUNT", 500, null, null))).isEqualTo(50);
    }

    @Test
    void timeModeDividesByUnitLengthRoundingUp() {
        // 3:00 of 30s commercial units -> 6
        assertThat(ScheduleService.fillUnitCount(slot("COMMERCIAL_CART", "TIME", null, 180, null))).isEqualTo(6);
        // 100s of 30s units -> ceil(3.33) = 4
        assertThat(ScheduleService.fillUnitCount(slot("COMMERCIAL_CART", "TIME", null, 100, null))).isEqualTo(4);
        // explicit unit length wins: 300s of 60s units -> 5
        assertThat(ScheduleService.fillUnitCount(slot("COMMERCIAL_CART", "TIME", null, 300, 60))).isEqualTo(5);
        // music default unit is 180s: 600s -> ceil(3.33) = 4
        assertThat(ScheduleService.fillUnitCount(slot("MUSIC_CART", "TIME", null, 600, null))).isEqualTo(4);
    }

    @Test
    void toEndStaysASingleMarker() {
        assertThat(ScheduleService.fillUnitCount(slot("MUSIC_CART", "TO_END", null, null, null))).isEqualTo(1);
    }
}
