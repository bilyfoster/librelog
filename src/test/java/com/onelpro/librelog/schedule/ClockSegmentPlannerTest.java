package com.onelpro.librelog.schedule;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Timing-engine tests: anchors (soft/hard), pad insertion, avail caps, TO_END
 * back-timing, and the never-trim rule for non-music content. Uses a scripted
 * {@link ClockSegmentPlanner.UnitSource} — no Spring, no mocks of repositories.
 */
class ClockSegmentPlannerTest {

    private final Instant start = Instant.parse("2026-08-01T06:00:00Z");

    // ---- scripted source -------------------------------------------------------------

    private ClockSegmentPlanner.UnitSource source(int musicLen, int spotLen, int padLen) {
        return new ClockSegmentPlanner.UnitSource() {
            @Override
            public ClockSegmentPlanner.Resolved resolveItem(ScheduleItem it, Instant at) {
                if ("COMMERCIAL_CART".equals(it.getKind())) {
                    return ClockSegmentPlanner.Resolved.of(new ClockSegmentPlanner.Unit(
                            300L, spotLen, null, null, UUID.randomUUID(), "Spot", null, false));
                }
                return ClockSegmentPlanner.Resolved.of(new ClockSegmentPlanner.Unit(
                        100L, musicLen, null, null, null, "Song", null, true));
            }

            @Override
            public ClockSegmentPlanner.Resolved resolvePad(Instant at) {
                return ClockSegmentPlanner.Resolved.of(new ClockSegmentPlanner.Unit(
                        900L, padLen, null, null, null, "Sweep", null, true));
            }
        };
    }

    private ScheduleItem fixed(String kind, long fileId, int len) {
        return ScheduleItem.builder().id(UUID.randomUUID()).scheduleDayId(UUID.randomUUID())
                .kind(kind).librtimeFileId(fileId).lengthSeconds(len).build();
    }

    private ScheduleItem cartItem(String kind) {
        return ScheduleItem.builder().id(UUID.randomUUID()).scheduleDayId(UUID.randomUUID())
                .kind(kind).cartId(UUID.randomUUID()).build();
    }

    private ScheduleItem anchored(ScheduleItem it, int offsetSeconds, String policy) {
        it.setAnchorOffsetSeconds(offsetSeconds);
        it.setAnchorPolicy(policy);
        return it;
    }

    // ---- tests -----------------------------------------------------------------------

    @Test
    void plainSequencePacksBackToBack() {
        var plan = ClockSegmentPlanner.plan(
                List.of(fixed("TRACK", 1L, 120), fixed("TRACK", 2L, 60)),
                start, start.plusSeconds(3600), source(180, 30, 30));
        assertThat(plan.rows()).hasSize(2);
        assertThat(plan.rows().get(0).startsAt()).isEqualTo(start);
        assertThat(plan.rows().get(1).startsAt()).isEqualTo(start.plusSeconds(120));
    }

    @Test
    void hardAnchorTrimsPrecedingMusicToLandExactly() {
        // 200s of music before a break hard-anchored at 3:00 -> music trimmed to 180.
        var music = cartItem("MUSIC_CART");
        var breakId = anchored(fixed("TRACK", 5L, 60), 180, "HARD");
        var plan = ClockSegmentPlanner.plan(List.of(music, breakId),
                start, start.plusSeconds(3600), source(200, 30, 30));
        var rows = plan.rows();
        assertThat(rows.get(0).cueOutSeconds()).isEqualTo(180);
        assertThat(rows.get(0).trimmed()).isTrue();
        assertThat(rows.get(1).startsAt()).isEqualTo(start.plusSeconds(180));
        assertThat(plan.notes()).anyMatch(n -> n.contains("Back-timed"));
    }

    @Test
    void softAnchorStartsLateAndFlags() {
        var music = cartItem("MUSIC_CART");
        var breakId = anchored(fixed("TRACK", 5L, 60), 180, "SOFT");
        var plan = ClockSegmentPlanner.plan(List.of(music, breakId),
                start, start.plusSeconds(3600), source(200, 30, 30));
        assertThat(plan.rows().get(0).cueOutSeconds()).isEqualTo(200); // untouched
        assertThat(plan.rows().get(1).startsAt()).isEqualTo(start.plusSeconds(200));
        assertThat(plan.notes()).anyMatch(n -> n.toLowerCase().contains("missed by 20"));
    }

    @Test
    void underrunPadsUpToTheAnchor() {
        // 100s of content, anchor at 3:00 -> 80s of pads (30+30+20-trimmed).
        var track = fixed("TRACK", 1L, 100);
        var breakId = anchored(fixed("TRACK", 5L, 60), 180, "SOFT");
        var plan = ClockSegmentPlanner.plan(List.of(track, breakId),
                start, start.plusSeconds(3600), source(180, 30, 30));
        var rows = plan.rows();
        assertThat(rows).hasSize(5); // track + 3 pads + break
        assertThat(rows.get(1).isPad()).isTrue();
        assertThat(rows.get(3).cueOutSeconds()).isEqualTo(20); // last pad trimmed
        assertThat(rows.get(4).startsAt()).isEqualTo(start.plusSeconds(180)); // break lands exactly
    }

    @Test
    void tinyGapStartsEarlyInsteadOfPadding() {
        var track = fixed("TRACK", 1L, 177);
        var breakId = anchored(fixed("TRACK", 5L, 60), 180, "SOFT");
        var plan = ClockSegmentPlanner.plan(List.of(track, breakId),
                start, start.plusSeconds(3600), source(180, 30, 30));
        assertThat(plan.rows()).hasSize(2);
        assertThat(plan.rows().get(1).startsAt()).isEqualTo(start.plusSeconds(177));
    }

    @Test
    void availSecondsCapStopsTheGroup() {
        // Four 30s spots in one avail capped at 60s -> two air, two dropped.
        UUID group = UUID.randomUUID();
        var items = new java.util.ArrayList<ScheduleItem>();
        for (int i = 0; i < 4; i++) {
            var it = cartItem("COMMERCIAL_CART");
            it.setFillGroup(group);
            it.setFillTargetSeconds(60);
            it.setLabel("Ad " + (i + 1));
            items.add(it);
        }
        var plan = ClockSegmentPlanner.plan(items, start, start.plusSeconds(3600), source(180, 30, 30));
        assertThat(plan.rows()).hasSize(2);
        assertThat(plan.skipped()).isEqualTo(2);
        assertThat(plan.notes()).anyMatch(n -> n.contains("Avail full"));
    }

    @Test
    void toEndFillBackTimesTheTopOfTheHour() {
        // 600s instance: 100s fixed + fill of 180s songs -> 180+180 then a 140s trim.
        var track = fixed("TRACK", 1L, 100);
        var marker = cartItem("MUSIC_CART");
        marker.setFillMode("TO_END");
        var plan = ClockSegmentPlanner.plan(List.of(track, marker),
                start, start.plusSeconds(600), source(180, 30, 30));
        var rows = plan.rows();
        assertThat(rows).hasSize(4);
        long total = rows.stream().mapToLong(ClockSegmentPlanner.PlannedRow::cueOutSeconds).sum();
        assertThat(total).isEqualTo(600);
        assertThat(rows.get(3).trimmed()).isTrue();
        assertThat(rows.get(3).cueOutSeconds()).isEqualTo(140);
        assertThat(rows.get(3).endsAt()).isEqualTo(start.plusSeconds(600)); // exact top of hour
        assertThat(rows.get(1).isFillUnit()).isTrue();
        assertThat(rows.get(1).item()).isEqualTo(marker); // marker linked for bookkeeping
    }

    @Test
    void nonMusicOverrunIsNeverCut() {
        // A 400s spot/interview running past a 300s hard anchor is flagged, not trimmed.
        var longSpot = cartItem("COMMERCIAL_CART");
        var breakId = anchored(fixed("TRACK", 5L, 60), 300, "HARD");
        var plan = ClockSegmentPlanner.plan(List.of(longSpot, breakId),
                start, start.plusSeconds(3600), source(180, 400, 30));
        assertThat(plan.rows().get(0).cueOutSeconds()).isEqualTo(400); // untouched
        assertThat(plan.notes()).anyMatch(n -> n.contains("Cannot hit hard boundary"));
        assertThat(plan.notes()).anyMatch(n -> n.contains("missed by 100"));
    }

    @Test
    void deadAirGapShiftsFollowingRowsWhenNoPadExists() {
        var padlessSource = new ClockSegmentPlanner.UnitSource() {
            @Override
            public ClockSegmentPlanner.Resolved resolveItem(ScheduleItem it, Instant at) {
                return ClockSegmentPlanner.Resolved.of(new ClockSegmentPlanner.Unit(
                        100L, 60, null, null, null, "Song", null, true));
            }
            @Override
            public ClockSegmentPlanner.Resolved resolvePad(Instant at) {
                return ClockSegmentPlanner.Resolved.fail(null);
            }
        };
        var track = fixed("TRACK", 1L, 60);
        var breakId = anchored(fixed("TRACK", 5L, 30), 180, "SOFT");
        var plan = ClockSegmentPlanner.plan(List.of(track, breakId),
                start, start.plusSeconds(3600), padlessSource);
        // No pads available: the anchored row still starts at its anchor (dead air noted).
        assertThat(plan.rows()).hasSize(2);
        assertThat(plan.rows().get(1).startsAt()).isEqualTo(start.plusSeconds(180));
        assertThat(plan.notes()).anyMatch(n -> n.contains("dead air"));
    }
}
