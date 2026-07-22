package com.onelpro.librelog.carts;

import com.onelpro.librelog.librtime.LibreTimeService;
import com.onelpro.librelog.orders.Spot;
import com.onelpro.librelog.orders.SpotRepository;
import com.onelpro.librelog.station.DayPartRepository;
import com.onelpro.librelog.station.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the cart resolver: rotation, separation, freshness (NEWEST_FIRST + max age),
 * the spot approval gate, and SPECIFIC_SHOW targeting. All repositories are mocked.
 */
class CartServiceResolverTest {

    private CartRepository carts;
    private CartMemberRepository members;
    private SeparationPolicyRepository policies;
    private CartPlayHistoryRepository history;
    private SpotRepository spots;
    private StationRepository stations;
    private DayPartRepository dayParts;
    private CartService service;

    private final UUID stationId = UUID.randomUUID();
    private final UUID cartId = UUID.randomUUID();
    private final Instant now = Instant.parse("2024-06-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        carts = mock(CartRepository.class);
        members = mock(CartMemberRepository.class);
        policies = mock(SeparationPolicyRepository.class);
        history = mock(CartPlayHistoryRepository.class);
        spots = mock(SpotRepository.class);
        StationRepository stationRepo = mock(StationRepository.class);
        LibreTimeService libretime = mock(LibreTimeService.class);
        dayParts = mock(DayPartRepository.class);
        this.stations = stationRepo;

        service = new CartService(carts, members, policies, history, spots, libretime, stationRepo, dayParts);

        when(stations.findById(any())).thenReturn(Optional.empty());           // -> UTC zone
        when(history.recentForStation(any(), any())).thenReturn(List.of());    // no recent plays
        when(policies.findById(any())).thenReturn(Optional.empty());           // default zero policy
        when(carts.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private CartService.Resolver resolver() {
        return service.newResolver(stationId, Duration.ofHours(48));
    }

    private Cart musicCart(String strategy, Integer maxAgeHours) {
        return Cart.builder().id(cartId).stationId(stationId).name("Music").kind("MUSIC")
                .category("MUSIC").source("MANUAL").rotationPointer(0)
                .selectionStrategy(strategy).maxAgeHours(maxAgeHours).build();
    }

    private Cart commercialCart() {
        return Cart.builder().id(cartId).stationId(stationId).name("Spots").kind("COMMERCIAL")
                .category("COMMERCIAL").source("ORDER").rotationPointer(0)
                .selectionStrategy(Cart.STRATEGY_ROTATION).build();
    }

    private CartMember music(int pos, String artist, String title, long fileId, Instant freshness) {
        return CartMember.builder().id(UUID.randomUUID()).cartId(cartId).position(pos).weight(1)
                .librtimeFileId(fileId).artist(artist).title(title)
                .freshnessAt(freshness).createdAt(freshness).enabled(true).build();
    }

    private CartMember spotMember(int pos, UUID spotId) {
        return CartMember.builder().id(UUID.randomUUID()).cartId(cartId).position(pos).weight(1)
                .spotId(spotId).librtimeFileId(1000L + pos).freshnessAt(now).createdAt(now).enabled(true).build();
    }

    @Test
    void rotation_picksInPositionOrderThenAdvances() {
        CartMember a = music(0, "A", "Song A", 1, now);
        CartMember b = music(1, "B", "Song B", 2, now);
        when(members.findByCartIdOrderByPositionAsc(cartId)).thenReturn(List.of(a, b));

        Cart cart = musicCart(Cart.STRATEGY_ROTATION, null);
        CartService.Resolver r = resolver();
        assertThat(r.resolve(cart, now, null).member()).isEqualTo(a);
        assertThat(r.resolve(cart, now, null).member()).isEqualTo(b); // pointer advanced
    }

    @Test
    void separation_skipsMemberWithRecentTitle() {
        CartMember a = music(0, "A", "Song A", 1, now);
        CartMember b = music(1, "B", "Song B", 2, now);
        when(members.findByCartIdOrderByPositionAsc(cartId)).thenReturn(List.of(a, b));
        when(policies.findById(cartId)).thenReturn(Optional.of(
                SeparationPolicy.builder().cartId(cartId).minMinutesSameTitle(180).build()));
        when(history.recentForStation(any(), any())).thenReturn(List.of(
                CartPlayHistory.builder().id(UUID.randomUUID()).stationId(stationId).cartId(cartId)
                        .title("Song A").playedAt(now.minus(Duration.ofMinutes(10))).build()));

        CartService.Resolution res = resolver().resolve(musicCart(Cart.STRATEGY_ROTATION, null), now, null);
        assertThat(res.member()).isEqualTo(b); // A is inside the 180-min title window
    }

    @Test
    void newestFirst_picksFreshestRegardlessOfPosition() {
        CartMember old = music(0, "Old", "Old", 1, now.minus(Duration.ofHours(10)));
        CartMember fresh = music(1, "Fresh", "Fresh", 2, now.minus(Duration.ofHours(1)));
        when(members.findByCartIdOrderByPositionAsc(cartId)).thenReturn(List.of(old, fresh));

        CartService.Resolution res = resolver().resolve(musicCart(Cart.STRATEGY_NEWEST_FIRST, null), now, null);
        assertThat(res.member()).isEqualTo(fresh);
    }

    @Test
    void maxAge_excludesStaleMembers() {
        CartMember stale = music(0, "Stale", "Stale", 1, now.minus(Duration.ofHours(10)));
        CartMember fresh = music(1, "Fresh", "Fresh", 2, now.minus(Duration.ofHours(1)));
        when(members.findByCartIdOrderByPositionAsc(cartId)).thenReturn(List.of(stale, fresh));

        // 6h freshness window: only the 1h-old member survives.
        CartService.Resolution res = resolver().resolve(musicCart(Cart.STRATEGY_NEWEST_FIRST, 6), now, null);
        assertThat(res.member()).isEqualTo(fresh);
    }

    @Test
    void maxAge_returnsEmptyWhenEverythingStale() {
        CartMember stale = music(0, "Stale", "Stale", 1, now.minus(Duration.ofHours(10)));
        when(members.findByCartIdOrderByPositionAsc(cartId)).thenReturn(List.of(stale));

        CartService.Resolution res = resolver().resolve(musicCart(Cart.STRATEGY_NEWEST_FIRST, 6), now, null);
        assertThat(res.member()).isNull();
        assertThat(res.note()).contains("fresher than 6h");
    }

    @Test
    void approvalGate_skipsUnapprovedSpot() {
        UUID draftSpot = UUID.randomUUID();
        UUID approvedSpot = UUID.randomUUID();
        CartMember m1 = spotMember(0, draftSpot);
        CartMember m2 = spotMember(1, approvedSpot);
        when(members.findByCartIdOrderByPositionAsc(cartId)).thenReturn(List.of(m1, m2));
        when(spots.findById(draftSpot)).thenReturn(Optional.of(spot(draftSpot, Spot.STATUS_DRAFT, "ANY_TIME", null)));
        when(spots.findById(approvedSpot)).thenReturn(Optional.of(spot(approvedSpot, Spot.STATUS_APPROVED, "ANY_TIME", null)));

        CartService.Resolution res = resolver().resolve(commercialCart(), now, null);
        assertThat(res.member()).isEqualTo(m2); // draft spot is not airable
    }

    @Test
    void specificShow_onlyAirsInsideTargetShow() {
        UUID spotId = UUID.randomUUID();
        CartMember m = spotMember(0, spotId);
        when(members.findByCartIdOrderByPositionAsc(cartId)).thenReturn(List.of(m));
        when(spots.findById(spotId)).thenReturn(Optional.of(
                spot(spotId, Spot.STATUS_APPROVED, "SPECIFIC_SHOW", 5L)));

        assertThat(resolver().resolve(commercialCart(), now, 9L).member()).isNull();   // wrong show
        assertThat(resolver().resolve(commercialCart(), now, 5L).member()).isEqualTo(m); // target show
        assertThat(resolver().resolve(commercialCart(), now, null).member()).isEqualTo(m); // preview (lenient)
    }

    @Test
    void prdFloor_musicArtistSeparationIs90MinutesEvenWithLenientPolicy() {
        CartMember a = music(0, "Artist A", "Song A", 1, now);
        CartMember b = music(1, "Artist B", "Song B", 2, now);
        when(members.findByCartIdOrderByPositionAsc(cartId)).thenReturn(List.of(a, b));
        // Zero per-cart policy: the PRD floor alone must block the same artist for 90 min.
        when(history.recentForStation(any(), any())).thenReturn(List.of(
                CartPlayHistory.builder().id(UUID.randomUUID()).stationId(stationId).cartId(cartId)
                        .artist("Artist A").title("Song A-old").playedAt(now.minus(Duration.ofMinutes(30))).build()));

        CartService.Resolution res = resolver().resolve(musicCart(Cart.STRATEGY_ROTATION, null), now, null);
        assertThat(res.member()).isEqualTo(b);
    }

    @Test
    void prdFloor_musicSongSeparationIs240MinutesEvenWithLenientPolicy() {
        CartMember a = music(0, "Artist A", "Same Song", 1, now);
        CartMember b = music(1, "Artist B", "Other Song", 2, now);
        when(members.findByCartIdOrderByPositionAsc(cartId)).thenReturn(List.of(a, b));
        when(history.recentForStation(any(), any())).thenReturn(List.of(
                CartPlayHistory.builder().id(UUID.randomUUID()).stationId(stationId).cartId(cartId)
                        .artist("Artist C").title("Same Song").playedAt(now.minus(Duration.ofHours(3))).build()));

        CartService.Resolution res = resolver().resolve(musicCart(Cart.STRATEGY_ROTATION, null), now, null);
        assertThat(res.member()).isEqualTo(b); // 3h < 240-min floor
    }

    @Test
    void prdFloor_doesNotApplyToNonMusicCategories() {
        // NEWS carts repeat the same title hourly by design; the PRD floor is music-only.
        CartMember a = music(0, "Station", "Hourly News", 1, now);
        when(members.findByCartIdOrderByPositionAsc(cartId)).thenReturn(List.of(a));
        when(history.recentForStation(any(), any())).thenReturn(List.of(
                CartPlayHistory.builder().id(UUID.randomUUID()).stationId(stationId).cartId(cartId)
                        .artist("Station").title("Hourly News").playedAt(now.minus(Duration.ofMinutes(60))).build()));

        Cart newsCart = Cart.builder().id(cartId).stationId(stationId).name("News").kind("MUSIC")
                .category("NEWS").source("MANUAL").rotationPointer(0)
                .selectionStrategy(Cart.STRATEGY_ROTATION).build();
        CartService.Resolution res = resolver().resolve(newsCart, now, null);
        assertThat(res.member()).isEqualTo(a);
    }

    @Test
    void clutter_fallbackSkipsSponsorMatchingAdjacentSlot() {
        UUID acmeSpot = UUID.randomUUID();
        UUID betaSpot = UUID.randomUUID();
        CartMember acme = spotMember(0, acmeSpot);
        acme.setSponsor("Acme");
        CartMember beta = spotMember(1, betaSpot);
        beta.setSponsor("Beta");
        when(members.findByCartIdOrderByPositionAsc(cartId)).thenReturn(List.of(acme, beta));
        when(spots.findById(acmeSpot)).thenReturn(Optional.of(spot(acmeSpot, Spot.STATUS_APPROVED, "ANY_TIME", null)));
        when(spots.findById(betaSpot)).thenReturn(Optional.of(spot(betaSpot, Spot.STATUS_APPROVED, "ANY_TIME", null)));
        // 20-min sponsor policy with both sponsors recently played: every member violates,
        // forcing the fallback path.
        when(policies.findById(cartId)).thenReturn(Optional.of(
                SeparationPolicy.builder().cartId(cartId).minMinutesSameSponsor(20).build()));
        when(history.recentForStation(any(), any())).thenReturn(List.of(
                CartPlayHistory.builder().id(UUID.randomUUID()).stationId(stationId).cartId(cartId)
                        .sponsor("Acme").playedAt(now.minus(Duration.ofMinutes(2))).build(),
                CartPlayHistory.builder().id(UUID.randomUUID()).stationId(stationId).cartId(cartId)
                        .sponsor("Beta").playedAt(now.minus(Duration.ofMinutes(5))).build()));

        CartService.Resolver r = resolver();
        // First slot: every member violates the sponsor window, so the fallback fires.
        CartService.Resolution first = r.resolve(commercialCart(), now, null);
        assertThat(first.member()).isNotNull();
        assertThat(first.note()).contains("Separation violated");

        // Second slot 30s later: fallback must skip the sponsor of the adjacent (pending) slot.
        CartService.Resolution second = r.resolve(commercialCart(), now.plusSeconds(30), null);
        assertThat(second.member()).isNotNull();
        assertThat(second.member().getSponsor()).isNotEqualTo(first.member().getSponsor());
    }

    @Test
    void orderPoolFairly_leastRecentlyAiredFirst() {
        Cart aardvark = Cart.builder().id(UUID.randomUUID()).stationId(stationId).name("Aardvark Autos")
                .kind("COMMERCIAL").category("COMMERCIAL").build();
        Cart beta = Cart.builder().id(UUID.randomUUID()).stationId(stationId).name("Beta Bakery")
                .kind("COMMERCIAL").category("COMMERCIAL").build();
        Cart zeke = Cart.builder().id(UUID.randomUUID()).stationId(stationId).name("Zeke's Tacos")
                .kind("COMMERCIAL").category("COMMERCIAL").build();
        // Aardvark aired 5 minutes ago, Beta 3 hours ago, Zeke never.
        when(history.recentForStation(any(), any())).thenReturn(List.of(
                CartPlayHistory.builder().id(UUID.randomUUID()).stationId(stationId)
                        .cartId(aardvark.getId()).playedAt(now.minus(Duration.ofMinutes(5))).build(),
                CartPlayHistory.builder().id(UUID.randomUUID()).stationId(stationId)
                        .cartId(beta.getId()).playedAt(now.minus(Duration.ofHours(3))).build()));

        List<Cart> ordered = resolver().orderPoolFairly(List.of(aardvark, beta, zeke));
        // Never-aired first, then oldest airing — name order no longer wins.
        assertThat(ordered).containsExactly(zeke, beta, aardvark);
    }

    @Test
    void orderPoolFairly_pendingPicksRotateWithinOnePush() {
        Cart a = Cart.builder().id(UUID.randomUUID()).stationId(stationId).name("A")
                .kind("COMMERCIAL").category("COMMERCIAL").build();
        Cart b = Cart.builder().id(UUID.randomUUID()).stationId(stationId).name("B")
                .kind("COMMERCIAL").category("COMMERCIAL").build();
        UUID aSpot = UUID.randomUUID();
        CartMember aMember = spotMember(0, aSpot);
        when(members.findByCartIdOrderByPositionAsc(a.getId())).thenReturn(List.of(aMember));
        when(spots.findById(aSpot)).thenReturn(Optional.of(spot(aSpot, Spot.STATUS_APPROVED, "ANY_TIME", null)));

        CartService.Resolver r = resolver();
        assertThat(r.orderPoolFairly(List.of(a, b)).get(0)).isEqualTo(a); // tie -> name order
        // Resolving from A records a pending play, so B must come first for the next slot.
        aMember.setCartId(a.getId());
        assertThat(r.resolve(a, now, null).member()).isNotNull();
        assertThat(r.orderPoolFairly(List.of(a, b)).get(0)).isEqualTo(b);
    }

    private Spot spot(UUID id, String status, String rotationKind, Long targetShowId) {
        return Spot.builder().id(id).orderId(UUID.randomUUID()).label("Spot").lengthSeconds(30)
                .status(status).rotationKind(rotationKind).targetShowId(targetShowId).build();
    }
}
