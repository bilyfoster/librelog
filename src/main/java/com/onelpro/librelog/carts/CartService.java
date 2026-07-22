package com.onelpro.librelog.carts;

import com.fasterxml.jackson.databind.JsonNode;
import com.onelpro.librelog.librtime.LibreTimeClient;
import com.onelpro.librelog.librtime.LibreTimeService;
import com.onelpro.librelog.orders.Spot;
import com.onelpro.librelog.orders.SpotRepository;
import com.onelpro.librelog.rumble.service.RotationSchedulerEngine;
import com.onelpro.librelog.station.DayPart;
import com.onelpro.librelog.station.DayPartRepository;
import com.onelpro.librelog.station.StationRepository;
import com.onelpro.librelog.time.TimeWindowUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Carts: rotating bins of music files (MUSIC) or commercial spots (COMMERCIAL).
 *
 * <p>At push time {@link #resolveOnce} picks one concrete file/spot from the cart
 * using round-robin rotation, weights, and the cart's {@link SeparationPolicy}
 * (artist/title/sponsor/product/cart cooldowns). The picked play is recorded in
 * {@link CartPlayHistory} so subsequent picks in the same push (and tomorrow's push)
 * see it as recent.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository carts;
    private final CartMemberRepository members;
    private final SeparationPolicyRepository policies;
    private final CartPlayHistoryRepository history;
    private final SpotRepository spots;
    private final LibreTimeService libretime;
    private final StationRepository stations;
    private final DayPartRepository dayParts;

    public List<Cart> listForStation(UUID stationId) {
        return carts.findByStationIdOrderByKindAscNameAsc(stationId);
    }

    public Optional<Cart> get(UUID cartId) { return carts.findById(cartId); }

    public List<CartMember> membersOf(UUID cartId) {
        return members.findByCartIdOrderByPositionAsc(cartId);
    }

    public SeparationPolicy policyFor(UUID cartId) {
        return policies.findById(cartId).orElseGet(() -> {
            SeparationPolicy p = SeparationPolicy.builder().cartId(cartId).build();
            return policies.save(p);
        });
    }

    /** Categories whose audio is sourced from the LibreTime library. */
    public static final Set<String> LIBRARY_CATEGORIES = Set.of(
            "MUSIC", "IMAGING", "CONTENT", "INTERVIEW", "NEWS", "WEATHER", "PROMO", "VOICETRACK");

    /** Categories whose audio is sourced from the spot/order pool. */
    public static final Set<String> COMMERCIAL_CATEGORIES = Set.of("COMMERCIAL", "SPONSORED_FEATURE");

    @Transactional
    public Cart create(UUID stationId, String name, String kind, String category, String source,
                       UUID orderId, String description, String selectionStrategy, Integer maxAgeHours) {
        if (!"MUSIC".equals(kind) && !"COMMERCIAL".equals(kind)) {
            throw new IllegalArgumentException("kind must be MUSIC or COMMERCIAL");
        }
        String strategy = normalizeStrategy(selectionStrategy);
        Integer maxAge = normalizeMaxAge(maxAgeHours);
        if (category == null || category.isBlank()) {
            category = "COMMERCIAL".equals(kind) ? "COMMERCIAL" : "MUSIC";
        }
        if ("MUSIC".equals(kind) && !LIBRARY_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("category " + category
                    + " is not allowed for library (MUSIC) carts");
        }
        if ("COMMERCIAL".equals(kind) && !COMMERCIAL_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("category " + category
                    + " is not allowed for spot (COMMERCIAL) carts");
        }
        if (source == null) source = "MANUAL";
        if (!"MANUAL".equals(source) && !"ORDER".equals(source)) {
            throw new IllegalArgumentException("source must be MANUAL or ORDER");
        }
        if ("ORDER".equals(source) && orderId == null) {
            throw new IllegalArgumentException("orderId required for ORDER carts");
        }
        carts.findByStationIdAndName(stationId, name).ifPresent(c -> {
            throw new IllegalArgumentException("Cart name already in use for this station");
        });
        Cart c = Cart.builder()
                .stationId(stationId).name(name).kind(kind).category(category).source(source)
                .orderId(orderId).description(description).rotationPointer(0)
                .selectionStrategy(strategy).maxAgeHours(maxAge)
                .build();
        c = carts.save(c);
        policies.save(defaultPolicyFor(c.getId(), category));
        if ("ORDER".equals(source)) syncOrderCart(c);
        return c;
    }

    /** Sensible separation defaults per category; can be overridden per cart in the UI. */
    public static SeparationPolicy defaultPolicyFor(UUID cartId, String category) {
        SeparationPolicy.SeparationPolicyBuilder b = SeparationPolicy.builder().cartId(cartId);
        switch (category) {
            // PRD (SCH-03): 90-min artist / 240-min song separation for the music format.
            case "MUSIC":              b.minMinutesSameArtist((int) RotationSchedulerEngine.ARTIST_SEPARATION.toMinutes())
                                         .minMinutesSameTitle((int) RotationSchedulerEngine.SONG_SEPARATION.toMinutes()); break;
            case "IMAGING":            b.minMinutesSameTitle(10); break;
            case "CONTENT":            b.minMinutesSameTitle(60); break;
            case "INTERVIEW":          b.minMinutesSameTitle(240); break;
            case "NEWS":               b.minMinutesSameTitle(120).minMinutesSameCart(30); break;
            case "WEATHER":            b.minMinutesSameTitle(20); break;
            case "PROMO":              b.minMinutesSameTitle(30).minMinutesSameProduct(15); break;
            case "VOICETRACK":         b.minMinutesSameTitle(240); break;
            case "COMMERCIAL":         b.minMinutesSameSponsor(20).minMinutesSameProduct(10); break;
            case "SPONSORED_FEATURE":  b.minMinutesSameSponsor(60).minMinutesSameProduct(60); break;
            default: /* leave zeros */ break;
        }
        return b.build();
    }

    @Transactional
    public Cart update(UUID cartId, String name, String description,
                       String selectionStrategy, Integer maxAgeHours, String category) {
        Cart c = carts.findById(cartId).orElseThrow(() -> new IllegalArgumentException("Cart not found"));
        if (name != null && !name.equals(c.getName())) {
            carts.findByStationIdAndName(c.getStationId(), name).ifPresent(other -> {
                if (!other.getId().equals(cartId))
                    throw new IllegalArgumentException("Cart name already in use for this station");
            });
            c.setName(name);
        }
        if (description != null) c.setDescription(description);
        if (selectionStrategy != null) c.setSelectionStrategy(normalizeStrategy(selectionStrategy));
        // maxAgeHours: a negative value clears the freshness window; >=1 sets it; null leaves it.
        if (maxAgeHours != null) c.setMaxAgeHours(maxAgeHours < 0 ? null : normalizeMaxAge(maxAgeHours));
        // Category is editable (kind is not — members are validated against it). The new
        // category must be valid for the cart's kind; members and policy are left alone.
        if (category != null && !category.equals(c.getCategory())) {
            Set<String> allowed = "MUSIC".equals(c.getKind()) ? LIBRARY_CATEGORIES : COMMERCIAL_CATEGORIES;
            if (!allowed.contains(category)) {
                throw new IllegalArgumentException("category " + category
                        + " is not allowed for " + ("MUSIC".equals(c.getKind()) ? "library (MUSIC)" : "spot (COMMERCIAL)") + " carts");
            }
            c.setCategory(category);
        }
        return carts.save(c);
    }

    private static String normalizeStrategy(String s) {
        if (s == null || s.isBlank()) return Cart.STRATEGY_ROTATION;
        String v = s.trim().toUpperCase();
        if (!Cart.STRATEGY_ROTATION.equals(v) && !Cart.STRATEGY_NEWEST_FIRST.equals(v)) {
            throw new IllegalArgumentException("selectionStrategy must be ROTATION or NEWEST_FIRST");
        }
        return v;
    }

    private static Integer normalizeMaxAge(Integer h) {
        if (h == null) return null;
        if (h == 0) return null;
        if (h < 1) throw new IllegalArgumentException("maxAgeHours must be >= 1 (or omit for no limit)");
        return h;
    }

    /** Re-sync every {@code source=ORDER} cart tied to this order. Call after spots change. */
    @Transactional
    public int syncCartsForOrder(UUID orderId) {
        if (orderId == null) return 0;
        List<Cart> orderCarts = carts.findByOrderId(orderId);
        for (Cart c : orderCarts) syncOrderCart(c);
        return orderCarts.size();
    }

    @Transactional
    public void delete(UUID cartId) {
        carts.deleteById(cartId);
    }

    @Transactional
    public CartMember addMember(UUID cartId, MemberInput rawInput) {
        Cart c = carts.findById(cartId).orElseThrow(() -> new IllegalArgumentException("Cart not found"));
        if ("ORDER".equals(c.getSource())) {
            throw new IllegalArgumentException("Order-backed cart members are managed automatically");
        }
        validateMember(c.getKind(), rawInput);
        MemberInput input = rawInput;
        // Optionally fill missing music metadata from LibreTime
        if ("MUSIC".equals(c.getKind()) && input.librtimeFileId() != null
                && (isBlank(input.artist()) || isBlank(input.title()) || input.lengthSeconds() == null)) {
            try {
                final long needId = input.librtimeFileId();
                JsonNode file = libretime.clientFor(c.getStationId())
                        .listFiles(null).stream()
                        .filter(n -> n.has("id") && n.get("id").asLong() == needId)
                        .findFirst().orElse(null);
                if (file != null) {
                    if (isBlank(input.artist())) input = input.withArtist(textOrNull(file, "artist_name"));
                    if (isBlank(input.title())) input = input.withTitle(textOrNull(file, "track_title"));
                    if (input.lengthSeconds() == null) input = input.withLengthSeconds(parseLengthSeconds(textOrNull(file, "length")));
                }
            } catch (Exception ex) {
                log.warn("Could not enrich music metadata for cart {}: {}", cartId, ex.toString());
            }
        }
        // Auto-fill commercial metadata from the spot
        if ("COMMERCIAL".equals(c.getKind()) && input.spotId() != null) {
            Spot s = spots.findById(input.spotId()).orElseThrow(() -> new IllegalArgumentException("Spot not found"));
            if (input.lengthSeconds() == null) input = input.withLengthSeconds(s.getLengthSeconds());
            if (isBlank(input.sponsor())) input = input.withSponsor(s.getLabel());
            if (input.librtimeFileId() == null) input = input.withLibrtimeFileId(s.getLibrtimeFileId());
        }
        int nextPos = members.findByCartIdOrderByPositionAsc(cartId).size();
        CartMember m = CartMember.builder()
                .cartId(cartId)
                .position(input.position() != null ? input.position() : nextPos)
                .weight(input.weight() != null ? input.weight() : 1)
                .librtimeFileId(input.librtimeFileId())
                .spotId(input.spotId())
                .artist(input.artist()).title(input.title())
                .sponsor(input.sponsor()).product(input.product())
                .lengthSeconds(input.lengthSeconds())
                .freshnessAt(Instant.now())
                .enabled(input.enabled() == null || input.enabled())
                .build();
        return members.save(m);
    }

    /**
     * Add many library files to a MUSIC cart in one request. Fetches the LibreTime file list
     * once, skips duplicates already in the cart, and enriches metadata from each file node.
     */
    @Transactional
    public BulkAddResult addLibraryFilesBulk(UUID cartId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return new BulkAddResult(0, 0, 0, 0, 0);
        }
        Cart c = carts.findById(cartId).orElseThrow(() -> new IllegalArgumentException("Cart not found"));
        if (!"MUSIC".equals(c.getKind())) {
            throw new IllegalArgumentException("Bulk add applies only to library (MUSIC) carts");
        }
        if ("ORDER".equals(c.getSource())) {
            throw new IllegalArgumentException("Order-backed cart members are managed automatically");
        }
        // Preserve order, drop nulls and duplicate IDs in the request.
        List<Long> orderedUnique = new ArrayList<>();
        Set<Long> seenReq = new HashSet<>();
        for (Long fid : fileIds) {
            if (fid == null || !seenReq.add(fid)) continue;
            orderedUnique.add(fid);
        }
        int skippedDupRequest = fileIds.size() - orderedUnique.size();

        Set<Long> existing = members.findByCartIdOrderByPositionAsc(cartId).stream()
                .map(CartMember::getLibrtimeFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        List<JsonNode> all = libretime.clientFor(c.getStationId()).listFiles(null);
        Map<Long, JsonNode> byId = new HashMap<>(all.size() * 2);
        for (JsonNode n : all) {
            if (n.has("id") && !n.get("id").isNull()) {
                byId.put(n.get("id").asLong(), n);
            }
        }

        int nextPos = members.findByCartIdOrderByPositionAsc(cartId).size();
        int added = 0;
        int skippedInCart = 0;
        int skippedMissing = 0;

        for (Long fid : orderedUnique) {
            if (existing.contains(fid)) {
                skippedInCart++;
                continue;
            }
            JsonNode file = byId.get(fid);
            if (file == null) {
                skippedMissing++;
                continue;
            }
            CartMember m = CartMember.builder()
                    .cartId(cartId)
                    .position(nextPos++)
                    .weight(1)
                    .librtimeFileId(fid)
                    .artist(trimOrNull(textOrNull(file, "artist_name")))
                    .title(trimOrNull(firstNonBlank(
                            textOrNull(file, "track_title"),
                            textOrNull(file, "name"))))
                    .lengthSeconds(parseLengthSeconds(textOrNull(file, "length")))
                    .freshnessAt(fileFreshness(file))
                    .enabled(true)
                    .build();
            members.save(m);
            existing.add(fid);
            added++;
        }
        return new BulkAddResult(added, skippedInCart, skippedMissing, skippedDupRequest, orderedUnique.size());
    }

    public record BulkAddResult(int added, int skippedAlreadyInCart, int skippedNotFound,
                                int skippedDuplicateInRequest, int uniqueIdsRequested) {}

    @Transactional
    public CartMember updateMember(UUID memberId, MemberInput input) {
        CartMember m = members.findById(memberId).orElseThrow(() -> new IllegalArgumentException("Cart member not found"));
        if (input.position() != null) m.setPosition(input.position());
        if (input.weight() != null) m.setWeight(input.weight());
        if (input.artist() != null) m.setArtist(input.artist());
        if (input.title() != null) m.setTitle(input.title());
        if (input.sponsor() != null) m.setSponsor(input.sponsor());
        if (input.product() != null) m.setProduct(input.product());
        if (input.lengthSeconds() != null) m.setLengthSeconds(input.lengthSeconds());
        if (input.enabled() != null) m.setEnabled(input.enabled());
        return members.save(m);
    }

    @Transactional
    public void removeMember(UUID memberId) { members.deleteById(memberId); }

    @Transactional
    public SeparationPolicy savePolicy(UUID cartId, SeparationPolicy p) {
        if (!carts.existsById(cartId)) throw new IllegalArgumentException("Cart not found");
        p.setCartId(cartId);
        return policies.save(p);
    }

    /** Reflect order spots into a source=ORDER cart. Idempotent. */
    @Transactional
    public void syncOrderCart(Cart c) {
        if (!"ORDER".equals(c.getSource()) || c.getOrderId() == null) return;
        List<Spot> spotList = spots.findByOrderIdOrderByCreatedAtAsc(c.getOrderId());
        members.deleteByCartId(c.getId());
        members.flush();
        int pos = 0;
        for (Spot s : spotList) {
            CartMember m = CartMember.builder()
                    .cartId(c.getId())
                    .position(pos++)
                    .weight(1)
                    .librtimeFileId(s.getLibrtimeFileId())
                    .spotId(s.getId())
                    .sponsor(s.getLabel())
                    .lengthSeconds(s.getLengthSeconds())
                    .freshnessAt(s.getUpdatedAt() != null ? s.getUpdatedAt() : Instant.now())
                    .enabled(true)
                    .build();
            members.save(m);
        }
    }

    /**
     * Resolution context for a single push. Pre-fetches recent history once, lets
     * callers add "just resolved" picks so back-to-back slot resolutions see them.
     */
    public class Resolver {
        private final UUID stationId;
        private final ZoneId stationZone;
        private final List<CartPlayHistory> recent;
        private final List<CartPlayHistory> pending = new ArrayList<>();
        private final Map<UUID, List<CartMember>> memberCache = new HashMap<>();
        private final Map<UUID, SeparationPolicy> policyCache = new HashMap<>();
        private final Map<UUID, Spot> spotCache = new HashMap<>();
        private final Map<UUID, DayPart> dayPartCache = new HashMap<>();

        Resolver(UUID stationId, ZoneId stationZone, Duration lookback) {
            this.stationId = stationId;
            this.stationZone = stationZone != null ? stationZone : ZoneOffset.UTC;
            this.recent = new ArrayList<>(history.recentForStation(stationId,
                    Instant.now().minus(lookback)));
        }

        public List<CartPlayHistory> getPending() { return pending; }

        /** Backwards-compatible: no show context (used by preview). */
        public Resolution resolve(Cart cart, Instant slotTime) {
            return resolve(cart, slotTime, null);
        }

        /**
         * @param currentShowId the LibreTime show id of the instance this slot belongs to,
         *        or null when unknown (preview). Used to enforce SPECIFIC_SHOW spots.
         */
        public Resolution resolve(Cart cart, Instant slotTime, Long currentShowId) {
            List<CartMember> all = memberCache.computeIfAbsent(cart.getId(),
                    cid -> members.findByCartIdOrderByPositionAsc(cid));
            List<CartMember> enabled = all.stream().filter(CartMember::isEnabled).toList();
            if (enabled.isEmpty()) {
                return Resolution.empty("Cart \"" + cart.getName() + "\" has no enabled members");
            }

            // Freshness window: drop anything older than maxAgeHours (e.g. stale news).
            List<CartMember> usable = enabled;
            if (cart.getMaxAgeHours() != null && cart.getMaxAgeHours() > 0) {
                Instant cutoff = slotTime.minus(Duration.ofHours(cart.getMaxAgeHours()));
                usable = enabled.stream().filter(m -> freshnessOf(m).isAfter(cutoff)).toList();
                if (usable.isEmpty()) {
                    return Resolution.empty("Cart \"" + cart.getName() + "\" has no member fresher than "
                            + cart.getMaxAgeHours() + "h");
                }
            }

            SeparationPolicy pol = policyCache.computeIfAbsent(cart.getId(),
                    cid -> policies.findById(cid).orElseGet(() ->
                            SeparationPolicy.builder().cartId(cid).build()));

            boolean newest = Cart.STRATEGY_NEWEST_FIRST.equals(cart.getSelectionStrategy());
            List<CartMember> ordered = orderedCandidates(cart, usable, newest);

            CartMember picked = null;
            for (CartMember candidate : ordered) {
                if (!eligibleAtStationTime(candidate, slotTime, currentShowId)) continue;
                if (passesSeparation(candidate, cart, pol, slotTime)) {
                    picked = candidate;
                    break;
                }
            }
            String note = null;
            if (picked == null) {
                // Safety valve: air something rather than leave dead air, but still avoid
                // clutter — never put the same sponsor back-to-back in a break if any
                // other eligible member exists.
                CartMember firstEligible = null;
                for (CartMember candidate : ordered) {
                    if (!eligibleAtStationTime(candidate, slotTime, currentShowId)) continue;
                    if (firstEligible == null) firstEligible = candidate;
                    if (createsClutter(candidate)) continue;
                    picked = candidate;
                    note = "Separation violated for cart \"" + cart.getName()
                            + "\" — no member satisfied policy";
                    break;
                }
                if (picked == null && firstEligible != null) {
                    picked = firstEligible;
                    note = "Separation violated for cart \"" + cart.getName()
                            + "\" — no member satisfied policy";
                    if (createsClutter(picked)) {
                        note += "; clutter: same sponsor as the adjacent slot";
                    }
                }
            }
            if (picked == null) {
                return Resolution.empty("No eligible member for cart \"" + cart.getName()
                        + "\" at this time (time window, show targeting, or unapproved spot)");
            }

            // Rotation advances the pointer; NEWEST_FIRST always re-sorts so it doesn't.
            if (!newest) {
                int chosenIdx = usable.indexOf(picked);
                cart.setRotationPointer((chosenIdx + 1) % usable.size());
                carts.save(cart);
            }

            CartPlayHistory h = CartPlayHistory.builder()
                    .stationId(stationId)
                    .cartId(cart.getId())
                    .cartMemberId(picked.getId())
                    .artist(picked.getArtist()).title(picked.getTitle())
                    .sponsor(picked.getSponsor()).product(picked.getProduct())
                    .librtimeFileId(picked.getLibrtimeFileId())
                    .spotId(picked.getSpotId())
                    .playedAt(slotTime)
                    .build();
            pending.add(h);

            return new Resolution(picked, h, note);
        }

        private Spot spotForMember(CartMember m) {
            if (m.getSpotId() == null) return null;
            return spotCache.computeIfAbsent(m.getSpotId(), id -> spots.findById(id).orElse(null));
        }

        /**
         * Order category-pool candidate carts by when they last aired — least recently
         * (or never) first, ties broken by name. Uses both persisted history and picks
         * already made in this push, so consecutive pool slots rotate across carts.
         */
        public List<Cart> orderPoolFairly(List<Cart> candidates) {
            if (candidates == null || candidates.size() < 2) {
                return candidates == null ? List.of() : candidates;
            }
            Map<UUID, Instant> lastAired = new HashMap<>();
            for (CartPlayHistory h : recent) {
                if (h.getCartId() != null && h.getPlayedAt() != null) {
                    lastAired.merge(h.getCartId(), h.getPlayedAt(), (a, b) -> a.isAfter(b) ? a : b);
                }
            }
            for (CartPlayHistory h : pending) {
                if (h.getCartId() != null && h.getPlayedAt() != null) {
                    lastAired.merge(h.getCartId(), h.getPlayedAt(), (a, b) -> a.isAfter(b) ? a : b);
                }
            }
            List<Cart> out = new ArrayList<>(candidates);
            out.sort(Comparator
                    .comparing((Cart c) -> lastAired.getOrDefault(c.getId(), Instant.EPOCH))
                    .thenComparing(Cart::getName, Comparator.nullsLast(String::compareTo)));
            return out;
        }

        /** Round-robin from the rotation pointer, or freshest-first for NEWEST_FIRST carts. */
        private List<CartMember> orderedCandidates(Cart cart, List<CartMember> usable, boolean newest) {
            if (newest) {
                List<CartMember> copy = new ArrayList<>(usable);
                copy.sort(Comparator.comparing(Resolver::freshnessOf).reversed());
                return copy;
            }
            int n = usable.size();
            int start = ((cart.getRotationPointer() % n) + n) % n;
            List<CartMember> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) out.add(usable.get((start + i) % n));
            return out;
        }

        private static Instant freshnessOf(CartMember m) {
            if (m.getFreshnessAt() != null) return m.getFreshnessAt();
            if (m.getCreatedAt() != null) return m.getCreatedAt();
            return Instant.EPOCH;
        }

        private boolean eligibleAtStationTime(CartMember m, Instant slotTime, Long currentShowId) {
            Spot sp = spotForMember(m);
            if (sp == null) {
                return true;
            }
            // Production gate: only approved (or already trafficked) spots may air.
            if (!Spot.isAirable(sp)) {
                return false;
            }
            // Show targeting: a SPECIFIC_SHOW spot only airs inside its target show. When the
            // show is unknown (preview, currentShowId == null) we don't enforce it.
            if ("SPECIFIC_SHOW".equals(sp.getRotationKind()) && sp.getTargetShowId() != null
                    && currentShowId != null && !currentShowId.equals(sp.getTargetShowId())) {
                return false;
            }
            Integer st = sp.getLocalWindowStartMinutes();
            Integer en = sp.getLocalWindowEndMinutes();
            if (sp.getDayPartId() != null) {
                DayPart dp = dayPartCache.computeIfAbsent(sp.getDayPartId(),
                        id -> dayParts.findById(id).orElse(null));
                if (dp != null) {
                    st = dp.getStartMinutes();
                    en = dp.getEndMinutes();
                }
            }
            if (!TimeWindowUtil.hasWindow(st, en)) {
                return true;
            }
            return TimeWindowUtil.isInstantWithin(st, en, stationZone, slotTime);
        }

        /** True when picking {@code m} would place the same sponsor in two adjacent slots. */
        private boolean createsClutter(CartMember m) {
            if (m.getSponsor() == null || pending.isEmpty()) return false;
            CartPlayHistory last = pending.get(pending.size() - 1);
            return equalsIgnoreCase(last.getSponsor(), m.getSponsor());
        }

        private boolean passesSeparation(CartMember m, Cart cart, SeparationPolicy p, Instant slotTime) {
            if (within(p.getMinMinutesSameCart(), slotTime,
                    h -> Objects.equals(h.getCartId(), cart.getId()))) return false;
            // PRD (SCH-03) burnout floors for the music format: 90-min artist, 240-min song.
            // Per-cart policy can only make these stricter, never looser.
            boolean musicFormat = "MUSIC".equals(cart.getKind()) && "MUSIC".equals(cart.getCategory());
            int artistFloor = musicFormat ? (int) RotationSchedulerEngine.ARTIST_SEPARATION.toMinutes() : 0;
            int titleFloor = musicFormat ? (int) RotationSchedulerEngine.SONG_SEPARATION.toMinutes() : 0;
            if (m.getArtist() != null && within(Math.max(p.getMinMinutesSameArtist(), artistFloor), slotTime,
                    h -> equalsIgnoreCase(h.getArtist(), m.getArtist()))) return false;
            if (m.getTitle() != null && within(Math.max(p.getMinMinutesSameTitle(), titleFloor), slotTime,
                    h -> equalsIgnoreCase(h.getTitle(), m.getTitle()))) return false;
            if (m.getSponsor() != null && within(p.getMinMinutesSameSponsor(), slotTime,
                    h -> equalsIgnoreCase(h.getSponsor(), m.getSponsor()))) return false;
            if (m.getProduct() != null && within(p.getMinMinutesSameProduct(), slotTime,
                    h -> equalsIgnoreCase(h.getProduct(), m.getProduct()))) return false;
            return true;
        }

        private boolean within(int minutes, Instant slotTime,
                               java.util.function.Predicate<CartPlayHistory> match) {
            if (minutes <= 0) return false;
            Instant lower = slotTime.minus(Duration.ofMinutes(minutes));
            for (CartPlayHistory h : recent)
                if (h.getPlayedAt().isAfter(lower) && match.test(h)) return true;
            for (CartPlayHistory h : pending)
                if (h.getPlayedAt().isAfter(lower) && match.test(h)) return true;
            return false;
        }
    }

    private ZoneId zoneForStation(UUID stationId) {
        return stations.findById(stationId)
                .map(st -> {
                    try {
                        return ZoneId.of(st.getTimeZone());
                    } catch (Exception ex) {
                        return ZoneOffset.UTC;
                    }
                })
                .orElse(ZoneOffset.UTC);
    }

    public Resolver newResolver(UUID stationId, Duration lookback) {
        return new Resolver(stationId, zoneForStation(stationId), lookback);
    }

    @Transactional
    public void recordHistory(List<CartPlayHistory> entries) {
        if (entries == null || entries.isEmpty()) return;
        history.saveAll(entries);
    }

    public record Resolution(CartMember member, CartPlayHistory history, String note) {
        static Resolution empty(String note) { return new Resolution(null, null, note); }
    }

    public record MemberInput(Integer position, Integer weight,
                              Long librtimeFileId, UUID spotId,
                              String artist, String title,
                              String sponsor, String product,
                              Integer lengthSeconds, Boolean enabled) {
        public MemberInput withArtist(String a) {
            return new MemberInput(position, weight, librtimeFileId, spotId, a, title, sponsor, product, lengthSeconds, enabled);
        }
        public MemberInput withTitle(String t) {
            return new MemberInput(position, weight, librtimeFileId, spotId, artist, t, sponsor, product, lengthSeconds, enabled);
        }
        public MemberInput withSponsor(String s) {
            return new MemberInput(position, weight, librtimeFileId, spotId, artist, title, s, product, lengthSeconds, enabled);
        }
        public MemberInput withLengthSeconds(Integer l) {
            return new MemberInput(position, weight, librtimeFileId, spotId, artist, title, sponsor, product, l, enabled);
        }
        public MemberInput withLibrtimeFileId(Long id) {
            return new MemberInput(position, weight, id, spotId, artist, title, sponsor, product, lengthSeconds, enabled);
        }
    }

    private static void validateMember(String kind, MemberInput in) {
        if ("MUSIC".equals(kind)) {
            if (in.librtimeFileId() == null) {
                throw new IllegalArgumentException("Music cart members require librtimeFileId");
            }
        } else {
            if (in.spotId() == null) {
                throw new IllegalArgumentException("Commercial cart members require spotId");
            }
        }
    }

    private static String textOrNull(JsonNode n, String f) {
        return n != null && n.has(f) && !n.get(f).isNull() ? n.get(f).asText() : null;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String firstNonBlank(String a, String b) {
        if (!isBlank(a)) return a;
        if (!isBlank(b)) return b;
        return null;
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    /** Best-effort upload/modified time from a LibreTime file node; falls back to now. */
    private static Instant fileFreshness(JsonNode file) {
        for (String field : new String[]{"utime", "mtime", "track_number"}) {
            String v = textOrNull(file, field);
            if (v == null || v.isBlank()) continue;
            try { return Instant.parse(v); } catch (Exception ignored) {}
            try { return java.time.LocalDateTime.parse(v).toInstant(ZoneOffset.UTC); } catch (Exception ignored) {}
            try { return java.time.LocalDateTime.parse(v.replace(' ', 'T')).toInstant(ZoneOffset.UTC); } catch (Exception ignored) {}
        }
        return Instant.now();
    }

    private static Integer parseLengthSeconds(String s) {
        if (s == null) return null;
        var m = java.util.regex.Pattern.compile("^(\\d+):(\\d+):(\\d+)").matcher(s);
        if (m.find()) {
            return Integer.parseInt(m.group(1)) * 3600
                    + Integer.parseInt(m.group(2)) * 60
                    + Integer.parseInt(m.group(3));
        }
        try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        return null;
    }

    /** Helper kept to surface client construction for callers needing a fresh {@link LibreTimeClient}. */
    public LibreTimeClient libreTimeFor(UUID stationId) {
        return libretime.clientFor(stationId);
    }
}
