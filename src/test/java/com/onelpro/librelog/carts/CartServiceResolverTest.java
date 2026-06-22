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

    private Spot spot(UUID id, String status, String rotationKind, Long targetShowId) {
        return Spot.builder().id(id).orderId(UUID.randomUUID()).label("Spot").lengthSeconds(30)
                .status(status).rotationKind(rotationKind).targetShowId(targetShowId).build();
    }
}
