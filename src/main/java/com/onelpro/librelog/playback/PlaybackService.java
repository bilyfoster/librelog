package com.onelpro.librelog.playback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/**
 * Imports as-run playback history from LibreTime and matches it back to scheduled items.
 *
 * Matching rule (intentionally simple for MVP):
 *   - Pair by {@code librtime_file_id} within +/- 5 minutes of the scheduled time.
 *   - First match wins; an unmatched scheduled item is MISSED; an unmatched playback
 *     row that lands inside the day's window is recorded but produces no row in the
 *     reconciliation table.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlaybackService {

    private static final Duration MATCH_WINDOW = Duration.ofMinutes(5);

    private final LibreTimeService libretime;
    private final PlaybackLogRepository logs;
    private final ReconciliationRepository recons;
    private final ScheduleDayRepository days;
    private final ScheduleItemRepository items;
    private final OrderRepository orders;
    private final SpotRepository spots;
    private final CustomerRepository customers;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ImportResult importDay(UUID stationId, LocalDate date) {
        var client = libretime.clientFor(stationId);
        List<JsonNode> entries = client.playoutHistory(date);

        Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        var existing = logs.findByStationIdAndPlayedAtBetweenOrderByPlayedAtAsc(stationId, from, to);
        logs.deleteAll(existing);
        logs.flush();

        int saved = 0;
        for (JsonNode n : entries) {
            Instant playedAt = parseTime(n);
            if (playedAt == null) continue;
            Long fileId = null;
            if (n.has("file") && !n.get("file").isNull()) fileId = n.get("file").asLong();
            else if (n.has("file_id") && !n.get("file_id").isNull()) fileId = n.get("file_id").asLong();
            PlaybackLogEntry e = PlaybackLogEntry.builder()
                    .stationId(stationId)
                    .playedAt(playedAt)
                    .librtimeFileId(fileId)
                    .raw(n.toString())
                    .build();
            logs.save(e);
            saved++;
        }

        ScheduleDay day = days.findByStationIdAndDate(stationId, date).orElse(null);
        ReconcileResult rec = day == null ? new ReconcileResult(0, 0, 0)
                : reconcileDay(day);
        return new ImportResult(saved, rec.matched(), rec.missed());
    }

    public List<PlaybackRow> listDay(UUID stationId, LocalDate date) {
        Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return logs.findByStationIdAndPlayedAtBetweenOrderByPlayedAtAsc(stationId, from, to)
                .stream()
                .map(e -> {
                    JsonNode raw = parseRaw(e.getRaw());
                    return new PlaybackRow(
                            e.getId().toString(),
                            e.getPlayedAt(),
                            e.getLibrtimeFileId(),
                            firstText(raw, "name", "title", "file_name", "filename"),
                            nestedText(raw, "file", "name", "title", "filename"),
                            firstText(raw, "show_name", "show"),
                            e.getLengthSeconds());
                })
                .toList();
    }

    @Transactional
    public ReconcileResult reconcileDay(ScheduleDay day) {
        var scheduled = items.findByScheduleDayIdOrderByPositionAsc(day.getId());
        recons.deleteByScheduleItemIdIn(scheduled.stream().map(ScheduleItem::getId).toList());
        recons.flush();

        Instant from = day.getDate().atStartOfDay(ZoneOffset.UTC).toInstant().minus(MATCH_WINDOW);
        Instant to = day.getDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().plus(MATCH_WINDOW);
        var actual = new ArrayList<>(logs.findByStationIdAndPlayedAtBetweenOrderByPlayedAtAsc(
                day.getStationId(), from, to));

        int matched = 0, missed = 0;
        for (ScheduleItem it : scheduled) {
            if (it.getLibrtimeFileId() == null || it.getScheduledAt() == null) continue;
            PlaybackLogEntry hit = null;
            int hitIdx = -1;
            for (int i = 0; i < actual.size(); i++) {
                PlaybackLogEntry p = actual.get(i);
                if (!Objects.equals(p.getLibrtimeFileId(), it.getLibrtimeFileId())) continue;
                if (Duration.between(p.getPlayedAt(), it.getScheduledAt()).abs().compareTo(MATCH_WINDOW) <= 0) {
                    hit = p; hitIdx = i; break;
                }
            }
            Reconciliation r = Reconciliation.builder()
                    .scheduleItemId(it.getId())
                    .playbackLogEntryId(hit == null ? null : hit.getId())
                    .matchedAt(hit == null ? null : Instant.now())
                    .status(hit == null ? "MISSED" : "MATCHED")
                    .build();
            recons.save(r);
            if (hit != null) {
                actual.remove(hitIdx);
                matched++;
            } else {
                missed++;
            }
        }
        return new ReconcileResult(scheduled.size(), matched, missed);
    }

    public OrderReconciliation orderSummary(UUID orderId) {
        Order o = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        var orderSpots = spots.findByOrderIdOrderByCreatedAtAsc(orderId);
        var spotIds = orderSpots.stream().map(Spot::getId).toList();
        if (spotIds.isEmpty()) {
            return new OrderReconciliation(orderId.toString(), o.getName(), 0, 0, 0, List.of());
        }

        var allItems = items.findBySpotIdIn(spotIds);
        var itemIds = allItems.stream().map(ScheduleItem::getId).toList();
        var reconciliations = itemIds.isEmpty() ? List.<Reconciliation>of()
                : recons.findByScheduleItemIdIn(itemIds);

        Map<UUID, Reconciliation> byItem = new HashMap<>();
        for (Reconciliation r : reconciliations) byItem.put(r.getScheduleItemId(), r);

        int scheduledCount = allItems.size();
        int matched = 0, missed = 0;
        var rows = new ArrayList<OrderReconciliationRow>();
        for (ScheduleItem it : allItems) {
            Spot s = orderSpots.stream().filter(sp -> sp.getId().equals(it.getSpotId())).findFirst().orElse(null);
            Reconciliation r = byItem.get(it.getId());
            String status = r == null ? "PENDING" : r.getStatus();
            if ("MATCHED".equals(status)) matched++;
            else if ("MISSED".equals(status)) missed++;
            rows.add(new OrderReconciliationRow(
                    it.getId().toString(),
                    s == null ? null : s.getLabel(),
                    it.getScheduledAt(),
                    status));
        }

        return new OrderReconciliation(orderId.toString(), o.getName(),
                scheduledCount, matched, missed, rows);
    }

    /**
     * Per-order fulfillment for one station+date (PRD §7.3 make-up reporting).
     * Ordered = scheduled items referencing airable spots of orders active on the date;
     * played = items whose reconciliation row is MATCHED; everything else is a missed
     * (make-up candidate), reported with its spot label.
     */
    public List<FulfillmentRow> fulfillment(UUID stationId, LocalDate date) {
        ScheduleDay day = days.findByStationIdAndDate(stationId, date).orElse(null);
        if (day == null) return List.of();
        var dayItems = items.findByScheduleDayIdOrderByPositionAsc(day.getId()).stream()
                .filter(i -> i.getSpotId() != null)
                .toList();
        if (dayItems.isEmpty()) return List.of();

        var itemIds = dayItems.stream().map(ScheduleItem::getId).toList();
        Map<UUID, Reconciliation> byItem = new HashMap<>();
        for (Reconciliation r : recons.findByScheduleItemIdIn(itemIds)) byItem.put(r.getScheduleItemId(), r);

        var spotIds = dayItems.stream().map(ScheduleItem::getSpotId).distinct().toList();
        Map<UUID, Spot> spotById = new HashMap<>();
        for (Spot s : spots.findAllById(spotIds)) spotById.put(s.getId(), s);

        var orderIds = spotById.values().stream().map(Spot::getOrderId).distinct().toList();
        Map<UUID, Order> orderById = new HashMap<>();
        for (Order o : orders.findAllById(orderIds)) orderById.put(o.getId(), o);
        var customerIds = orderById.values().stream().map(Order::getCustomerId).distinct().toList();
        Map<UUID, String> customerNames = new HashMap<>();
        for (Customer c : customers.findAllById(customerIds)) customerNames.put(c.getId(), c.getName());

        Map<UUID, FulfillmentAcc> byOrder = new LinkedHashMap<>();
        for (ScheduleItem it : dayItems) {
            Spot s = spotById.get(it.getSpotId());
            if (!Spot.isAirable(s)) continue;
            Order o = orderById.get(s.getOrderId());
            if (o == null || o.getStartDate().isAfter(date)) continue;
            if (o.getEndDate() != null && o.getEndDate().isBefore(date)) continue;
            FulfillmentAcc acc = byOrder.computeIfAbsent(o.getId(), k -> new FulfillmentAcc());
            acc.ordered++;
            Reconciliation r = byItem.get(it.getId());
            if (r != null && "MATCHED".equals(r.getStatus())) acc.played++;
            else acc.missedLabels.add(s.getLabel());
        }

        return byOrder.entrySet().stream()
                .map(e -> {
                    Order o = orderById.get(e.getKey());
                    FulfillmentAcc a = e.getValue();
                    return new FulfillmentRow(e.getKey().toString(), o.getName(),
                            customerNames.get(o.getCustomerId()),
                            a.ordered, a.played, a.missedLabels.size(), List.copyOf(a.missedLabels));
                })
                .toList();
    }

    private static final class FulfillmentAcc {
        int ordered;
        int played;
        final List<String> missedLabels = new ArrayList<>();
    }

    private static Instant parseTime(JsonNode n) {
        for (String field : List.of("starts", "starts_at", "played_at", "start_time")) {
            if (n.has(field) && !n.get(field).isNull()) {
                try {
                    return parseLoose(n.get(field).asText());
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static Instant parseLoose(String s) {
        try { return Instant.parse(s); } catch (Exception ignored) {}
        try { return LocalDateTime.parse(s).toInstant(ZoneOffset.UTC); } catch (Exception ignored) {}
        return ZonedDateTime.parse(s).toInstant();
    }

    private JsonNode parseRaw(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return objectMapper.readTree(raw); } catch (Exception ignored) { return null; }
    }

    private static String firstText(JsonNode n, String... fields) {
        if (n == null) return null;
        for (String field : fields) {
            if (n.has(field) && !n.get(field).isNull()) {
                String value = n.get(field).asText();
                if (value != null && !value.isBlank()) return value;
            }
        }
        return null;
    }

    private static String nestedText(JsonNode n, String objectField, String... fields) {
        if (n == null || !n.has(objectField) || !n.get(objectField).isObject()) return null;
        return firstText(n.get(objectField), fields);
    }

    public record ImportResult(int entriesSaved, int matched, int missed) {}
    public record PlaybackRow(String id, Instant playedAt, Long librtimeFileId,
                              String name, String fileName, String showName,
                              Integer lengthSeconds) {}
    public record ReconcileResult(int scheduled, int matched, int missed) {}
    public record OrderReconciliation(String orderId, String orderName,
                                      int scheduled, int matched, int missed,
                                      List<OrderReconciliationRow> rows) {}
    public record OrderReconciliationRow(String scheduleItemId, String spotLabel,
                                         Instant scheduledAt, String status) {}
    public record FulfillmentRow(String orderId, String orderName, String customerName,
                                 int ordered, int played, int missed,
                                 List<String> missedSpotLabels) {}
}
