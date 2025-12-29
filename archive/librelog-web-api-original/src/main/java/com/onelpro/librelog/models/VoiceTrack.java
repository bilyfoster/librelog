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
 * Entity representing a voice track in the system.
 */
@Entity
@Table(name = "voice_tracks", indexes = {
        @Index(name = "ix_voice_tracks_id", columnList = "id"),
        @Index(name = "ix_voice_tracks_show_name", columnList = "show_name"),
        @Index(name = "ix_voice_tracks_scheduled_time", columnList = "scheduled_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "show_name", length = 255)
    private String showName;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "scheduled_time")
    private Instant scheduledTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "created_at")
    private Instant createdAt;
}

