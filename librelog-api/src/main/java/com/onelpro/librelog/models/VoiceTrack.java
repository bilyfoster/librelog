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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Entity representing a voice track (DJ recorded segment) in the system.
 * Voice tracks are used for intros, outros, transitions, and show segments.
 */
@Entity
@Table(name = "voice_tracks", indexes = {
		@Index(name = "ix_voice_tracks_station", columnList = "station_id"),
		@Index(name = "ix_voice_tracks_scheduled_date", columnList = "scheduled_date"),
		@Index(name = "ix_voice_tracks_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceTrack {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "title", nullable = false)
	private String title;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "station_id", nullable = false)
	private Station station;

	@Column(name = "show_name")
	private String showName;

	@Column(name = "segment_type")
	private String segmentType; // INTRO, OUTRO, TRANSITION, LINER, TEASER

	@Column(name = "file_url", columnDefinition = "TEXT")
	private String fileUrl;

	@Column(name = "file_path")
	private String filePath;

	@Column(name = "duration_seconds")
	private Integer durationSeconds;

	@Column(name = "scheduled_date")
	private LocalDate scheduledDate;

	@Column(name = "scheduled_time")
	private LocalTime scheduledTime;

	@Column(name = "script_text", columnDefinition = "TEXT")
	private String scriptText;

	@Column(name = "recorded_text", columnDefinition = "TEXT")
	private String recordedText;

	@Column(name = "status")
	@Builder.Default
	private String status = "DRAFT"; // DRAFT, RECORDED, APPROVED, SCHEDULED, AIRED

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recorded_by")
	private User recordedBy;

	@Column(name = "recorded_at")
	private LocalDateTime recordedAt;

	/**
	 * Reference to the song/track that plays BEFORE this voice track.
	 * Provides context for the DJ when recording.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "song_before_id")
	private Track songBefore;

	/**
	 * Reference to the song/track that plays AFTER this voice track.
	 * Provides context for the DJ when recording.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "song_after_id")
	private Track songAfter;

	/**
	 * Denormalized title of the song before (for display without join).
	 */
	@Column(name = "song_before_title")
	private String songBeforeTitle;

	/**
	 * Denormalized title of the song after (for display without join).
	 */
	@Column(name = "song_after_title")
	private String songAfterTitle;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}
