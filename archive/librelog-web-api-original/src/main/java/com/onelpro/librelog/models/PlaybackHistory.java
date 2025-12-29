package com.onelpro.librelog.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing playback history in the system.
 */
@Entity
@Table(name = "playback_history", indexes = {
        @Index(name = "ix_playback_history_id", columnList = "id"),
        @Index(name = "ix_playback_history_track_id", columnList = "track_id"),
        @Index(name = "ix_playback_history_log_id", columnList = "log_id"),
        @Index(name = "ix_playback_history_played_at", columnList = "played_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private DailyLog log;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;
}

