package com.onelpro.librelog.librtime;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/stations/{stationId}/librtime")
@RequiredArgsConstructor
public class LibreTimeController {

    private final LibreTimeService service;

    public record ConnectionDto(String baseUrl, boolean configured, Instant lastTestedAt, Boolean lastTestOk, String lastTestMessage) {}
    public record SaveConnectionRequest(@NotBlank String baseUrl, String apiKey) {}

    @GetMapping("/connection")
    public ResponseEntity<ConnectionDto> getConnection(@PathVariable UUID stationId) {
        return service.findConnection(stationId)
                .map(c -> new ConnectionDto(c.getBaseUrl(), c.getApiKeyEncrypted() != null,
                        c.getLastTestedAt(), c.getLastTestOk(), c.getLastTestMessage()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(new ConnectionDto(null, false, null, null, null)));
    }

    @PutMapping("/connection")
    public ConnectionDto saveConnection(@PathVariable UUID stationId, @Valid @RequestBody SaveConnectionRequest req) {
        var c = service.saveConnection(stationId, req.baseUrl(), req.apiKey());
        return new ConnectionDto(c.getBaseUrl(), c.getApiKeyEncrypted() != null,
                c.getLastTestedAt(), c.getLastTestOk(), c.getLastTestMessage());
    }

    @PostMapping("/connection/test")
    public Map<String, Object> testConnection(@PathVariable UUID stationId) {
        var r = service.test(stationId);
        return Map.of("ok", r.ok(), "message", r.message());
    }

    // Phase 2: read-only browse endpoints
    @GetMapping("/library")
    public List<JsonNode> library(@PathVariable UUID stationId,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(defaultValue = "100") int limit) {
        return service.clientFor(stationId).searchFiles(q, limit);
    }

    @GetMapping("/shows")
    public List<JsonNode> shows(@PathVariable UUID stationId) {
        return service.clientFor(stationId).listShows();
    }

    @GetMapping("/show-instances")
    public List<JsonNode> showInstances(@PathVariable UUID stationId,
                                        @RequestParam("date") String date) {
        return service.clientFor(stationId).listShowInstances(LocalDate.parse(date));
    }

    @GetMapping("/templates")
    public Map<String, List<JsonNode>> templates(@PathVariable UUID stationId) {
        var c = service.clientFor(stationId);
        return Map.of(
                "smartBlocks", c.listSmartBlocks(),
                "playlists", c.listPlaylists());
    }
}
