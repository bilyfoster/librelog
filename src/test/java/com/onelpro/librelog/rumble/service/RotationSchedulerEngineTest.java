package com.onelpro.librelog.rumble.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RotationSchedulerEngineTest {

    private final RotationSchedulerEngine engine = new RotationSchedulerEngine();
    private final Instant noon = Instant.parse("2026-07-20T12:00:00Z");

    @Test
    void rejectsSameSongInsideFourHours() {
        RotationScheduledItem previous = new RotationScheduledItem(10L, "Artist", noon);
        RotationScheduledItem candidate = new RotationScheduledItem(10L, "Other", noon.plusSeconds(239 * 60L));

        assertThat(engine.validateRotationRules(List.of(previous), candidate)).isFalse();
    }

    @Test
    void rejectsSameArtistInsideNinetyMinutes() {
        RotationScheduledItem previous = new RotationScheduledItem(10L, "The Band", noon);
        RotationScheduledItem candidate = new RotationScheduledItem(11L, "the band", noon.plusSeconds(89 * 60L));

        assertThat(engine.validateRotationRules(List.of(previous), candidate)).isFalse();
    }

    @Test
    void acceptsCandidateOutsideSongAndArtistWindows() {
        RotationScheduledItem song = new RotationScheduledItem(10L, "Artist", noon);
        RotationScheduledItem artist = new RotationScheduledItem(11L, "Same Artist", noon.plusSeconds(60));
        RotationScheduledItem candidate = new RotationScheduledItem(10L, "Same Artist", noon.plusSeconds(241 * 60L));

        assertThat(engine.validateRotationRules(List.of(song, artist), candidate)).isTrue();
    }

    @Test
    void requiresCandidateTime() {
        assertThatThrownBy(() -> engine.validateRotationRules(List.of(), new RotationScheduledItem(1L, "A", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scheduledAt");
    }
}
