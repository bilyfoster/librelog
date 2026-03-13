package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.TrackType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a track/audio file in the music library.
 */
@Entity
@Table(name = "tracks", indexes = {
		@Index(name = "ix_tracks_title", columnList = "title"),
		@Index(name = "ix_tracks_artist", columnList = "artist"),
		@Index(name = "ix_tracks_type", columnList = "type"),
		@Index(name = "ix_tracks_station", columnList = "station_id"),
		@Index(name = "ix_tracks_libretime_id", columnList = "libretime_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Track {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "artist")
	private String artist;

	@Column(name = "album")
	private String album;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	@Builder.Default
	private TrackType type = TrackType.MUSIC;

	@Column(name = "genre")
	private String genre;

	@Column(name = "duration_seconds")
	private Integer durationSeconds;

	@Column(name = "filepath")
	private String filepath;

	@Column(name = "libretime_id")
	private String libretimeId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "station_id")
	private Station station;

	@Column(name = "isrc")
	private String isrc;

	@Column(name = "year")
	private Integer year;

	@Column(name = "bpm")
	private Integer bpm;

	@Column(name = "rating")
	private Integer rating;

	@Column(name = "play_count")
	@Builder.Default
	private Integer playCount = 0;

	@Column(name = "last_played")
	private LocalDateTime lastPlayed;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}
