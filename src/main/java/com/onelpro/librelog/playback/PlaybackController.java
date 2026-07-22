package com.onelpro.librelog.playback;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PlaybackController {

    private final PlaybackService service;

    @PostMapping("/api/stations/{stationId}/playback/import")
    public ResponseEntity<?> importDay(@PathVariable UUID stationId,
                                        @RequestParam("date") String date) {
        try {
            var r = service.importDay(stationId, LocalDate.parse(date));
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/stations/{stationId}/playback")
    public ResponseEntity<?> listDay(@PathVariable UUID stationId,
                                     @RequestParam("date") String date) {
        try {
            return ResponseEntity.ok(service.listDay(stationId, LocalDate.parse(date)));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/orders/{orderId}/reconciliation")
    public ResponseEntity<?> orderReconciliation(@PathVariable UUID orderId) {
        try {
            return ResponseEntity.ok(service.orderSummary(orderId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/api/playback/fulfillment")
    public ResponseEntity<?> fulfillment(@RequestParam("stationId") UUID stationId,
                                         @RequestParam("date") String date) {
        try {
            return ResponseEntity.ok(service.fulfillment(stationId, LocalDate.parse(date)));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        }
    }
}
