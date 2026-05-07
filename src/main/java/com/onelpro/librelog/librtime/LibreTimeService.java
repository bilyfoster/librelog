package com.onelpro.librelog.librtime;

import com.onelpro.librelog.config.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Builds {@link LibreTimeClient}s on demand from the stored per-station connection,
 * decrypts the password, and writes back test results.
 */
@Service
@RequiredArgsConstructor
public class LibreTimeService {

    private final LibreTimeConnectionRepository connections;
    private final EncryptionService encryption;

    public Optional<LibreTimeConnection> findConnection(UUID stationId) {
        return connections.findById(stationId);
    }

    public LibreTimeConnection saveConnection(UUID stationId, String baseUrl, String username, String password) {
        LibreTimeConnection conn = connections.findById(stationId)
                .orElseGet(() -> LibreTimeConnection.builder().stationId(stationId).build());
        conn.setBaseUrl(baseUrl);
        if (username != null) conn.setUsername(username);
        if (password != null && !password.isBlank()) {
            conn.setApiKeyEncrypted(encryption.encrypt(password));
        } else if (conn.getApiKeyEncrypted() == null) {
            // first save with no password yet - column is NOT NULL, store an empty placeholder
            conn.setApiKeyEncrypted(encryption.encrypt(""));
        }
        return connections.save(conn);
    }

    public LibreTimeClient.TestResult test(UUID stationId) {
        var conn = connections.findById(stationId).orElse(null);
        if (conn == null) return new LibreTimeClient.TestResult(false, "No connection configured");
        if (conn.getUsername() == null || conn.getUsername().isBlank()) {
            return new LibreTimeClient.TestResult(false, "Username is required");
        }
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
        String password = encryption.decrypt(conn.getApiKeyEncrypted());
        return new LibreTimeClient(conn.getBaseUrl(), conn.getUsername(), password);
    }
}
