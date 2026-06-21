package com.onelpro.librelog.orders;

import com.onelpro.librelog.station.DayPart;
import com.onelpro.librelog.station.DayPartRepository;
import com.onelpro.librelog.time.TimeWindowUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SpotController {

    private final SpotRepository spots;
    private final OrderRepository orders;
    private final DayPartRepository dayParts;

    public record SpotDto(String id, String orderId, Long librtimeFileId, int lengthSeconds,
                          String label, String rotationKind, Long targetShowId,
                          String dayPartId,
                          Integer localWindowStartMinutes, Integer localWindowEndMinutes) {
        static SpotDto from(Spot s) {
            return new SpotDto(s.getId().toString(), s.getOrderId().toString(),
                    s.getLibrtimeFileId(), s.getLengthSeconds(), s.getLabel(),
                    s.getRotationKind(), s.getTargetShowId(),
                    s.getDayPartId() == null ? null : s.getDayPartId().toString(),
                    s.getLocalWindowStartMinutes(), s.getLocalWindowEndMinutes());
        }
    }

    public record SpotRequest(@NotBlank String label, @Positive int lengthSeconds,
                              Long librtimeFileId, String rotationKind, Long targetShowId,
                              UUID dayPartId,
                              Integer localWindowStartMinutes, Integer localWindowEndMinutes) {}

    @GetMapping("/api/orders/{orderId}/spots")
    public ResponseEntity<List<SpotDto>> listForOrder(@PathVariable UUID orderId) {
        if (!orders.existsById(orderId)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(spots.findByOrderIdOrderByCreatedAtAsc(orderId)
                .stream().map(SpotDto::from).toList());
    }

    @PostMapping("/api/orders/{orderId}/spots")
    public ResponseEntity<?> create(@PathVariable UUID orderId, @Valid @RequestBody SpotRequest req) {
        Order order = orders.findById(orderId).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();
        long count = spots.countByOrderId(orderId);
        int cap = Order.spotCap(order);
        if (count >= cap) {
            String msg = cap == 0
                    ? "Order has no spot capacity configured."
                    : "This order allows at most " + cap + " spot(s). Remove one or raise the order limit.";
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
        String rotation = req.rotationKind() == null ? "ANY_TIME" : req.rotationKind();
        if (!"ANY_TIME".equals(rotation) && !"SPECIFIC_SHOW".equals(rotation)) {
            return ResponseEntity.badRequest().body(Map.of("error", "rotationKind must be ANY_TIME or SPECIFIC_SHOW"));
        }
        try {
            Spot s = Spot.builder()
                    .orderId(orderId)
                    .label(req.label())
                    .lengthSeconds(req.lengthSeconds())
                    .librtimeFileId(req.librtimeFileId())
                    .rotationKind(rotation)
                    .targetShowId(req.targetShowId())
                    .build();
            applySpotWindow(order, s, req);
            return ResponseEntity.ok(SpotDto.from(spots.save(s)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/spots/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody SpotRequest req) {
        var s = spots.findById(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();
        Order order = orders.findById(s.getOrderId()).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();
        s.setLabel(req.label());
        s.setLengthSeconds(req.lengthSeconds());
        s.setLibrtimeFileId(req.librtimeFileId());
        if (req.rotationKind() != null) {
            if (!"ANY_TIME".equals(req.rotationKind()) && !"SPECIFIC_SHOW".equals(req.rotationKind())) {
                return ResponseEntity.badRequest().body(Map.of("error", "rotationKind must be ANY_TIME or SPECIFIC_SHOW"));
            }
            s.setRotationKind(req.rotationKind());
        }
        s.setTargetShowId(req.targetShowId());
        try {
            applySpotWindow(order, s, req);
            return ResponseEntity.ok(SpotDto.from(spots.save(s)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/spots/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!spots.existsById(id)) return ResponseEntity.notFound().build();
        spots.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void applySpotWindow(Order order, Spot s, SpotRequest req) {
        if (req.dayPartId() != null) {
            DayPart dp = dayParts.findById(req.dayPartId())
                    .orElseThrow(() -> new IllegalArgumentException("Day part not found"));
            if (!dp.getStationId().equals(order.getStationId())) {
                throw new IllegalArgumentException("Day part does not belong to this order's station");
            }
            s.setDayPartId(req.dayPartId());
            s.setLocalWindowStartMinutes(null);
            s.setLocalWindowEndMinutes(null);
            return;
        }
        s.setDayPartId(null);
        s.setLocalWindowStartMinutes(req.localWindowStartMinutes());
        s.setLocalWindowEndMinutes(TimeWindowUtil.normalizeExclusiveEnd(req.localWindowEndMinutes()));
        TimeWindowUtil.validateWindowPair(s.getLocalWindowStartMinutes(), s.getLocalWindowEndMinutes(), "Spot");
    }
}
