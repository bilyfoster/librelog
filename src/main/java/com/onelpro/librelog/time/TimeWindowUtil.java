package com.onelpro.librelog.time;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Station-local minute-of-day windows for clock slots and commercial spots.
 * Windows use half-open ranges {@code [start, end)} in minutes from local midnight.
 * {@code start} is 0–1439 inclusive; {@code end} is 1–1440 exclusive (1440 = end at next midnight).
 * When {@code start > end} (after clamping), the window wraps past midnight (e.g. 22:00–06:00).
 */
public final class TimeWindowUtil {

    private TimeWindowUtil() {}

    /**
     * UI {@code <input type="time">} often uses {@code 00:00} for "midnight / end of day", which parses as 0.
     * In half-open form that means {@code [start, 1440)} through the end of the calendar day.
     */
    public static int normalizeExclusiveEnd(int endExclusive) {
        return endExclusive == 0 ? 1440 : endExclusive;
    }

    /** @return null if {@code endExclusive} is null */
    public static Integer normalizeExclusiveEnd(Integer endExclusive) {
        if (endExclusive == null) {
            return null;
        }
        return normalizeExclusiveEnd(endExclusive.intValue());
    }

    public static int minuteOfDay(ZoneId zone, Instant instant) {
        var z = instant.atZone(zone);
        return z.getHour() * 60 + z.getMinute();
    }

    public static boolean isMinuteWithin(Integer startInclusive, Integer endExclusive, int minuteOfDay) {
        if (startInclusive == null || endExclusive == null) {
            return true;
        }
        int m = Math.floorMod(minuteOfDay, 1440);
        int s = Math.min(1439, Math.max(0, startInclusive));
        int e = normalizeExclusiveEnd(endExclusive);
        if (e < 1 || e > 1440) {
            return true;
        }
        if (e == 1440) {
            return m >= s;
        }
        if (s < e) {
            return m >= s && m < e;
        }
        if (s > e) {
            return m >= s || m < e;
        }
        return false;
    }

    public static boolean isInstantWithin(Integer startInclusive, Integer endExclusive,
                                          ZoneId zone, Instant instant) {
        if (startInclusive == null || endExclusive == null) {
            return true;
        }
        return isMinuteWithin(startInclusive, endExclusive, minuteOfDay(zone, instant));
    }

    public static boolean hasWindow(Integer startInclusive, Integer endExclusive) {
        return startInclusive != null && endExclusive != null;
    }

    /**
     * @throws IllegalArgumentException if one of the pair is set without the other or values are out of range
     */
    public static void validateWindowPair(Integer startInclusive, Integer endExclusive, String context) {
        if (startInclusive == null && endExclusive == null) {
            return;
        }
        if (startInclusive == null || endExclusive == null) {
            throw new IllegalArgumentException(context + ": set both start and end local minutes, or leave both unset");
        }
        int e = normalizeExclusiveEnd(endExclusive);
        if (startInclusive < 0 || startInclusive > 1439) {
            throw new IllegalArgumentException(context + ": start minutes must be 0–1439");
        }
        if (e < 1 || e > 1440) {
            throw new IllegalArgumentException(context + ": end minutes must be 1–1440 (exclusive end of day; 00:00 = midnight = end of day)");
        }
        if (startInclusive < e) {
            return;
        }
        if (startInclusive > e) {
            return;
        }
        throw new IllegalArgumentException(context + ": empty time window");
    }
}
