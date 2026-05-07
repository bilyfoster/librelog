package com.onelpro.librelog.playback;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "playback_log_entry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaybackLogEntry {

    @Id
    private UUID id;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    @Column(name = "librtime_file_id")
    private Long librtimeFileId;

    @Column(name = "length_seconds")
    private Integer lengthSeconds;

    @Column(columnDefinition = "TEXT")
    private String raw;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
