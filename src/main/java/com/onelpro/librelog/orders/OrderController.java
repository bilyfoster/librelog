package com.onelpro.librelog.orders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orders;
    private final SpotRepository spots;

    public record OrderDto(String id, String stationId, String customerId, String name,
                           LocalDate startDate, LocalDate endDate, int totalSpots, String notes,
                           long spotCount) {
        static OrderDto from(Order o, long spotCount) {
            return new OrderDto(o.getId().toString(), o.getStationId().toString(),
                    o.getCustomerId().toString(), o.getName(), o.getStartDate(), o.getEndDate(),
                    o.getTotalSpots(), o.getNotes(), spotCount);
        }
    }

    public record OrderRequest(@NotBlank String stationId, @NotBlank String customerId,
                                @NotBlank String name,
                                @NotNull LocalDate startDate, @NotNull LocalDate endDate,
                                @Positive int totalSpots, String notes) {}

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
    public OrderDto create(@Valid @RequestBody OrderRequest req) {
        Order o = Order.builder()
                .stationId(UUID.fromString(req.stationId()))
                .customerId(UUID.fromString(req.customerId()))
                .name(req.name())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .totalSpots(req.totalSpots())
                .notes(req.notes())
                .build();
        return OrderDto.from(orders.save(o), 0L);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> update(@PathVariable UUID id, @Valid @RequestBody OrderRequest req) {
        var o = orders.findById(id).orElse(null);
        if (o == null) return ResponseEntity.notFound().build();
        o.setCustomerId(UUID.fromString(req.customerId()));
        o.setName(req.name());
        o.setStartDate(req.startDate());
        o.setEndDate(req.endDate());
        o.setTotalSpots(req.totalSpots());
        o.setNotes(req.notes());
        return ResponseEntity.ok(OrderDto.from(orders.save(o), spots.countByOrderId(o.getId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!orders.existsById(id)) return ResponseEntity.notFound().build();
        orders.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
