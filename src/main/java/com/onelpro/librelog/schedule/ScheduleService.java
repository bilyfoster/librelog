package com.onelpro.librelog.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.onelpro.librelog.auth.AppUser;
import com.onelpro.librelog.auth.AppUserRepository;
import com.onelpro.librelog.carts.Cart;
import com.onelpro.librelog.carts.CartMember;
import com.onelpro.librelog.carts.CartRepository;
import com.onelpro.librelog.carts.CartService;
import com.onelpro.librelog.carts.ClockService;
import com.onelpro.librelog.carts.ClockTemplateRepository;
import com.onelpro.librelog.carts.ClockTemplateSlot;
import com.onelpro.librelog.librtime.LibreTimeClient;
import com.onelpro.librelog.librtime.LibreTimeService;
import com.onelpro.librelog.orders.Spot;
import com.onelpro.librelog.orders.SpotRepository;
import com.onelpro.librelog.rumble.service.JazzHandoffService;
import com.onelpro.librelog.station.StationRepository;
import com.onelpro.librelog.time.TimeWindowUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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

    private static final String DEFAULT_ADVERTISER_CART_PLACEHOLDER = "Advertiser cart slot";

    private final ScheduleDayRepository days;
    private final ScheduleItemRepository items;
    private final DayLockRepository locks;
    private final AppUserRepository users;
    private final LibreTimeService libretime;
    private final StationRepository stations;
    private final CartService cartService;
    private final CartRepository cartRepo;
    private final ClockService clockService;
    private final ClockTemplateRepository clockTemplates;
    private final ScheduleDayClockSegmentRepository clockSegments;
    private final SpotRepository spots;
    private final JazzHandoffService jazzHandoff;

    /**
     * Loaded day view. {@code lockedByOtherUser} is true when the day is currently
     * locked by someone other than the requester; in that case the day is read-only.
     */
    public record DayView(ScheduleDay day, List<ScheduleItem> items,
                          DayLock lock, AppUser lockHolder, boolean lockedByOtherUser,
                          List<ScheduleDayClockSegment> clockSegments) {}

    /** One row in the per-day clock schedule (station-local window → clock template). */
    public record ClockSegmentInput(int localStartMinutes, int localEndMinutes, UUID clockTemplateId) {}

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
        var segs = clockSegments.findByScheduleDayIdOrderByPositionAsc(day.getId());
        return new DayView(day, itemList, lock, holder, lockedByOther, segs);
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
            // When a cart-backed slot is saved we don't have a resolved file yet — that
            // happens at push time. Length defaults to 30s for slot math if missing.
            if (it.getLengthSeconds() == null) it.setLengthSeconds(30);
            items.save(it);
        }
        day = days.save(day);
        lock.setExpiresAt(Instant.now().plus(LOCK_TTL));
        locks.save(lock);
        return load(day.getStationId(), day.getDate(), userId);
    }

    @Transactional
    public DayView preloadFromLibreTime(UUID stationId, LocalDate date, UUID userId, boolean replaceExisting) {
        DayView current = load(stationId, date, userId);
        ScheduleDay day = current.day();
        if ("PUSHED".equals(day.getStatus())) {
            throw new ConcurrencyException("Day already pushed and read-only. Reopen as admin to edit.");
        }
        var lock = activeLock(day.getId()).orElse(null);
        if (lock == null || !lock.getUserId().equals(userId)) {
            throw new ConcurrencyException("Acquire the day lock before preloading from LibreTime");
        }
        var existing = items.findByScheduleDayIdOrderByPositionAsc(day.getId());
        if (!existing.isEmpty() && !replaceExisting) {
            throw new ConcurrencyException("This day already has slots. Use replace if you want to overwrite them from LibreTime.");
        }

        var client = libretime.clientFor(stationId);
        ZoneId stationZone = stations.findById(stationId)
                .map(s -> safeZone(s.getTimeZone())).orElse(ZoneOffset.UTC);
        var showInstances = client.listShowInstances(date, stationZone);
        var preloaded = new ArrayList<ScheduleItem>();
        int position = 0;

        for (JsonNode instance : showInstances) {
            Long instanceId = numberField(instance, "id");
            if (instanceId == null) continue;

            var scheduled = client.listScheduleForInstance(instanceId);
            scheduled.sort(Comparator
                    .comparing((JsonNode n) -> numberField(n, "position"), Comparator.nullsLast(Long::compareTo))
                    .thenComparing(n -> textField(n, "starts"), Comparator.nullsLast(String::compareTo)));

            int slotIndex = 0;
            for (JsonNode n : scheduled) {
                Long fileId = numberField(n, "file");
                if (fileId == null) fileId = numberField(n, "file_id");
                if (fileId == null) continue;

                preloaded.add(ScheduleItem.builder()
                        .scheduleDayId(day.getId())
                        .showInstanceId(instanceId)
                        .slotIndex(slotIndex++)
                        .kind("TRACK")
                        .librtimeFileId(fileId)
                        .scheduledAt(firstTime(n, "starts", "starts_at", "scheduled_at"))
                        .lengthSeconds(lengthSeconds(n))
                        .position(position++)
                        .build());
            }
        }

        if (!existing.isEmpty()) {
            items.deleteByScheduleDayId(day.getId());
            items.flush();
        }
        for (ScheduleItem it : preloaded) items.save(it);
        lock.setExpiresAt(Instant.now().plus(LOCK_TTL));
        locks.save(lock);
        days.save(day);
        return load(stationId, date, userId);
    }

    /**
     * Phase 5: push the day's items to LibreTime per show instance.
     *
     * <p>For each show instance, we clear LibreTime's existing schedule rows and POST
     * new ones. LibreTime's {@code /api/v2/schedule} requires explicit
     * {@code starts_at} / {@code ends_at} timestamps, so we walk the items in order
     * starting at the show instance's {@code starts_at} and accumulate their lengths.
     * Items whose computed start would fall past the show's end are skipped (and the
     * count is reported back) instead of failing the whole push.</p>
     */
    @Transactional
    public PushResult push(UUID dayId, UUID userId) {
        ScheduleDay day = days.findById(dayId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule day not found"));
        if ("PUSHED".equals(day.getStatus())) {
            throw new ConcurrencyException("Day already pushed");
        }
        // Playout safety rail (PRD §7): never rewrite the active or next playout day.
        // Logs must be committed at least 24 hours ahead of air time.
        jazzHandoff.assertSafeHandoffDate(day.getDate());
        var lock = activeLock(dayId).orElse(null);
        if (lock == null || !lock.getUserId().equals(userId)) {
            throw new ConcurrencyException("Acquire the day lock before pushing");
        }

        LibreTimeClient client = libretime.clientFor(day.getStationId());
        // Group items by show instance so we can clear-and-rewrite per instance.
        var byShow = new java.util.LinkedHashMap<Long, java.util.List<ScheduleItem>>();
        for (ScheduleItem it : items.findByScheduleDayIdOrderByPositionAsc(dayId)) {
            if (it.getShowInstanceId() == null) continue;
            byShow.computeIfAbsent(it.getShowInstanceId(), k -> new java.util.ArrayList<>()).add(it);
        }

        int instancesTouched = 0;
        int rowsWritten = 0;
        int rowsSkipped = 0;
        int rowsResolved = 0;
        var skipReasons = new ArrayList<String>();
        // Reuse one resolver per push so cart-rotation pointers and separation
        // history are consistent across all hours of the day.
        CartService.Resolver resolver = cartService.newResolver(day.getStationId(), Duration.ofHours(48));

        for (var e : byShow.entrySet()) {
            long instanceId = e.getKey();
            JsonNode instance;
            try { instance = client.getShowInstance(instanceId); }
            catch (Exception ex) {
                throw new IllegalStateException("Show instance " + instanceId + " could not be fetched from LibreTime: "
                        + ex.getMessage(), ex);
            }
            if (instance == null) {
                skipReasons.add("Instance " + instanceId + " not found in LibreTime; skipped " + e.getValue().size() + " row(s)");
                rowsSkipped += e.getValue().size();
                continue;
            }
            Instant instanceStart = parseInstantSafe(textField(instance, "starts_at"));
            Instant instanceEnd = parseInstantSafe(textField(instance, "ends_at"));
            Long currentShowId = numberField(instance, "show");
            if (instanceStart == null) {
                skipReasons.add("Instance " + instanceId + " has no starts_at; skipped " + e.getValue().size() + " row(s)");
                rowsSkipped += e.getValue().size();
                continue;
            }

            try {
                client.clearScheduleForInstance(instanceId);
            } catch (Exception ex) {
                throw new IllegalStateException("Could not clear existing schedule for instance "
                        + instanceId + ": " + ex.getMessage(), ex);
            }

            Instant cursor = instanceStart;
            int position = 0;
            for (ScheduleItem it : e.getValue()) {
                Long fileId = it.getLibrtimeFileId();
                int len = it.getLengthSeconds() != null ? it.getLengthSeconds() : 0;

                // Cart slots: resolve to a concrete file/spot now, using the *intended*
                // start time for separation lookups (specific cart or category pool).
                if (fileId == null && ("MUSIC_CART".equals(it.getKind()) || "COMMERCIAL_CART".equals(it.getKind()))) {
                    CartPickResult pick = pickCartMemberForItem(it, day, resolver, cursor, currentShowId);
                    if (pick.member() == null) {
                        rowsSkipped++;
                        if (pick.note() != null) {
                            skipReasons.add("Position " + it.getPosition() + ": " + pick.note());
                        } else {
                            skipReasons.add("Position " + it.getPosition() + ": no cart or category to resolve");
                        }
                        continue;
                    }
                    Cart cart = pick.cart();
                    CartMember picked = pick.member();
                    fileId = resolveFileId(picked);
                    if (fileId == null) {
                        rowsSkipped++;
                        skipReasons.add("Cart \"" + cart.getName() + "\" picked member without a LibreTime file");
                        continue;
                    }
                    if (len <= 0 && picked.getLengthSeconds() != null) len = picked.getLengthSeconds();
                    rowsResolved++;
                    if (pick.note() != null) skipReasons.add(pick.note());
                    it.setCartId(cart.getId());
                    it.setLibrtimeFileId(fileId);
                    it.setResolvedMemberId(picked.getId());
                    if (it.getLabel() == null) it.setLabel(displayLabelOf(cart, picked));
                    items.save(it);
                    // An approved spot that lands in a push is now trafficked (on air).
                    markSpotTrafficked(picked.getSpotId());
                }
                if (fileId == null) continue;
                if (len <= 0) len = 30; // fall-back so 0-length placeholders still get a row

                Instant rowStart = cursor;
                Instant rowEnd = rowStart.plusSeconds(len);
                if (instanceEnd != null && rowStart.isAfter(instanceEnd.minusSeconds(1))) {
                    rowsSkipped++;
                    skipReasons.add("Instance " + instanceId + " is full; dropped slot at position " + it.getPosition());
                    continue;
                }
                try {
                    client.scheduleFileInInstance(instanceId, fileId, position++, rowStart, rowEnd, len,
                            it.getSegueOffsetSeconds(), it.getDuckDb());
                    rowsWritten++;
                    // Persist the actual air time (and the resolved file/length) so that
                    // playback reconciliation can later match this row by file id + time.
                    // Without this, clock-built and cart/spot-resolved rows have a null
                    // scheduledAt and reconcileDay() silently skips them.
                    it.setScheduledAt(rowStart);
                    it.setLibrtimeFileId(fileId);
                    it.setLengthSeconds(len);
                    items.save(it);
                } catch (Exception ex) {
                    throw new IllegalStateException("Push to LibreTime failed for show instance "
                            + instanceId + " (file " + fileId + ", position " + (position - 1)
                            + "): " + ex.getMessage(), ex);
                }
                cursor = rowEnd;
            }
            instancesTouched++;
        }

        // Persist the resolved play history so tomorrow's push respects today's separation.
        cartService.recordHistory(resolver.getPending());

        day.setStatus("PUSHED");
        day.setPushedAt(Instant.now());
        day.setPushedBy(userId);
        days.save(day);
        return new PushResult(instancesTouched, rowsWritten, rowsResolved, rowsSkipped, skipReasons);
    }

    public record PushResult(int instancesTouched, int rowsWritten, int rowsResolved,
                             int rowsSkipped, List<String> notes) {}

    /** Materialize a clock template into slots for a single show instance. */
    @Transactional
    public DayView applyClock(UUID dayId, UUID userId, long showInstanceId, UUID clockId) {
        ScheduleDay day = days.findById(dayId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule day not found"));
        if ("PUSHED".equals(day.getStatus())) {
            throw new ConcurrencyException("Day already pushed and read-only. Reopen as admin to edit.");
        }
        var lock = activeLock(dayId).orElse(null);
        if (lock == null || !lock.getUserId().equals(userId)) {
            throw new ConcurrencyException("Acquire the day lock before applying a clock");
        }
        var clockSlots = clockService.slotsOf(clockId);
        if (clockSlots.isEmpty()) {
            throw new IllegalArgumentException("Clock has no slots");
        }

        replaceInstanceSlotsWithClock(dayId, showInstanceId, clockSlots);

        lock.setExpiresAt(Instant.now().plus(LOCK_TTL));
        locks.save(lock);
        return load(day.getStationId(), day.getDate(), userId);
    }

    /**
     * For each LibreTime show instance on this calendar day (chronological), picks the clock
     * from {@link #saveClockSchedule(UUID, UUID, List)} whose station-local window contains
     * the instance start time, and replaces that instance's draft slots with that clock's
     * template. Instances with no matching segment are left unchanged.
     */
    @Transactional
    public ApplyClockScheduleResult applyClockScheduleToShows(UUID dayId, UUID userId) {
        ScheduleDay day = days.findById(dayId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule day not found"));
        if ("PUSHED".equals(day.getStatus())) {
            throw new ConcurrencyException("Day already pushed and read-only. Reopen as admin to edit.");
        }
        var lock = activeLock(dayId).orElse(null);
        if (lock == null || !lock.getUserId().equals(userId)) {
            throw new ConcurrencyException("Acquire the day lock before applying the clock schedule");
        }
        List<ScheduleDayClockSegment> segs = clockSegments.findByScheduleDayIdOrderByPositionAsc(dayId);
        if (segs.isEmpty()) {
            throw new IllegalArgumentException("Add at least one clock schedule row (local hours → clock) before applying.");
        }

        LibreTimeClient client = libretime.clientFor(day.getStationId());
        ZoneId stationZone = stations.findById(day.getStationId())
                .map(s -> safeZone(s.getTimeZone())).orElse(ZoneOffset.UTC);
        List<JsonNode> rawInstances = client.listShowInstances(day.getDate(), stationZone);
        if (rawInstances.isEmpty()) {
            throw new IllegalArgumentException("No LibreTime show instances for " + day.getDate());
        }

        LinkedHashMap<Long, Instant> instanceStarts = new LinkedHashMap<>();
        for (JsonNode n : rawInstances) {
            Long id = numberField(n, "id");
            if (id == null) continue;
            Instant t = parseInstantSafe(textField(n, "starts_at"));
            instanceStarts.putIfAbsent(id, t);
        }
        if (instanceStarts.isEmpty()) {
            throw new IllegalArgumentException("No valid show instance ids returned for " + day.getDate());
        }

        List<Long> sortedIds = instanceStarts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Map.Entry::getKey)
                .toList();

        int updated = 0;
        for (long instanceId : sortedIds) {
            Instant t = instanceStarts.get(instanceId);
            if (t == null) {
                continue;
            }
            int md = TimeWindowUtil.minuteOfDay(stationZone, t);
            ScheduleDayClockSegment seg = firstSegmentContaining(segs, md);
            if (seg == null) {
                continue;
            }
            var clockSlots = clockService.slotsOf(seg.getClockTemplateId());
            if (clockSlots.isEmpty()) {
                continue;
            }
            replaceInstanceSlotsWithClock(dayId, instanceId, clockSlots);
            updated++;
        }

        lock.setExpiresAt(Instant.now().plus(LOCK_TTL));
        locks.save(lock);
        DayView view = load(day.getStationId(), day.getDate(), userId);
        return new ApplyClockScheduleResult(view, updated);
    }

    public record ApplyClockScheduleResult(DayView dayView, int instancesUpdated) {}

    @Transactional
    public DayView saveClockSchedule(UUID dayId, UUID userId, List<ClockSegmentInput> rows) {
        ScheduleDay day = days.findById(dayId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule day not found"));
        if ("PUSHED".equals(day.getStatus())) {
            throw new ConcurrencyException("Day already pushed and read-only. Reopen as admin to edit.");
        }
        var lock = activeLock(dayId).orElse(null);
        if (lock == null || !lock.getUserId().equals(userId)) {
            throw new ConcurrencyException("Acquire the day lock before editing the clock schedule");
        }
        UUID stationId = day.getStationId();
        clockSegments.deleteByScheduleDayId(dayId);
        clockSegments.flush();
        int pos = 0;
        for (ClockSegmentInput row : rows) {
            if (row.clockTemplateId() == null) {
                throw new IllegalArgumentException("Each schedule row needs a clock");
            }
            var tpl = clockTemplates.findById(row.clockTemplateId())
                    .orElseThrow(() -> new IllegalArgumentException("Clock not found: " + row.clockTemplateId()));
            if (!tpl.getStationId().equals(stationId)) {
                throw new IllegalArgumentException("Clock \"" + tpl.getName() + "\" does not belong to this station");
            }
            TimeWindowUtil.validateWindowPair(row.localStartMinutes(), row.localEndMinutes(),
                    "Clock schedule row " + (pos + 1));
            int endNorm = TimeWindowUtil.normalizeExclusiveEnd(row.localEndMinutes());
            ScheduleDayClockSegment seg = ScheduleDayClockSegment.builder()
                    .scheduleDayId(dayId)
                    .position(pos++)
                    .localStartMinutes(row.localStartMinutes())
                    .localEndMinutes(endNorm)
                    .clockTemplateId(row.clockTemplateId())
                    .build();
            clockSegments.save(seg);
        }
        lock.setExpiresAt(Instant.now().plus(LOCK_TTL));
        locks.save(lock);
        return load(stationId, day.getDate(), userId);
    }

    private static ScheduleDayClockSegment firstSegmentContaining(List<ScheduleDayClockSegment> segs, int minuteOfDay) {
        for (ScheduleDayClockSegment s : segs) {
            if (TimeWindowUtil.isMinuteWithin(s.getLocalStartMinutes(), s.getLocalEndMinutes(), minuteOfDay)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Replace all schedule rows for {@code showInstanceId} with slots materialized from
     * {@code clockSlots}; preserve items belonging to other instances.
     */
    private void replaceInstanceSlotsWithClock(UUID dayId, long showInstanceId,
                                               List<ClockTemplateSlot> clockSlots) {
        ScheduleDay day = days.findById(dayId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule day not found"));
        var existing = items.findByScheduleDayIdOrderByPositionAsc(dayId);
        var keep = new ArrayList<ScheduleItem>();
        for (ScheduleItem it : existing) {
            if (it.getShowInstanceId() == null || it.getShowInstanceId() != showInstanceId) {
                keep.add(it);
            }
        }
        items.deleteByScheduleDayId(dayId);
        items.flush();

        int position = 0;
        for (ScheduleItem it : keep) {
            it.setId(null);
            it.setPosition(position++);
            items.save(it);
        }
        int slotIndex = 0;
        for (ClockTemplateSlot s : clockSlots) {
            ScheduleItem it = clockSlotToScheduleItem(s, day.getId(), showInstanceId, slotIndex++, position++);
            if (it != null) items.save(it);
        }
    }

    private record CartPickResult(Cart cart, CartMember member, String note) {}

    /**
     * Resolves a specific {@link ScheduleItem#getCartId()} or the first cart in
     * {@link ScheduleItem#getCartCategory()} (name order) that yields an eligible member
     * at {@code slotTime} (spot day parts / local windows apply inside {@link CartService.Resolver}).
     */
    private CartPickResult pickCartMemberForItem(ScheduleItem it, ScheduleDay day,
                                                 CartService.Resolver resolver, Instant slotTime,
                                                 Long currentShowId) {
        if (!"MUSIC_CART".equals(it.getKind()) && !"COMMERCIAL_CART".equals(it.getKind())) {
            return new CartPickResult(null, null, null);
        }
        if (it.getCartId() != null) {
            Cart cart = cartRepo.findById(it.getCartId()).orElse(null);
            if (cart == null) {
                return new CartPickResult(null, null, "Referenced cart was not found");
            }
            var res = resolver.resolve(cart, slotTime, currentShowId);
            return new CartPickResult(cart, res.member(), res.note());
        }
        if (it.getCartCategory() != null && !it.getCartCategory().isBlank()) {
            String ck = "MUSIC_CART".equals(it.getKind()) ? "MUSIC" : "COMMERCIAL";
            List<Cart> candidates = cartRepo.findByStationIdAndKindAndCategoryOrderByNameAsc(
                    day.getStationId(), ck, it.getCartCategory());
            for (Cart c : candidates) {
                var res = resolver.resolve(c, slotTime, currentShowId);
                if (res.member() != null) {
                    return new CartPickResult(c, res.member(), res.note());
                }
            }
            return new CartPickResult(null, null,
                    "No cart in category \"" + it.getCartCategory() + "\" had an eligible member at this time");
        }
        return new CartPickResult(null, null, null);
    }

    /** Advance an APPROVED spot to TRAFFICKED once it has been pushed into a schedule. */
    private void markSpotTrafficked(UUID spotId) {
        if (spotId == null) return;
        spots.findById(spotId).ifPresent(sp -> {
            if (Spot.STATUS_APPROVED.equals(sp.getStatus())) {
                sp.setStatus(Spot.STATUS_TRAFFICKED);
                spots.save(sp);
            }
        });
    }

    private ScheduleItem clockSlotToScheduleItem(ClockTemplateSlot s, UUID dayId, long instanceId,
                                                 int slotIndex, int position) {
        switch (s.getKind()) {
            case "MUSIC_CART", "COMMERCIAL_CART" -> {
                if (s.getCartCategory() != null && !s.getCartCategory().isBlank()) {
                    int len = s.getDefaultLengthSeconds() != null ? s.getDefaultLengthSeconds()
                            : ("COMMERCIAL_CART".equals(s.getKind()) ? 30 : 180);
                    return ScheduleItem.builder()
                            .scheduleDayId(dayId).showInstanceId(instanceId).slotIndex(slotIndex)
                            .kind(s.getKind())
                            .cartCategory(s.getCartCategory())
                            .lengthSeconds(len)
                            .label(s.getLabel() != null ? s.getLabel() : ("Any " + s.getCartCategory() + " cart"))
                            .position(position).build();
                }
                if (s.getCartId() == null) {
                    if ("COMMERCIAL_CART".equals(s.getKind())) {
                        String lab = s.getLabel();
                        if (lab == null || lab.isBlank()) {
                            lab = DEFAULT_ADVERTISER_CART_PLACEHOLDER;
                        }
                        return ScheduleItem.builder()
                                .scheduleDayId(dayId).showInstanceId(instanceId).slotIndex(slotIndex)
                                .kind("PLACEHOLDER")
                                .lengthSeconds(s.getDefaultLengthSeconds() != null ? s.getDefaultLengthSeconds() : 30)
                                .label(lab)
                                .position(position).build();
                    }
                    return null;
                }
                Cart cart = cartRepo.findById(s.getCartId()).orElse(null);
                if (cart == null) return null;
                int len = s.getDefaultLengthSeconds() != null ? s.getDefaultLengthSeconds()
                        : ("COMMERCIAL_CART".equals(s.getKind()) ? 30 : 180);
                return ScheduleItem.builder()
                        .scheduleDayId(dayId).showInstanceId(instanceId).slotIndex(slotIndex)
                        .kind(s.getKind()).cartId(cart.getId()).lengthSeconds(len)
                        .label(s.getLabel() != null ? s.getLabel() : cart.getName())
                        .position(position).build();
            }
            case "VOICETRACK" -> {
                // Empty voice-track slot: no file until a take is recorded (Phase 4),
                // at which point librtimeFileId is set and push treats it like a track.
                return ScheduleItem.builder()
                        .scheduleDayId(dayId).showInstanceId(instanceId).slotIndex(slotIndex)
                        .kind("VOICETRACK")
                        .lengthSeconds(s.getDefaultLengthSeconds() != null ? s.getDefaultLengthSeconds() : 30)
                        .label(s.getLabel() != null ? s.getLabel() : "Voice track")
                        .position(position).build();
            }
            case "TRACK" -> {
                if (s.getLibrtimeFileId() == null) return null;
                return ScheduleItem.builder()
                        .scheduleDayId(dayId).showInstanceId(instanceId).slotIndex(slotIndex)
                        .kind("TRACK").librtimeFileId(s.getLibrtimeFileId())
                        .lengthSeconds(s.getDefaultLengthSeconds() != null ? s.getDefaultLengthSeconds() : 180)
                        .label(s.getLabel()).position(position).build();
            }
            case "SPOT" -> {
                if (s.getSpotId() == null) return null;
                Spot spot = spots.findById(s.getSpotId()).orElse(null);
                int len = spot != null ? spot.getLengthSeconds()
                        : (s.getDefaultLengthSeconds() != null ? s.getDefaultLengthSeconds() : 30);
                return ScheduleItem.builder()
                        .scheduleDayId(dayId).showInstanceId(instanceId).slotIndex(slotIndex)
                        .kind("SPOT").spotId(s.getSpotId())
                        .librtimeFileId(spot != null ? spot.getLibrtimeFileId() : null)
                        .lengthSeconds(len)
                        .label(s.getLabel() != null ? s.getLabel() : (spot != null ? spot.getLabel() : null))
                        .position(position).build();
            }
            default -> {
                return ScheduleItem.builder()
                        .scheduleDayId(dayId).showInstanceId(instanceId).slotIndex(slotIndex)
                        .kind("PLACEHOLDER")
                        .lengthSeconds(s.getDefaultLengthSeconds() != null ? s.getDefaultLengthSeconds() : 30)
                        .label(s.getLabel())
                        .position(position).build();
            }
        }
    }

    /** Dry-run resolution without writing to LibreTime; useful for "preview" UX. */
    public List<PreviewItem> previewResolution(UUID dayId) {
        var preview = new ArrayList<PreviewItem>();
        ScheduleDay day = days.findById(dayId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule day not found"));
        CartService.Resolver resolver = cartService.newResolver(day.getStationId(), Duration.ofHours(48));
        for (ScheduleItem it : items.findByScheduleDayIdOrderByPositionAsc(dayId)) {
            if ("MUSIC_CART".equals(it.getKind()) || "COMMERCIAL_CART".equals(it.getKind())) {
                CartPickResult pick = pickCartMemberForItem(it, day, resolver, Instant.now(), null);
                if (pick.member() != null) {
                    Cart cart = pick.cart();
                    CartMember picked = pick.member();
                    preview.add(new PreviewItem(it.getId().toString(), it.getKind(),
                            cart.getName(),
                            resolveFileId(picked),
                            displayLabelOf(cart, picked),
                            pick.note()));
                } else {
                    String pool = it.getCartCategory() != null
                            ? ("category " + it.getCartCategory())
                            : (it.getCartId() != null ? "cart" : "slot");
                    preview.add(new PreviewItem(it.getId().toString(), it.getKind(), null,
                            it.getLibrtimeFileId(), it.getLabel(),
                            pick.note() != null ? pick.note() : ("Could not resolve " + pool)));
                }
                continue;
            }
            if (it.getCartId() == null) {
                preview.add(new PreviewItem(it.getId().toString(), it.getKind(), null,
                        it.getLibrtimeFileId(), it.getLabel(), null));
                continue;
            }
            Cart cart = cartRepo.findById(it.getCartId()).orElse(null);
            if (cart == null) {
                preview.add(new PreviewItem(it.getId().toString(), it.getKind(),
                        null, null, "(missing cart)", "Cart missing"));
                continue;
            }
            // Use 'now' as the time anchor — preview is purely indicative.
            var res = resolver.resolve(cart, Instant.now());
            CartMember picked = res.member();
            preview.add(new PreviewItem(it.getId().toString(), it.getKind(),
                    cart.getName(),
                    picked == null ? null : resolveFileId(picked),
                    picked == null ? null : displayLabelOf(cart, picked),
                    res.note()));
        }
        return preview;
    }

    public record PreviewItem(String scheduleItemId, String kind, String cartName,
                              Long librtimeFileId, String label, String note, boolean violation) {
        PreviewItem(String scheduleItemId, String kind, String cartName,
                    Long librtimeFileId, String label, String note) {
            this(scheduleItemId, kind, cartName, librtimeFileId, label, note, isViolation(note));
        }

        private static boolean isViolation(String note) {
            return note != null && (note.contains("Separation violated") || note.contains("clutter"));
        }
    }

    private static Long resolveFileId(CartMember m) {
        return m.getLibrtimeFileId();
    }

    private static String displayLabelOf(Cart cart, CartMember m) {
        if ("MUSIC".equals(cart.getKind())) {
            String s = joinNonBlank(" — ", m.getArtist(), m.getTitle());
            return s.isBlank() ? ("Music #" + m.getLibrtimeFileId()) : s;
        }
        return m.getSponsor() != null ? m.getSponsor() : ("Spot " + m.getSpotId());
    }

    private static String joinNonBlank(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }

    private static ZoneId safeZone(String tz) {
        if (tz == null || tz.isBlank()) return ZoneOffset.UTC;
        try { return ZoneId.of(tz); } catch (Exception e) { return ZoneOffset.UTC; }
    }

    private static Instant parseInstantSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Instant.parse(s); } catch (Exception ignored) {}
        try { return ZonedDateTime.parse(s).toInstant(); } catch (Exception ignored) {}
        try { return LocalDateTime.parse(s).toInstant(ZoneOffset.UTC); } catch (Exception ignored) {}
        return null;
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

    private static Long numberField(JsonNode n, String field) {
        if (n == null || !n.has(field) || n.get(field).isNull()) return null;
        JsonNode v = n.get(field);
        if (v.isNumber()) return v.asLong();
        if (v.isObject() && v.has("id") && v.get("id").isNumber()) return v.get("id").asLong();
        if (v.isTextual()) {
            try { return Long.parseLong(v.asText()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static String textField(JsonNode n, String field) {
        return n != null && n.has(field) && !n.get(field).isNull() ? n.get(field).asText() : null;
    }

    private static Instant firstTime(JsonNode n, String... fields) {
        for (String field : fields) {
            String value = textField(n, field);
            if (value == null || value.isBlank()) continue;
            try { return Instant.parse(value); } catch (Exception ignored) {}
            try { return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC); } catch (Exception ignored) {}
            try { return ZonedDateTime.parse(value).toInstant(); } catch (Exception ignored) {}
        }
        return null;
    }

    private static Integer lengthSeconds(JsonNode n) {
        Long direct = numberField(n, "length_seconds");
        if (direct == null) direct = numberField(n, "duration_seconds");
        if (direct != null) return direct.intValue();
        String length = textField(n, "length");
        if (length == null && n != null && n.has("file") && n.get("file").isObject()) {
            length = textField(n.get("file"), "length");
        }
        if (length == null) return null;
        var m = java.util.regex.Pattern.compile("^(\\d+):(\\d+):(\\d+)").matcher(length);
        if (m.find()) {
            return Integer.parseInt(m.group(1)) * 3600
                    + Integer.parseInt(m.group(2)) * 60
                    + Integer.parseInt(m.group(3));
        }
        try { return Integer.parseInt(length); } catch (NumberFormatException ignored) {}
        return null;
    }

    public static class ConcurrencyException extends RuntimeException {
        public ConcurrencyException(String message) { super(message); }
    }
}
