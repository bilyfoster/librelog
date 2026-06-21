package com.onelpro.librelog.carts;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ClockController {

    private final ClockService clocks;

    public record ClockDto(String id, String stationId, String name, String description,
                           List<SlotDto> slots) {}

    public record SlotDto(String id, int position, String kind,
                          String cartId, String cartCategory, Long librtimeFileId, String spotId,
                          String label, Integer defaultLengthSeconds) {
        static SlotDto from(ClockTemplateSlot s) {
            return new SlotDto(s.getId() == null ? null : s.getId().toString(),
                    s.getPosition(), s.getKind(),
                    s.getCartId() == null ? null : s.getCartId().toString(),
                    s.getCartCategory(),
                    s.getLibrtimeFileId(),
                    s.getSpotId() == null ? null : s.getSpotId().toString(),
                    s.getLabel(), s.getDefaultLengthSeconds());
        }
    }

    public record ClockRequest(String name, String description) {}

    public record SlotRequest(String kind, String cartId, String cartCategory, Long librtimeFileId, String spotId,
                              String label, Integer defaultLengthSeconds) {
        ClockTemplateSlot toEntity() {
            return ClockTemplateSlot.builder()
                    .kind(kind)
                    .cartId(uuidOrNull(cartId))
                    .cartCategory(trimOrNull(cartCategory))
                    .librtimeFileId(librtimeFileId)
                    .spotId(uuidOrNull(spotId))
                    .label(label)
                    .defaultLengthSeconds(defaultLengthSeconds)
                    .build();
        }

        private static UUID uuidOrNull(String s) {
            if (s == null || s.isBlank()) return null;
            return UUID.fromString(s.trim());
        }

        private static String trimOrNull(String s) {
            if (s == null) return null;
            String t = s.trim();
            return t.isEmpty() ? null : t;
        }
    }

    @GetMapping("/api/stations/{stationId}/clocks")
    public List<ClockDto> list(@PathVariable UUID stationId) {
        return clocks.listForStation(stationId).stream()
                .map(c -> toDto(c, clocks.slotsOf(c.getId())))
                .toList();
    }

    @PostMapping("/api/stations/{stationId}/clocks")
    public ResponseEntity<?> create(@PathVariable UUID stationId, @RequestBody ClockRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name is required"));
        }
        ClockTemplate c = clocks.create(stationId, req.name(), req.description());
        return ResponseEntity.ok(toDto(c, List.of()));
    }

    @PutMapping("/api/clocks/{clockId}")
    public ResponseEntity<?> rename(@PathVariable UUID clockId, @RequestBody ClockRequest req) {
        try {
            ClockTemplate c = clocks.rename(clockId, req.name(), req.description());
            return ResponseEntity.ok(toDto(c, clocks.slotsOf(c.getId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/clocks/{clockId}")
    public ResponseEntity<Void> delete(@PathVariable UUID clockId) {
        clocks.delete(clockId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/clocks/{clockId}/slots")
    public ResponseEntity<?> setSlots(@PathVariable UUID clockId, @RequestBody List<SlotRequest> req) {
        try {
            var saved = clocks.setSlots(clockId, req.stream().map(SlotRequest::toEntity).toList());
            return ResponseEntity.ok(saved.stream().map(SlotDto::from).toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private static ClockDto toDto(ClockTemplate c, List<ClockTemplateSlot> slots) {
        return new ClockDto(c.getId().toString(), c.getStationId().toString(),
                c.getName(), c.getDescription(),
                slots.stream().map(SlotDto::from).toList());
    }
}
