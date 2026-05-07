package com.onelpro.librelog.librtime;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/stations/{stationId}/librtime")
@RequiredArgsConstructor
public class LibreTimeController {

    private final LibreTimeService service;

    public record ConnectionDto(String baseUrl, String username, boolean configured,
                                Instant lastTestedAt, Boolean lastTestOk, String lastTestMessage) {}

    public record SaveConnectionRequest(@NotBlank String baseUrl, String username, String password) {}

    public record FileDto(long id, String name, String length, String mime, Long size, String filepath) {}

    public record ShowInstanceDto(long id, String showName, Long showId,
                                  String startsAt, String endsAt,
                                  String description, String filledTime, boolean modified) {}

    public record TemplateDto(long id, String name, String description, String type) {}

    @GetMapping("/connection")
    public ResponseEntity<ConnectionDto> getConnection(@PathVariable UUID stationId) {
        return service.findConnection(stationId)
                .map(c -> new ConnectionDto(c.getBaseUrl(), c.getUsername(),
                        c.getApiKeyEncrypted() != null && !c.getApiKeyEncrypted().isEmpty(),
                        c.getLastTestedAt(), c.getLastTestOk(), c.getLastTestMessage()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(new ConnectionDto(null, null, false, null, null, null)));
    }

    @PutMapping("/connection")
    public ConnectionDto saveConnection(@PathVariable UUID stationId,
                                        @Valid @RequestBody SaveConnectionRequest req) {
        var c = service.saveConnection(stationId, req.baseUrl(), req.username(), req.password());
        return new ConnectionDto(c.getBaseUrl(), c.getUsername(),
                c.getApiKeyEncrypted() != null && !c.getApiKeyEncrypted().isEmpty(),
                c.getLastTestedAt(), c.getLastTestOk(), c.getLastTestMessage());
    }

    @PostMapping("/connection/test")
    public Map<String, Object> testConnection(@PathVariable UUID stationId) {
        var r = service.test(stationId);
        return Map.of("ok", r.ok(), "message", r.message());
    }

    /**
     * GET /library?q=&limit=  — returns a normalized {@link FileDto} list.
     * The LibreTime files endpoint doesn't have a generic search; we filter by
     * substring match on the name field client-side.
     */
    @GetMapping("/library")
    public ResponseEntity<?> library(@PathVariable UUID stationId,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(defaultValue = "200") int limit) {
        try {
            var raw = service.clientFor(stationId).listFiles(null);
            String needle = q == null ? null : q.toLowerCase();
            List<FileDto> out = new ArrayList<>();
            for (JsonNode n : raw) {
                if (out.size() >= limit) break;
                String name = textOrNull(n, "name");
                if (needle != null) {
                    String hay = (name + " " + textOrNull(n, "filepath") + " " + textOrNull(n, "description"))
                            .toLowerCase();
                    if (!hay.contains(needle)) continue;
                }
                out.add(new FileDto(
                        n.has("id") ? n.get("id").asLong() : 0L,
                        name,
                        textOrNull(n, "length"),
                        textOrNull(n, "mime"),
                        n.has("size") && !n.get("size").isNull() ? n.get("size").asLong() : null,
                        textOrNull(n, "filepath")));
            }
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "LibreTime: " + rootMessage(e)));
        }
    }

    @GetMapping("/shows")
    public ResponseEntity<?> shows(@PathVariable UUID stationId) {
        try {
            return ResponseEntity.ok(service.clientFor(stationId).listShows());
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "LibreTime: " + rootMessage(e)));
        }
    }

    @GetMapping("/show-instances")
    public ResponseEntity<?> showInstances(@PathVariable UUID stationId,
                                           @RequestParam("date") String date) {
        try {
            var client = service.clientFor(stationId);
            var instances = client.listShowInstances(LocalDate.parse(date));
            // Build show id -> name map so the UI gets human-friendly names.
            Map<Long, String> showNames = new HashMap<>();
            for (JsonNode s : client.listShows()) {
                if (s.has("id") && s.has("name")) {
                    showNames.put(s.get("id").asLong(), s.get("name").asText());
                }
            }
            List<ShowInstanceDto> out = new ArrayList<>();
            for (JsonNode i : instances) {
                long showId = i.has("show") ? i.get("show").asLong() : 0L;
                out.add(new ShowInstanceDto(
                        i.has("id") ? i.get("id").asLong() : 0L,
                        showNames.getOrDefault(showId, "Show #" + showId),
                        showId,
                        textOrNull(i, "starts_at"),
                        textOrNull(i, "ends_at"),
                        textOrNull(i, "description"),
                        textOrNull(i, "filled_time"),
                        i.has("modified") && i.get("modified").asBoolean()));
            }
            out.sort(Comparator.comparing(d -> d.startsAt() == null ? "" : d.startsAt()));
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "LibreTime: " + rootMessage(e)));
        }
    }

    @GetMapping("/templates")
    public ResponseEntity<?> templates(@PathVariable UUID stationId) {
        try {
            var c = service.clientFor(stationId);
            List<TemplateDto> all = new ArrayList<>();
            for (JsonNode n : c.listSmartBlocks()) all.add(toTemplate(n, "smart-block"));
            for (JsonNode n : c.listPlaylists()) all.add(toTemplate(n, "playlist"));
            return ResponseEntity.ok(all);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "LibreTime: " + rootMessage(e)));
        }
    }

    private static TemplateDto toTemplate(JsonNode n, String type) {
        return new TemplateDto(
                n.has("id") ? n.get("id").asLong() : 0L,
                textOrNull(n, "name"),
                textOrNull(n, "description"),
                type);
    }

    private static String textOrNull(JsonNode n, String f) {
        return n.has(f) && !n.get(f).isNull() ? n.get(f).asText() : null;
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        return c.getMessage() == null ? c.getClass().getSimpleName() : c.getMessage();
    }
}
