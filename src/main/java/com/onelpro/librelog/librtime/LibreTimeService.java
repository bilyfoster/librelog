package com.onelpro.librelog.librtime;

import com.onelpro.librelog.config.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Builds {@link LibreTimeClient}s on demand from the stored per-station connection,
 * decrypts API keys, and writes back test results.
 */
@Service
@RequiredArgsConstructor
public class LibreTimeService {

    private final LibreTimeConnectionRepository connections;
    private final EncryptionService encryption;

    public Optional<LibreTimeConnection> findConnection(UUID stationId) {
        return connections.findById(stationId);
    }

    public LibreTimeConnection saveConnection(UUID stationId, String baseUrl, String apiKey) {
        LibreTimeConnection conn = connections.findById(stationId)
                .orElseGet(() -> LibreTimeConnection.builder().stationId(stationId).build());
        conn.setBaseUrl(baseUrl);
        if (apiKey != null && !apiKey.isBlank()) {
            conn.setApiKeyEncrypted(encryption.encrypt(apiKey));
        }
        return connections.save(conn);
    }

    public LibreTimeClient.TestResult test(UUID stationId) {
        var conn = connections.findById(stationId).orElse(null);
        if (conn == null) return new LibreTimeClient.TestResult(false, "No connection configured");
        var result = clientFor(conn).test();
        conn.setLastTestedAt(Instant.now());
        conn.setLastTestOk(result.ok());
        conn.setLastTestMessage(result.message());
        connections.save(conn);
        return result;
    }

    public LibreTimeClient clientFor(UUID stationId) {
        var conn = connections.findById(stationId)
                .orElseThrow(() -> new IllegalStateException("LibreTime connection not configured for station " + stationId));
        return clientFor(conn);
    }

    private LibreTimeClient clientFor(LibreTimeConnection conn) {
        String apiKey = encryption.decrypt(conn.getApiKeyEncrypted());
        return new LibreTimeClient(conn.getBaseUrl(), apiKey);
    }
}
