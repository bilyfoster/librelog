package com.onelpro.librelog.librtime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "librtime_connection")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibreTimeConnection {

    @Id
    @Column(name = "station_id")
    private UUID stationId;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    /** LibreTime username for HTTP Basic auth. */
    @Column(name = "username")
    private String username;

    /**
     * Encrypted LibreTime password (used as the secret half of HTTP Basic auth).
     * Column is named "api_key_encrypted" for backward compatibility with the initial
     * schema; semantically it stores the password.
     */
    @Column(name = "api_key_encrypted", nullable = false, columnDefinition = "TEXT")
    private String apiKeyEncrypted;

    @Column(name = "last_tested_at")
    private Instant lastTestedAt;

    @Column(name = "last_test_ok")
    private Boolean lastTestOk;

    @Column(name = "last_test_message", columnDefinition = "TEXT")
    private String lastTestMessage;
}
