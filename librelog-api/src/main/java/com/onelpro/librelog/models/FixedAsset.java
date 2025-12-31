package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.AssetType;
import com.onelpro.librelog.enums.MusicCategory;
import com.onelpro.librelog.enums.TimingType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * FixedAsset entity representing static carts (fixed audio elements).
 * These are audio files that play at the exact same time every day,
 * such as Legal IDs at :00, News Intros, Station IDs, etc.
 */
@Entity
@Table(name = "fixed_assets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedAsset {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "clock_template_id", nullable = false)
	private ClockTemplate clockTemplate;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "asset_type", nullable = false)
	private AssetType assetType;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "asset_identifier")
	private String assetIdentifier;

	@Enumerated(EnumType.STRING)
	@Column(name = "timing_type", nullable = false)
	private TimingType timingType;

	@Enumerated(EnumType.STRING)
	@Column(name = "music_category")
	private MusicCategory musicCategory;

	@Column(name = "show_segment_name")
	private String showSegmentName;

	@Column(name = "duration_seconds")
	private Integer durationSeconds;

	@Column(name = "content_type")
	private String contentType; // MUSIC, TALK, INTERVIEW, MIXED, ADVERT, etc.

	// LibreTime integration fields
	@Column(name = "libretime_cart_id")
	private String libreTimeCartId;

	@Column(name = "cue_in_ms")
	private Integer cueInMs;

	@Column(name = "cue_out_ms")
	private Integer cueOutMs;

	@Column(name = "fade_in_ms")
	private Integer fadeInMs;

	@Column(name = "fade_out_ms")
	private Integer fadeOutMs;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

