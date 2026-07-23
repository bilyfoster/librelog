package com.onelpro.librelog.schedule;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hot-clock timing engine: turns one show instance's schedule items into a list of
 * planned rows with exact start times, honoring anchors (fixed offsets like "break A
 * at 18:00"), avail caps (max units and/or max seconds per fill group), TO_END music
 * fills, pad insertion for underruns, and cue-out trimming for hard anchors and
 * top-of-hour back-timing.
 *
 * <p>Pure planning: resolution side effects (rotation pointers, play history) live in
 * the supplied {@link UnitSource}; the planner itself only does timing math, which
 * makes it directly unit-testable with scripted sources.</p>
 */
public final class ClockSegmentPlanner {

    /** Never cut a music/pad unit below this many seconds. */
    static final int MIN_TRIM_SECONDS = 20;
    /** Gaps smaller than this start the next element early instead of padding. */
    static final int SMALL_GAP_SECONDS = 5;
    /** An avail unit may exceed the group's seconds cap by up to this much. */
    static final int AVAIL_GRACE_SECONDS = 15;
    /** Underruns at the instance end larger than this are flagged, not padded. */
    static final int MAX_END_PAD_SECONDS = 90;
    /** Safety cap on units one fill/pad loop may produce. */
    static final int MAX_UNITS_PER_FILL = 60;

    private ClockSegmentPlanner() {}

    /** One resolved piece of audio the planner can place. */
    public record Unit(Long fileId, int lengthSeconds, UUID cartId, UUID memberId,
                       UUID spotId, String label, String note, boolean trimmable) {}

    /** Supplies resolved audio on demand — production wraps the cart resolver. */
    public interface UnitSource {
        /** Resolve one unit for a cart-backed item airing at {@code at}; null unit = nothing eligible. */
        Resolved resolveItem(ScheduleItem item, Instant at);
        /** Resolve one pad/sweeper unit, or null unit when no pad source is available. */
        Resolved resolvePad(Instant at);
    }

    public record Resolved(Unit unit, String failureNote) {
        public static Resolved fail(String note) { return new Resolved(null, note); }
        public static Resolved of(Unit u) { return new Resolved(u, null); }
    }

    /**
     * A planned schedule row; {@code cueOutSeconds < unit.lengthSeconds} means trimmed.
     * {@code isFillUnit} rows come from a TO_END marker: {@code item} is the marker
     * itself (for bookkeeping) but the marker must not receive the unit's file id.
     */
    public record PlannedRow(ScheduleItem item, Unit unit, Instant startsAt, int cueOutSeconds,
                             Integer segueOffsetSeconds, BigDecimal duckDb,
                             boolean isPad, boolean isFillUnit) {
        public Instant endsAt() { return startsAt.plusSeconds(cueOutSeconds); }
        public boolean trimmed() { return cueOutSeconds < unit.lengthSeconds(); }
    }

    public record Plan(List<PlannedRow> rows, List<String> notes, int skipped, int resolved) {}

    // ---- internal mutable structures -------------------------------------------------

    private static final class RowBuf {
        ScheduleItem item;      // null for pads; the TO_END marker for its fill units
        Unit unit;
        int cueOut;
        Integer segue;
        BigDecimal duck;
        boolean isPad;
        boolean isFillUnit;     // produced by a TO_END marker (item = the marker)
        int gapBefore;          // unpadded dead-air seconds preceding this row
        RowBuf(ScheduleItem item, Unit unit, Integer segue, BigDecimal duck, boolean isPad) {
            this.item = item; this.unit = unit; this.cueOut = Math.max(1, unit.lengthSeconds());
            this.segue = segue; this.duck = duck; this.isPad = isPad;
        }
    }

    private static final class Segment {
        Integer anchorOffset;   // null for the first (starts at instance start)
        boolean hard;
        final List<ScheduleItem> items = new ArrayList<>();
    }

    // ---- entry point -----------------------------------------------------------------

    public static Plan plan(List<ScheduleItem> items, Instant instanceStart, Instant instanceEnd,
                            UnitSource source) {
        List<String> notes = new ArrayList<>();
        int[] skipped = {0};
        int[] resolved = {0};
        Map<UUID, Integer> groupSeconds = new HashMap<>();

        // Partition into segments at anchored items (the anchored item leads its segment).
        List<Segment> segments = new ArrayList<>();
        Segment cur = new Segment();
        segments.add(cur);
        for (ScheduleItem it : items) {
            if (it.getAnchorOffsetSeconds() != null && !cur.items.isEmpty()) {
                cur = new Segment();
                segments.add(cur);
            }
            if (it.getAnchorOffsetSeconds() != null && cur.anchorOffset == null && cur.items.isEmpty()) {
                cur.anchorOffset = it.getAnchorOffsetSeconds();
                cur.hard = "HARD".equalsIgnoreCase(String.valueOf(it.getAnchorPolicy()));
            }
            cur.items.add(it);
        }

        List<RowBuf> all = new ArrayList<>();
        long cursor = 0; // seconds from instance start
        Long instanceLen = instanceEnd == null ? null
                : Duration.between(instanceStart, instanceEnd).getSeconds();

        for (int si = 0; si < segments.size(); si++) {
            Segment seg = segments.get(si);
            // Budget boundary: next segment's anchor, else instance end, else unbounded.
            Long boundary = null;
            boolean boundaryHard = false;
            if (si + 1 < segments.size() && segments.get(si + 1).anchorOffset != null) {
                boundary = segments.get(si + 1).anchorOffset.longValue();
                boundaryHard = segments.get(si + 1).hard;
            } else if (instanceLen != null) {
                boundary = instanceLen;
                boundaryHard = true; // the top of the hour is always a hard stop
            }

            // Align to this segment's own anchor first.
            long pendingGap = 0;
            if (seg.anchorOffset != null) {
                long target = seg.anchorOffset;
                if (cursor < target) {
                    long gap = target - cursor;
                    if (gap <= SMALL_GAP_SECONDS) {
                        // start early; negligible
                    } else {
                        long padded = padGap(all, gap, instanceStart.plusSeconds(cursor), source, notes);
                        if (padded < gap) {
                            pendingGap = gap - padded; // unpadded dead air shifts the next row
                            notes.add("No pad audio for " + pendingGap + "s before anchor "
                                    + mmss(target) + " — dead air likely");
                        }
                        cursor = target;
                    }
                } else if (cursor > target) {
                    notes.add((seg.hard ? "HARD" : "Soft") + " anchor " + mmss(target)
                            + " missed by " + (cursor - target) + "s — running late");
                }
            }

            // Place the segment's items.
            List<RowBuf> segRows = new ArrayList<>();
            ScheduleItem toEndMarker = null;
            for (ScheduleItem it : seg.items) {
                if ("TO_END".equals(it.getFillMode())) {
                    toEndMarker = it;
                    continue; // resolved during fitting below
                }
                Instant at = instanceStart.plusSeconds(cursor + sumSeconds(segRows));
                RowBuf row = rowForItem(it, at, source, notes, skipped, resolved, groupSeconds);
                if (row != null) segRows.add(row);
            }
            if (toEndMarker != null && boundary == null) {
                notes.add("Fill-to-end at position " + toEndMarker.getPosition()
                        + " skipped: instance has no end time");
            }

            long used = sumSeconds(segRows);

            // Extendable TO_END fill: consume the leftover budget with music, trimming
            // the final unit so the boundary lands exactly.
            if (toEndMarker != null && boundary != null) {
                long leftover = boundary - (cursor + used);
                int units = 0;
                while (leftover > 0 && units < MAX_UNITS_PER_FILL) {
                    Instant at = instanceStart.plusSeconds(cursor + sumSeconds(segRows));
                    Resolved r = source.resolveItem(toEndMarker, at);
                    if (r.unit() == null) {
                        if (r.failureNote() != null) notes.add("Fill stopped: " + r.failureNote());
                        break;
                    }
                    RowBuf row = new RowBuf(toEndMarker, r.unit(), null, null, false);
                    row.isFillUnit = true;
                    if (row.cueOut > leftover) {
                        long trimmedTo = leftover;
                        if (trimmedTo >= MIN_TRIM_SECONDS) {
                            row.cueOut = (int) trimmedTo;
                            segRows.add(row);
                            resolved[0]++;
                            notes.add("Back-timed: trimmed \"" + safeLabel(r.unit())
                                    + "\" to " + mmss(trimmedTo) + " to hit " + mmss(boundary));
                            leftover = 0;
                        } else {
                            // Too short to be worth a song fragment — pad the tail instead.
                            long padded = padGap(segRows, leftover, at, source, notes);
                            if (padded < leftover) {
                                notes.add("Hour under by " + (leftover - padded) + "s at "
                                        + mmss(boundary) + " (no pad audio)");
                            }
                            leftover = 0;
                        }
                    } else {
                        segRows.add(row);
                        resolved[0]++;
                        leftover -= row.cueOut;
                        units++;
                    }
                }
                used = sumSeconds(segRows);
            }

            long end = cursor + used;

            if (boundary != null && end > boundary) {
                long over = end - boundary;
                if (boundaryHard) {
                    over = trimFromEnd(segRows, over, notes, boundary);
                    if (over > 0) {
                        notes.add("Cannot hit hard boundary " + mmss(boundary) + " — over by "
                                + over + "s (nothing left to trim)");
                    }
                } else {
                    notes.add("Running " + over + "s past soft anchor " + mmss(boundary));
                }
                used = sumSeconds(segRows);
                end = cursor + used;
            }

            // Underrun with no fill: pad only near the very end of the instance, and only
            // modest amounts — a badly under-filled hour is a clock problem, not a pad job.
            if (toEndMarker == null && boundary != null && boundaryHard
                    && si == segments.size() - 1) {
                long leftover = boundary - end;
                if (leftover > SMALL_GAP_SECONDS && leftover <= MAX_END_PAD_SECONDS) {
                    long padded = padGap(segRows, leftover,
                            instanceStart.plusSeconds(end), source, notes);
                    if (padded > 0) {
                        used = sumSeconds(segRows);
                        end = cursor + used;
                    }
                } else if (leftover > MAX_END_PAD_SECONDS) {
                    notes.add("Hour under-filled by " + mmss(leftover)
                            + " — add a fill or more content to this clock");
                }
            }

            if (pendingGap > 0 && !segRows.isEmpty()) {
                segRows.get(0).gapBefore = (int) pendingGap;
            }
            all.addAll(segRows);
            cursor = end;
        }

        // Materialize instants sequentially; gapBefore realizes unpadded anchor jumps.
        List<PlannedRow> rows = new ArrayList<>(all.size());
        Instant t = instanceStart;
        for (RowBuf rb : all) {
            t = t.plusSeconds(rb.gapBefore);
            rows.add(new PlannedRow(rb.item, rb.unit, t, rb.cueOut, rb.segue, rb.duck,
                    rb.isPad, rb.isFillUnit));
            t = t.plusSeconds(rb.cueOut);
        }
        return new Plan(rows, notes, skipped[0], resolved[0]);
    }

    // ---- helpers ---------------------------------------------------------------------

    private static RowBuf rowForItem(ScheduleItem it, Instant at, UnitSource source,
                                     List<String> notes, int[] skipped, int[] resolved,
                                     Map<UUID, Integer> groupSeconds) {
        boolean cartish = "MUSIC_CART".equals(it.getKind()) || "COMMERCIAL_CART".equals(it.getKind());
        if (it.getLibrtimeFileId() != null) {
            int len = it.getLengthSeconds() != null && it.getLengthSeconds() > 0 ? it.getLengthSeconds() : 30;
            boolean trimmable = "TRACK".equals(it.getKind()) || "MUSIC_CART".equals(it.getKind());
            return new RowBuf(it, new Unit(it.getLibrtimeFileId(), len, it.getCartId(),
                    it.getResolvedMemberId(), it.getSpotId(), it.getLabel(), null, trimmable),
                    it.getSegueOffsetSeconds(), it.getDuckDb(), false);
        }
        if (!cartish) {
            // VOICETRACK without a take, PLACEHOLDER, NOTE: occupies no air time at push.
            return null;
        }
        // Avail cap pre-check using the unit's nominal length.
        Integer cap = it.getFillGroup() != null ? it.getFillTargetSeconds() : null;
        if (cap != null) {
            int already = groupSeconds.getOrDefault(it.getFillGroup(), 0);
            if (already >= cap) {
                notes.add("Avail full (" + mmss(cap) + " cap) — dropped \""
                        + String.valueOf(it.getLabel()) + "\"");
                skipped[0]++;
                return null;
            }
        }
        Resolved r = source.resolveItem(it, at);
        if (r.unit() == null) {
            skipped[0]++;
            notes.add("Position " + it.getPosition() + ": "
                    + (r.failureNote() != null ? r.failureNote() : "no cart or category to resolve"));
            return null;
        }
        if (r.unit().note() != null) notes.add(r.unit().note());
        if (cap != null) {
            int already = groupSeconds.getOrDefault(it.getFillGroup(), 0);
            int after = already + r.unit().lengthSeconds();
            if (after > cap + AVAIL_GRACE_SECONDS) {
                notes.add("Avail cap " + mmss(cap) + " would be exceeded ("
                        + mmss(after) + ") — dropped \"" + safeLabel(r.unit()) + "\"");
                skipped[0]++;
                return null;
            }
            groupSeconds.put(it.getFillGroup(), after);
        }
        resolved[0]++;
        return new RowBuf(it, r.unit(), it.getSegueOffsetSeconds(), it.getDuckDb(), false);
    }

    /** Append pad rows summing up to {@code gap} seconds (last one trimmed). Returns seconds covered. */
    private static long padGap(List<RowBuf> out, long gap, Instant at, UnitSource source,
                               List<String> notes) {
        long covered = 0;
        int units = 0;
        while (covered < gap && units < MAX_UNITS_PER_FILL) {
            Resolved r = source.resolvePad(at.plusSeconds(covered));
            if (r == null || r.unit() == null) break;
            RowBuf row = new RowBuf(null, r.unit(), null, null, true);
            long remaining = gap - covered;
            if (row.cueOut > remaining) {
                if (remaining < 2) break; // not worth a sliver
                row.cueOut = (int) remaining;
            }
            out.add(row);
            covered += row.cueOut;
            units++;
        }
        if (covered > 0) notes.add("Padded " + covered + "s with sweeper/imaging");
        return covered;
    }

    /** Trim/drop from the segment's end until {@code over} is consumed. Returns leftover overage. */
    private static long trimFromEnd(List<RowBuf> segRows, long over, List<String> notes, long boundary) {
        for (int i = segRows.size() - 1; i >= 0 && over > 0; i--) {
            RowBuf rb = segRows.get(i);
            if (rb.isPad) {
                // Pads absorb fully: shrink or drop.
                long cut = Math.min(over, rb.cueOut);
                rb.cueOut -= (int) cut;
                over -= cut;
                if (rb.cueOut < 2) segRows.remove(i);
                continue;
            }
            if (rb.unit.trimmable()) {
                int maxCut = rb.cueOut - MIN_TRIM_SECONDS;
                if (maxCut > 0) {
                    long cut = Math.min(over, maxCut);
                    rb.cueOut -= (int) cut;
                    over -= cut;
                    notes.add("Back-timed: trimmed \"" + safeLabel(rb.unit) + "\" by " + cut
                            + "s to hit " + mmss(boundary));
                }
                // A whole trimmable fill unit can also be dropped if trimming wasn't enough.
                if (over >= rb.cueOut && (rb.isFillUnit || rb.item == null)) {
                    over -= rb.cueOut;
                    notes.add("Dropped fill unit \"" + safeLabel(rb.unit) + "\" to hit " + mmss(boundary));
                    segRows.remove(i);
                }
            }
            // Untrimmable content (spots, interviews, fixed non-music) is never cut.
        }
        return over;
    }

    private static long sumSeconds(List<RowBuf> rows) {
        long s = 0;
        for (RowBuf r : rows) s += r.cueOut;
        return s;
    }

    private static String safeLabel(Unit u) {
        return u.label() != null ? u.label() : ("file " + u.fileId());
    }

    static String mmss(long seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}
