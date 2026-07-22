package com.onelpro.librelog.playback;

import com.onelpro.librelog.customers.Customer;
import com.onelpro.librelog.customers.CustomerRepository;
import com.onelpro.librelog.librtime.LibreTimeService;
import com.onelpro.librelog.orders.Order;
import com.onelpro.librelog.orders.OrderRepository;
import com.onelpro.librelog.orders.Spot;
import com.onelpro.librelog.orders.SpotRepository;
import com.onelpro.librelog.schedule.ScheduleDay;
import com.onelpro.librelog.schedule.ScheduleDayRepository;
import com.onelpro.librelog.schedule.ScheduleItem;
import com.onelpro.librelog.schedule.ScheduleItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for per-order fulfillment (make-up reporting) and the scoped
 * orderSummary query. All repositories are mocked.
 */
class PlaybackServiceFulfillmentTest {

    private PlaybackLogRepository logs;
    private ReconciliationRepository recons;
    private ScheduleDayRepository days;
    private ScheduleItemRepository items;
    private OrderRepository orders;
    private SpotRepository spots;
    private CustomerRepository customers;
    private PlaybackService service;

    private final UUID stationId = UUID.randomUUID();
    private final UUID dayId = UUID.randomUUID();
    private final LocalDate date = LocalDate.of(2024, 6, 2);

    private final UUID customerId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID otherOrderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        logs = mock(PlaybackLogRepository.class);
        recons = mock(ReconciliationRepository.class);
        days = mock(ScheduleDayRepository.class);
        items = mock(ScheduleItemRepository.class);
        orders = mock(OrderRepository.class);
        spots = mock(SpotRepository.class);
        customers = mock(CustomerRepository.class);
        service = new PlaybackService(mock(LibreTimeService.class), logs, recons, days, items,
                orders, spots, customers);

        when(days.findByStationIdAndDate(stationId, date)).thenReturn(Optional.of(
                ScheduleDay.builder().id(dayId).stationId(stationId).date(date).status("PUSHED").build()));
        when(customers.findAllById(any())).thenReturn(List.of(
                Customer.builder().id(customerId).stationId(stationId).name("Acme Corp").build()));
    }

    private Order order(UUID id, LocalDate start, LocalDate end) {
        return Order.builder().id(id).stationId(stationId).customerId(customerId)
                .name("Order " + id.toString().substring(0, 4))
                .startDate(start).endDate(end).totalSpots(10).build();
    }

    private Spot spot(UUID orderId, String status, String label) {
        return Spot.builder().id(UUID.randomUUID()).orderId(orderId).label(label)
                .status(status).rotationKind("ANY_TIME").lengthSeconds(30).build();
    }

    private ScheduleItem item(Spot s) {
        return ScheduleItem.builder().id(UUID.randomUUID()).scheduleDayId(dayId)
                .slotIndex(0).kind("SPOT").spotId(s.getId()).librtimeFileId(1L)
                .scheduledAt(Instant.parse("2024-06-02T12:00:00Z")).position(0).build();
    }

    private Reconciliation recon(ScheduleItem it, String status) {
        return Reconciliation.builder().id(UUID.randomUUID()).scheduleItemId(it.getId())
                .status(status).build();
    }

    @Test
    void fulfillment_countsOrderedPlayedAndMissedWithLabels() {
        Spot aired = spot(orderId, Spot.STATUS_TRAFFICKED, "Acme :30 A");
        Spot skipped = spot(orderId, Spot.STATUS_APPROVED, "Acme :30 B");
        ScheduleItem i1 = item(aired);
        ScheduleItem i2 = item(skipped);
        when(items.findByScheduleDayIdOrderByPositionAsc(dayId)).thenReturn(List.of(i1, i2));
        when(recons.findByScheduleItemIdIn(any())).thenReturn(List.of(
                recon(i1, "MATCHED"), recon(i2, "MISSED")));
        when(spots.findAllById(any())).thenReturn(List.of(aired, skipped));
        when(orders.findAllById(any())).thenReturn(List.of(
                order(orderId, date.minusDays(1), date.plusDays(10))));

        var rows = service.fulfillment(stationId, date);

        assertThat(rows).hasSize(1);
        var row = rows.get(0);
        assertThat(row.orderId()).isEqualTo(orderId.toString());
        assertThat(row.customerName()).isEqualTo("Acme Corp");
        assertThat(row.ordered()).isEqualTo(2);
        assertThat(row.played()).isEqualTo(1);
        assertThat(row.missed()).isEqualTo(1);
        assertThat(row.missedSpotLabels()).containsExactly("Acme :30 B");
    }

    @Test
    void fulfillment_reportsZeroMissesWhenEverythingAired() {
        Spot s = spot(orderId, Spot.STATUS_TRAFFICKED, "Acme :30 A");
        ScheduleItem i1 = item(s);
        when(items.findByScheduleDayIdOrderByPositionAsc(dayId)).thenReturn(List.of(i1));
        when(recons.findByScheduleItemIdIn(any())).thenReturn(List.of(recon(i1, "MATCHED")));
        when(spots.findAllById(any())).thenReturn(List.of(s));
        when(orders.findAllById(any())).thenReturn(List.of(order(orderId, date, null)));

        var rows = service.fulfillment(stationId, date);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).missed()).isZero();
        assertThat(rows.get(0).missedSpotLabels()).isEmpty();
    }

    @Test
    void fulfillment_excludesDraftSpotsAndInactiveOrders() {
        Spot draft = spot(orderId, Spot.STATUS_DRAFT, "Draft spot");
        Spot expired = spot(otherOrderId, Spot.STATUS_TRAFFICKED, "Expired order spot");
        ScheduleItem i1 = item(draft);
        ScheduleItem i2 = item(expired);
        when(items.findByScheduleDayIdOrderByPositionAsc(dayId)).thenReturn(List.of(i1, i2));
        when(recons.findByScheduleItemIdIn(any())).thenReturn(List.of());
        when(spots.findAllById(any())).thenReturn(List.of(draft, expired));
        when(orders.findAllById(any())).thenReturn(List.of(
                order(orderId, date.minusDays(1), null),          // active, but its spot is DRAFT
                order(otherOrderId, date.minusDays(30), date.minusDays(1)))); // ended yesterday

        var rows = service.fulfillment(stationId, date);

        assertThat(rows).isEmpty();
    }

    @Test
    void fulfillment_countsUnreconciledScheduledItemAsMissed() {
        Spot s = spot(orderId, Spot.STATUS_TRAFFICKED, "Acme :30 A");
        ScheduleItem i1 = item(s);
        when(items.findByScheduleDayIdOrderByPositionAsc(dayId)).thenReturn(List.of(i1));
        when(recons.findByScheduleItemIdIn(any())).thenReturn(List.of()); // no reconciliation row
        when(spots.findAllById(any())).thenReturn(List.of(s));
        when(orders.findAllById(any())).thenReturn(List.of(order(orderId, date, null)));

        var rows = service.fulfillment(stationId, date);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).played()).isZero();
        assertThat(rows.get(0).missedSpotLabels()).containsExactly("Acme :30 A");
    }

    @Test
    void fulfillment_returnsEmptyWhenNoScheduleDay() {
        when(days.findByStationIdAndDate(stationId, date)).thenReturn(Optional.empty());

        assertThat(service.fulfillment(stationId, date)).isEmpty();
    }

    @Test
    void orderSummary_usesScopedQueryInsteadOfFindAll() {
        Spot s = spot(orderId, Spot.STATUS_TRAFFICKED, "Acme :30 A");
        when(orders.findById(orderId)).thenReturn(Optional.of(order(orderId, date, null)));
        when(spots.findByOrderIdOrderByCreatedAtAsc(orderId)).thenReturn(List.of(s));
        when(items.findBySpotIdIn(any())).thenReturn(List.of());

        var summary = service.orderSummary(orderId);

        assertThat(summary.scheduled()).isZero();
        verify(items).findBySpotIdIn(List.of(s.getId()));
        verify(items, never()).findAll();
    }
}
