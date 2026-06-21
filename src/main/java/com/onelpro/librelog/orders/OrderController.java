package com.onelpro.librelog.orders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orders;
    private final SpotRepository spots;

    public record OrderDto(String id, String stationId, String customerId, String name,
                           LocalDate startDate, LocalDate endDate, int totalSpots, String notes,
                           String orderKind, Integer monthlySpotAllowance, Integer monthlyPriceCents,
                           int spotCap, long spotCount) {
        static OrderDto from(Order o, long spotCount) {
            return new OrderDto(o.getId().toString(), o.getStationId().toString(),
                    o.getCustomerId().toString(), o.getName(), o.getStartDate(), o.getEndDate(),
                    o.getTotalSpots(), o.getNotes(),
                    o.getOrderKind(), o.getMonthlySpotAllowance(), o.getMonthlyPriceCents(),
                    Order.spotCap(o), spotCount);
        }
    }

    public record OrderRequest(@NotBlank String stationId, @NotBlank String customerId,
                               @NotBlank String name,
                               @NotNull LocalDate startDate, LocalDate endDate,
                               Integer totalSpots, String orderKind,
                               Integer monthlySpotAllowance, Integer monthlyPriceCents,
                               String notes) {}

    @GetMapping
    public List<OrderDto> list(@RequestParam(required = false) UUID stationId,
                               @RequestParam(required = false) UUID customerId) {
        List<Order> list;
        if (customerId != null) list = orders.findByCustomerIdOrderByStartDateDesc(customerId);
        else if (stationId != null) list = orders.findByStationIdOrderByStartDateDesc(stationId);
        else list = orders.findAll();
        return list.stream()
                .map(o -> OrderDto.from(o, spots.countByOrderId(o.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> get(@PathVariable UUID id) {
        return orders.findById(id)
                .map(o -> OrderDto.from(o, spots.countByOrderId(o.getId())))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody OrderRequest req) {
        try {
            String kind = normalizeKind(req.orderKind());
            validateForKind(req, kind);
            Order o = buildOrderFromRequest(UUID.randomUUID(), req, kind);
            return ResponseEntity.ok(OrderDto.from(orders.save(o), 0L));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody OrderRequest req) {
        try {
            var o = orders.findById(id).orElse(null);
            if (o == null) return ResponseEntity.notFound().build();
            String kind = normalizeKind(req.orderKind());
            validateForKind(req, kind);
            applyRequestToOrder(o, req, kind);
            long spotCount = spots.countByOrderId(o.getId());
            int cap = Order.spotCap(o);
            if (spotCount > cap) {
                throw new IllegalArgumentException(
                        "This order already has " + spotCount + " spot(s); new limit is " + cap
                                + ". Remove spots first or keep a higher limit.");
            }
            return ResponseEntity.ok(OrderDto.from(orders.save(o), spotCount));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!orders.existsById(id)) return ResponseEntity.notFound().build();
        orders.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private static String normalizeKind(String raw) {
        if (raw == null || raw.isBlank()) return Order.KIND_STANDARD;
        return raw.trim().toUpperCase();
    }

    private static void validateForKind(OrderRequest req, String kind) {
        if (Order.KIND_FOUNDING_MEMBER.equals(kind)) {
            if (req.monthlySpotAllowance() == null || req.monthlySpotAllowance() < 1) {
                throw new IllegalArgumentException(
                        "Founding member orders require monthlySpotAllowance (e.g. 10 spots per month)");
            }
            if (req.monthlyPriceCents() != null && req.monthlyPriceCents() < 0) {
                throw new IllegalArgumentException("monthlyPriceCents cannot be negative");
            }
            return;
        }
        if (Order.KIND_STANDARD.equals(kind)) {
            if (req.endDate() == null) {
                throw new IllegalArgumentException("Standard orders require an end date");
            }
            if (req.totalSpots() == null || req.totalSpots() < 1) {
                throw new IllegalArgumentException("Standard orders require totalSpots >= 1");
            }
            return;
        }
        throw new IllegalArgumentException("orderKind must be STANDARD or FOUNDING_MEMBER");
    }

    private static Order buildOrderFromRequest(UUID id, OrderRequest req, String kind) {
        boolean founding = Order.KIND_FOUNDING_MEMBER.equals(kind);
        return Order.builder()
                .id(id)
                .stationId(UUID.fromString(req.stationId()))
                .customerId(UUID.fromString(req.customerId()))
                .name(req.name())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .totalSpots(founding ? 0 : req.totalSpots())
                .orderKind(kind)
                .monthlySpotAllowance(founding ? req.monthlySpotAllowance() : null)
                .monthlyPriceCents(founding ? req.monthlyPriceCents() : null)
                .notes(req.notes())
                .build();
    }

    private static void applyRequestToOrder(Order o, OrderRequest req, String kind) {
        boolean founding = Order.KIND_FOUNDING_MEMBER.equals(kind);
        o.setCustomerId(UUID.fromString(req.customerId()));
        o.setName(req.name());
        o.setStartDate(req.startDate());
        o.setEndDate(req.endDate());
        o.setOrderKind(kind);
        o.setNotes(req.notes());
        if (founding) {
            o.setTotalSpots(0);
            o.setMonthlySpotAllowance(req.monthlySpotAllowance());
            o.setMonthlyPriceCents(req.monthlyPriceCents());
        } else {
            o.setTotalSpots(req.totalSpots());
            o.setMonthlySpotAllowance(null);
            o.setMonthlyPriceCents(null);
        }
    }
}
