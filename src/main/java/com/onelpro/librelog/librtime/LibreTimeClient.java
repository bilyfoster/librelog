package com.onelpro.librelog.librtime;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Thin adapter for the LibreTime REST API. Built per-call from a {@link LibreTimeConnection},
 * not as a singleton, because each station may point at a different LibreTime instance.
 */
public class LibreTimeClient {

    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final WebClient web;

    public LibreTimeClient(String baseUrl, String apiKey) {
        this.web = WebClient.builder()
                .baseUrl(stripTrailingSlash(baseUrl))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Api-Key " + apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    public TestResult test() {
        try {
            JsonNode body = web.get().uri("/api/version")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));
            String version = body != null && body.has("version") ? body.get("version").asText() : "unknown";
            return new TestResult(true, "LibreTime " + version);
        } catch (Exception e) {
            return new TestResult(false, e.getMessage());
        }
    }

    public List<JsonNode> listShows() {
        return getList("/api/show");
    }

    public List<JsonNode> listShowInstances(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        String uri = "/api/show-instances?starts__gte=" + ISO_LOCAL.format(start)
                + "&starts__lt=" + ISO_LOCAL.format(end);
        return getList(uri);
    }

    public List<JsonNode> searchFiles(String query, int limit) {
        StringBuilder sb = new StringBuilder("/api/file?limit=" + limit);
        if (query != null && !query.isBlank()) {
            sb.append("&q=").append(java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8));
        }
        return getList(sb.toString());
    }

    public List<JsonNode> listSmartBlocks() {
        return getList("/api/smart-block");
    }

    public List<JsonNode> listPlaylists() {
        return getList("/api/playlist");
    }

    public List<JsonNode> playoutHistory(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        String uri = "/api/playouthistory?starts__gte=" + ISO_LOCAL.format(start)
                + "&starts__lt=" + ISO_LOCAL.format(end);
        return getList(uri);
    }

    /**
     * Push a list of items into a show instance. Each item is a small map suitable for
     * LibreTime's schedule endpoint; the exact payload contract is intentionally tolerant.
     */
    public JsonNode pushShowItems(long showInstanceId, List<Map<String, Object>> items) {
        return web.put().uri("/api/show-instances/" + showInstanceId + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("items", items))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(30));
    }

    private List<JsonNode> getList(String uri) {
        JsonNode body = web.get().uri(uri).retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(15));
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

    public record TestResult(boolean ok, String message) {}
}
