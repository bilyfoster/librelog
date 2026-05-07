package com.onelpro.librelog.schedule;

import com.onelpro.librelog.auth.AppUser;
import com.onelpro.librelog.auth.AppUserRepository;
import com.onelpro.librelog.librtime.LibreTimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Day Builder core: load/save the day's draft, acquire/extend/release the per-day lock,
 * and (in Phase 5) push to LibreTime.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {

    public static final Duration LOCK_TTL = Duration.ofMinutes(15);

    private final ScheduleDayRepository days;
    private final ScheduleItemRepository items;
    private final DayLockRepository locks;
    private final AppUserRepository users;
    private final LibreTimeService libretime;

    /**
     * Loaded day view. {@code lockedByOtherUser} is true when the day is currently
     * locked by someone other than the requester; in that case the day is read-only.
     */
    public record DayView(ScheduleDay day, List<ScheduleItem> items,
                          DayLock lock, AppUser lockHolder, boolean lockedByOtherUser) {}

    @Transactional
    public DayView load(UUID stationId, LocalDate date, UUID requestingUserId) {
        ScheduleDay day = days.findByStationIdAndDate(stationId, date)
                .orElseGet(() -> {
                    ScheduleDay d = ScheduleDay.builder()
                            .stationId(stationId).date(date).status("DRAFT").build();
                    return days.save(d);
                });
        var itemList = items.findByScheduleDayIdOrderByPositionAsc(day.getId());
        DayLock lock = activeLock(day.getId()).orElse(null);
        AppUser holder = lock != null ? users.findById(lock.getUserId()).orElse(null) : null;
        boolean lockedByOther = lock != null && !lock.getUserId().equals(requestingUserId);
        return new DayView(day, itemList, lock, holder, lockedByOther);
    }

    @Transactional
    public DayLock acquireLock(UUID dayId, UUID userId) {
        ensureDayExists(dayId);
        Instant now = Instant.now();
        var existing = activeLock(dayId).orElse(null);
        if (existing != null) {
            if (!existing.getUserId().equals(userId)) {
                throw new ConcurrencyException("Day is locked by another user until " + existing.getExpiresAt());
            }
            existing.setExpiresAt(now.plus(LOCK_TTL));
            return locks.save(existing);
        }
        DayLock l = DayLock.builder()
                .scheduleDayId(dayId)
                .userId(userId)
                .acquiredAt(now)
                .expiresAt(now.plus(LOCK_TTL))
                .build();
        return locks.save(l);
    }

    @Transactional
    public DayLock extendLock(UUID dayId, UUID userId) {
        var existing = activeLock(dayId)
                .orElseThrow(() -> new ConcurrencyException("No active lock to extend"));
        if (!existing.getUserId().equals(userId)) {
            throw new ConcurrencyException("Lock held by another user");
        }
        existing.setExpiresAt(Instant.now().plus(LOCK_TTL));
        return locks.save(existing);
    }

    @Transactional
    public void releaseLock(UUID dayId, UUID userId, boolean force) {
        var existing = locks.findById(dayId).orElse(null);
        if (existing == null) return;
        if (!force && !existing.getUserId().equals(userId)) {
            throw new ConcurrencyException("Lock held by another user");
        }
        locks.delete(existing);
    }

    public Optional<DayLock> activeLock(UUID dayId) {
        return locks.findById(dayId).filter(l -> l.getExpiresAt().isAfter(Instant.now()));
    }

    @Transactional
    public DayView save(UUID dayId, UUID userId, Long expectedVersion, List<ScheduleItem> newItems) {
        ScheduleDay day = days.findById(dayId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule day not found"));
        if ("PUSHED".equals(day.getStatus())) {
            throw new ConcurrencyException("Day already pushed and read-only. Reopen as admin to edit.");
        }
        var lock = activeLock(dayId).orElse(null);
        if (lock == null || !lock.getUserId().equals(userId)) {
            throw new ConcurrencyException("Acquire the day lock before saving");
        }
        if (expectedVersion != null && !expectedVersion.equals(day.getVersion())) {
            throw new ConcurrencyException(
                    "Stale view: version was " + expectedVersion + " but current is " + day.getVersion());
        }
        items.deleteByScheduleDayId(dayId);
        items.flush();
        int pos = 0;
        for (ScheduleItem it : newItems) {
            it.setId(null);
            it.setScheduleDayId(dayId);
            it.setPosition(pos++);
            items.save(it);
        }
        day = days.save(day);
        lock.setExpiresAt(Instant.now().plus(LOCK_TTL));
        locks.save(lock);
        return load(day.getStationId(), day.getDate(), userId);
    }

    /** Phase 5: push the day's items to LibreTime per show instance. */
    @Transactional
    public ScheduleDay push(UUID dayId, UUID userId) {
        ScheduleDay day = days.findById(dayId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule day not found"));
        if ("PUSHED".equals(day.getStatus())) {
            throw new ConcurrencyException("Day already pushed");
        }
        var lock = activeLock(dayId).orElse(null);
        if (lock == null || !lock.getUserId().equals(userId)) {
            throw new ConcurrencyException("Acquire the day lock before pushing");
        }

        var client = libretime.clientFor(day.getStationId());
        // Group items by show instance so we can clear-and-rewrite per instance.
        var byShow = new java.util.LinkedHashMap<Long, java.util.List<ScheduleItem>>();
        for (ScheduleItem it : items.findByScheduleDayIdOrderByPositionAsc(dayId)) {
            if (it.getShowInstanceId() == null || it.getLibrtimeFileId() == null) continue;
            byShow.computeIfAbsent(it.getShowInstanceId(), k -> new java.util.ArrayList<>()).add(it);
        }

        for (var e : byShow.entrySet()) {
            long instanceId = e.getKey();
            try {
                client.clearScheduleForInstance(instanceId);
                int position = 0;
                for (ScheduleItem it : e.getValue()) {
                    client.scheduleFileInInstance(instanceId, it.getLibrtimeFileId(), position++);
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Push to LibreTime failed for show instance "
                        + instanceId + ": " + ex.getMessage(), ex);
            }
        }

        day.setStatus("PUSHED");
        day.setPushedAt(Instant.now());
        day.setPushedBy(userId);
        return days.save(day);
    }

    @Transactional
    public ScheduleDay reopen(UUID dayId) {
        ScheduleDay day = days.findById(dayId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule day not found"));
        day.setStatus("DRAFT");
        day.setPushedAt(null);
        day.setPushedBy(null);
        return days.save(day);
    }

    private void ensureDayExists(UUID dayId) {
        if (!days.existsById(dayId)) throw new IllegalArgumentException("Schedule day not found");
    }

    public static class ConcurrencyException extends RuntimeException {
        public ConcurrencyException(String message) { super(message); }
    }
}
