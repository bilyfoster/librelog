package com.onelpro.librelog.librtime;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
        return listShowInstances(date, ZoneOffset.UTC);
    }

    /**
     * LibreTime 4.5's {@code /api/v2/show-instances} viewset has no FilterSet, so the
     * {@code starts_after}/{@code starts_before} query params are ignored and every
     * instance is returned. We still send them (some forks honor them) but always
     * filter client-side for the [date 00:00, date+1 00:00) window in the supplied
     * station {@link ZoneId}.
     */
    public List<JsonNode> listShowInstances(LocalDate date, ZoneId zone) {
        ZoneId z = zone == null ? ZoneOffset.UTC : zone;
        ZonedDateTime startZdt = date.atStartOfDay(z);
        ZonedDateTime endZdt = startZdt.plusDays(1);
        Instant startInstant = startZdt.toInstant();
        Instant endInstant = endZdt.toInstant();

        String start = encIsoInstant(startInstant.toString());
        String end = encIsoInstant(endInstant.toString());
        String uri = "/api/v2/show-instances?starts_after=" + start + "&starts_before=" + end;
        List<JsonNode> raw = getList(uri);

        List<JsonNode> filtered = new ArrayList<>(raw.size());
        for (JsonNode n : raw) {
            Instant t = parseInstant(textOrNull(n, "starts_at"));
            if (t == null) continue;
            // Half-open window: [startInstant, endInstant)
            if (!t.isBefore(startInstant) && t.isBefore(endInstant)) {
                filtered.add(n);
            }
        }
        return filtered;
    }

    private static String textOrNull(JsonNode n, String f) {
        return n != null && n.has(f) && !n.get(f).isNull() ? n.get(f).asText() : null;
    }

    private static Instant parseInstant(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Instant.parse(s);
        } catch (Exception e) {
            try {
                // fallback: naive ISO local strings, treat as UTC
                return java.time.LocalDateTime.parse(s).toInstant(ZoneOffset.UTC);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    public List<JsonNode> listSchedule(LocalDate date) {
        String start = encIsoInstant(date.atStartOfDay(ZoneOffset.UTC).toInstant().toString());
        String end = encIsoInstant(date.atStartOfDay(ZoneOffset.UTC).plusDays(1).toInstant().toString());
        String uri = "/api/v2/schedule?starts_after=" + start + "&starts_before=" + end;
        return getList(uri);
    }

    public List<JsonNode> listScheduleForInstance(long instanceId) {
        return getList("/api/v2/schedule?instance=" + instanceId);
    }

    /** Fetch a single show instance to learn its starts_at / ends_at. */
    public JsonNode getShowInstance(long instanceId) {
        return getJson("/api/v2/show-instances/" + instanceId);
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
        String start = encIsoInstant(date.atStartOfDay(ZoneOffset.UTC).toInstant().toString());
        String end = encIsoInstant(date.atStartOfDay(ZoneOffset.UTC).plusDays(1).toInstant().toString());
        String uri = "/api/v2/playout-history?starts_after=" + start + "&starts_before=" + end;
        return getList(uri);
    }

    /**
     * Schedule a single file inside a show instance.
     *
     * <p>The LibreTime {@code /api/v2/schedule} POST (in 4.5.x) requires
     * {@code instance}, {@code file}, {@code position}, {@code starts_at},
     * {@code ends_at}, {@code cue_in}, {@code cue_out}, and {@code broadcasted}.
     * The caller is responsible for computing absolute {@code startsAt} (typically
     * the show instance start plus accumulated lengths) and
     * {@code endsAt = startsAt + lengthSeconds}.</p>
     *
     * <p>Returns the created Schedule object.</p>
     */
    public JsonNode scheduleFileInInstance(long instanceId, long fileId, int position,
                                           Instant startsAt, Instant endsAt,
                                           int lengthSeconds) {
        return scheduleFileInInstance(instanceId, fileId, position, startsAt, endsAt,
                lengthSeconds, null, null);
    }

    /**
     * Overload carrying the PRD §6.3 segue/duck markers. A positive
     * {@code segueOffsetSeconds} becomes the tail crossfade ({@code fade_out});
     * LibreTime's schedule has no duck-gain field, so {@code duckDb} is persisted in
     * LibreLog only and not sent.
     */
    public JsonNode scheduleFileInInstance(long instanceId, long fileId, int position,
                                           Instant startsAt, Instant endsAt,
                                           int lengthSeconds,
                                           Integer segueOffsetSeconds, java.math.BigDecimal duckDb) {
        java.util.LinkedHashMap<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("instance", instanceId);
        body.put("file", fileId);
        body.put("position", position);
        body.put("starts_at", startsAt.toString());
        body.put("ends_at", endsAt.toString());
        body.put("cue_in", "00:00:00");
        body.put("cue_out", formatHms(Math.max(0, lengthSeconds)));
        body.put("fade_in", "00:00:00.500");
        body.put("fade_out", segueOffsetSeconds != null && segueOffsetSeconds > 0
                ? formatHms(segueOffsetSeconds) : "00:00:00.500");
        body.put("broadcasted", 0);
        body.put("playout_status", 1);
        return web.post().uri("/api/v2/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(30));
    }

    private static String formatHms(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /** Delete every existing schedule item for a show instance (so we can re-push). */
    public void clearScheduleForInstance(long instanceId) {
        // /api/v2/schedule supports filtering by query, but DELETE is per-id.
        var existing = listScheduleForInstance(instanceId);
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

    /** Safety cap so a misbehaving {@code next} chain can't loop forever. */
    private static final int MAX_PAGES = 1000;

    /**
     * Fetch a list, following Django REST Framework pagination. LibreTime returns
     * {@code {count, next, previous, results}}; {@code next} is an absolute URL to the
     * following page (or null on the last page). Earlier this read only the first page,
     * which truncated the library, show instances, and playout history. We now walk
     * every page via {@code next}. Plain-array and single-object responses are still
     * handled for forks that don't paginate.
     */
    private List<JsonNode> getList(String uri) {
        java.util.ArrayList<JsonNode> out = new java.util.ArrayList<>();
        String next = uri;
        int pages = 0;
        while (next != null && pages++ < MAX_PAGES) {
            JsonNode body = getJson(next);
            if (body == null) break;
            if (body.isArray()) {
                body.forEach(out::add);
                return out;
            }
            if (body.has("results") && body.get("results").isArray()) {
                body.get("results").forEach(out::add);
                next = (body.has("next") && !body.get("next").isNull())
                        ? toPathAndQuery(body.get("next").asText())
                        : null;
                continue;
            }
            out.add(body);
            return out;
        }
        return out;
    }

    /**
     * Reduce an absolute {@code next} URL to path?query so it re-resolves against our
     * configured baseUrl. LibreTime behind a reverse proxy often returns an internal
     * scheme/host in {@code next} that wouldn't be reachable as-is.
     */
    private static String toPathAndQuery(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            java.net.URI u = java.net.URI.create(url);
            if (u.getRawPath() == null) return url;
            return u.getRawQuery() == null ? u.getRawPath() : u.getRawPath() + "?" + u.getRawQuery();
        } catch (Exception e) {
            return url;
        }
    }

    private static String stripTrailingSlash(String s) {
        if (s == null) return null;
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String errorMessage(Throwable t) {
        if (t instanceof WebClientResponseException w) {
            String body = w.getResponseBodyAsString();
            if (body != null && !body.isBlank() && body.length() < 600) {
                return "HTTP " + w.getStatusCode().value() + ": " + body;
            }
            return "HTTP " + w.getStatusCode().value() + " from LibreTime";
        }
        Throwable cause = t.getCause();
        if (cause != null && cause.getMessage() != null) return cause.getMessage();
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }

    public record TestResult(boolean ok, String message) {}

    private static String encIsoInstant(String iso) {
        return URLEncoder.encode(iso, StandardCharsets.UTF_8);
    }
}
