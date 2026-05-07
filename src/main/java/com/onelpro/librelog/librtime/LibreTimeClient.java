package com.onelpro.librelog.librtime;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Adapter for the LibreTime v2 REST API.
 *
 * <p>LibreTime supports HTTP Basic auth and session cookies. We use Basic auth with the
 * configured username and password. Endpoint paths are documented in
 * {@code /api/v2/schema} on a live LibreTime instance.</p>
 *
 * <p>Built per-call from a {@link LibreTimeConnection} so each station may point at a
 * different LibreTime instance.</p>
 */
public class LibreTimeClient {

    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final WebClient web;

    public LibreTimeClient(String baseUrl, String username, String password) {
        WebClient.Builder b = WebClient.builder()
                .baseUrl(stripTrailingSlash(baseUrl))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024));
        if (username != null && !username.isBlank()) {
            String creds = username + ":" + (password == null ? "" : password);
            String basic = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
            b = b.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic);
        }
        this.web = b.build();
    }

    public TestResult test() {
        try {
            JsonNode body = getJson("/api/v2/version");
            String v = body != null && body.has("api_version") ? body.get("api_version").asText() : "unknown";
            // /version is anonymous - prove auth works by hitting an authenticated endpoint
            try {
                getJson("/api/v2/shows");
                return new TestResult(true, "LibreTime API v" + v);
            } catch (WebClientResponseException e) {
                if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                    return new TestResult(false, "LibreTime reachable (v" + v + ") but credentials rejected ("
                            + e.getStatusCode().value() + ")");
                }
                throw e;
            }
        } catch (Exception e) {
            return new TestResult(false, errorMessage(e));
        }
    }

    public List<JsonNode> listShows() {
        return getList("/api/v2/shows");
    }

    public List<JsonNode> listShowInstances(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        String uri = "/api/v2/show-instances?starts_after=" + ISO_LOCAL.format(start)
                + "&starts_before=" + ISO_LOCAL.format(end);
        return getList(uri);
    }

    public List<JsonNode> listSchedule(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        String uri = "/api/v2/schedule?starts_after=" + ISO_LOCAL.format(start)
                + "&starts_before=" + ISO_LOCAL.format(end);
        return getList(uri);
    }

    public List<JsonNode> listFiles(String genre) {
        StringBuilder sb = new StringBuilder("/api/v2/files");
        if (genre != null && !genre.isBlank()) {
            sb.append("?genre=").append(java.net.URLEncoder.encode(genre, StandardCharsets.UTF_8));
        }
        return getList(sb.toString());
    }

    public List<JsonNode> listSmartBlocks() {
        return getList("/api/v2/smart-blocks");
    }

    public List<JsonNode> listPlaylists() {
        return getList("/api/v2/playlists");
    }

    public List<JsonNode> playoutHistory(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        String uri = "/api/v2/playout-history?starts_after=" + ISO_LOCAL.format(start)
                + "&starts_before=" + ISO_LOCAL.format(end);
        return getList(uri);
    }

    /**
     * Schedule a single file inside a show instance.
     *
     * <p>The LibreTime {@code /api/v2/schedule} POST requires an {@code instance}
     * (show instance id) and a {@code file} (file id) plus position/cue fields.
     * Returns the created Schedule object.</p>
     */
    public JsonNode scheduleFileInInstance(long instanceId, long fileId, int position) {
        Map<String, Object> body = Map.of(
                "instance", instanceId,
                "file", fileId,
                "position", position,
                "cue_in", "00:00:00",
                "cue_out", "00:00:00",
                "fade_in", "00:00:00.500",
                "fade_out", "00:00:00.500");
        return web.post().uri("/api/v2/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(30));
    }

    /** Delete every existing schedule item for a show instance (so we can re-push). */
    public void clearScheduleForInstance(long instanceId) {
        // /api/v2/schedule supports filtering by query, but DELETE is per-id.
        var existing = getList("/api/v2/schedule?instance=" + instanceId);
        for (JsonNode n : existing) {
            if (!n.has("id")) continue;
            long id = n.get("id").asLong();
            try {
                web.delete().uri("/api/v2/schedule/" + id)
                        .retrieve()
                        .toBodilessEntity()
                        .block(Duration.ofSeconds(15));
            } catch (Exception ignored) {
                // best-effort
            }
        }
    }

    private JsonNode getJson(String uri) {
        return web.get().uri(uri).retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(15));
    }

    private List<JsonNode> getList(String uri) {
        JsonNode body = getJson(uri);
        if (body == null) return List.of();
        if (body.isArray()) {
            java.util.ArrayList<JsonNode> out = new java.util.ArrayList<>(body.size());
            body.forEach(out::add);
            return out;
        }
        if (body.has("results") && body.get("results").isArray()) {
            java.util.ArrayList<JsonNode> out = new java.util.ArrayList<>();
            body.get("results").forEach(out::add);
            return out;
        }
        return List.of(body);
    }

    private static String stripTrailingSlash(String s) {
        if (s == null) return null;
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String errorMessage(Throwable t) {
        if (t instanceof WebClientResponseException w) {
            return "HTTP " + w.getStatusCode().value() + " from LibreTime";
        }
        Throwable cause = t.getCause();
        if (cause != null && cause.getMessage() != null) return cause.getMessage();
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }

    public record TestResult(boolean ok, String message) {}
}
