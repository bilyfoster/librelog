package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.TrackType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a track (audio file) in the system.
 */
@Entity
@Table(name = "tracks", indexes = {
        @Index(name = "ix_tracks_id", columnList = "id"),
        @Index(name = "ix_tracks_title", columnList = "title"),
        @Index(name = "ix_tracks_artist", columnList = "artist"),
        @Index(name = "ix_tracks_type", columnList = "type"),
        @Index(name = "ix_tracks_genre", columnList = "genre"),
        @Index(name = "ix_tracks_libretime_id", columnList = "libretime_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "artist", length = 255)
    private String artist;

    @Column(name = "album", length = 255)
    private String album;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private TrackType type;

    @Column(name = "genre", length = 100)
    private String genre;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "filepath", nullable = false, columnDefinition = "TEXT")
    private String filepath;

    @Column(name = "libretime_id", length = 50, unique = true)
    private String libretimeId;

    @Column(name = "last_played")
    private Instant lastPlayed;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

