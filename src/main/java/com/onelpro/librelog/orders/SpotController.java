package com.onelpro.librelog.orders;

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

    public record SpotDto(String id, String orderId, Long librtimeFileId, int lengthSeconds,
                          String label, String rotationKind, Long targetShowId) {
        static SpotDto from(Spot s) {
            return new SpotDto(s.getId().toString(), s.getOrderId().toString(),
                    s.getLibrtimeFileId(), s.getLengthSeconds(), s.getLabel(),
                    s.getRotationKind(), s.getTargetShowId());
        }
    }

    public record SpotRequest(@NotBlank String label, @Positive int lengthSeconds,
                              Long librtimeFileId, String rotationKind, Long targetShowId) {}

    @GetMapping("/api/orders/{orderId}/spots")
    public ResponseEntity<List<SpotDto>> listForOrder(@PathVariable UUID orderId) {
        if (!orders.existsById(orderId)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(spots.findByOrderIdOrderByCreatedAtAsc(orderId)
                .stream().map(SpotDto::from).toList());
    }

    @PostMapping("/api/orders/{orderId}/spots")
    public ResponseEntity<?> create(@PathVariable UUID orderId, @Valid @RequestBody SpotRequest req) {
        if (!orders.existsById(orderId)) return ResponseEntity.notFound().build();
        String rotation = req.rotationKind() == null ? "ANY_TIME" : req.rotationKind();
        if (!"ANY_TIME".equals(rotation) && !"SPECIFIC_SHOW".equals(rotation)) {
            return ResponseEntity.badRequest().body(Map.of("error", "rotationKind must be ANY_TIME or SPECIFIC_SHOW"));
        }
        Spot s = Spot.builder()
                .orderId(orderId)
                .label(req.label())
                .lengthSeconds(req.lengthSeconds())
                .librtimeFileId(req.librtimeFileId())
                .rotationKind(rotation)
                .targetShowId(req.targetShowId())
                .build();
        return ResponseEntity.ok(SpotDto.from(spots.save(s)));
    }

    @PutMapping("/api/spots/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody SpotRequest req) {
        var s = spots.findById(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();
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
        return ResponseEntity.ok(SpotDto.from(spots.save(s)));
    }

    @DeleteMapping("/api/spots/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!spots.existsById(id)) return ResponseEntity.notFound().build();
        spots.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
