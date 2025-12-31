package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.AssetType;
import com.onelpro.librelog.enums.MusicCategory;
import com.onelpro.librelog.enums.TimingType;
import com.onelpro.librelog.enums.TransitionCode;
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
 * BreakStructure entity defining commercial break positions and durations
 * within a clock template.
 */
@Entity
@Table(name = "break_structures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakStructure {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "clock_template_id", nullable = false)
	private ClockTemplate clockTemplate;

	@Column(nullable = false)
	private String name;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "duration_seconds", nullable = false)
	private Integer durationSeconds;

	@Column(name = "is_floating")
	private Boolean isFloating;

	@ManyToOne
	@JoinColumn(name = "avail_type_id")
	private AvailType availType;

	@Enumerated(EnumType.STRING)
	@Column(name = "timing_type")
	private TimingType timingType;

	@Enumerated(EnumType.STRING)
	@Column(name = "transition_code")
	private TransitionCode transitionCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "asset_type")
	private AssetType assetType;

	@Enumerated(EnumType.STRING)
	@Column(name = "music_category")
	private MusicCategory musicCategory;

	@Column(name = "show_segment_name")
	private String showSegmentName;

	// LibreTime integration fields
	@Column(name = "libretime_smart_block_id")
	private String libreTimeSmartBlockId;

	@Column(name = "libretime_playlist_id")
	private String libreTimePlaylistId;

	@Column(name = "break_content_type")
	private String breakContentType; // STATIC, DYNAMIC, PLAYLIST

	@Column(name = "break_criteria_json", columnDefinition = "TEXT")
	private String breakCriteriaJson; // JSON for smart block criteria

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

