package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.AssetType;
import com.onelpro.librelog.enums.MusicCategory;
import com.onelpro.librelog.enums.TimingType;
import com.onelpro.librelog.enums.TransitionCode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for break structure creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakStructureRequestDTO {

	@NotNull(message = "Clock template ID is required")
	private UUID clockTemplateId;

	@NotBlank(message = "Name is required")
	private String name;

	@NotNull(message = "Start time is required")
	private LocalTime startTime;

	@NotNull(message = "Duration in seconds is required")
	@Min(value = 1, message = "Duration must be at least 1 second")
	private Integer durationSeconds;

	private Boolean isFloating;

	private UUID availTypeId;

	private TimingType timingType;

	private TransitionCode transitionCode;

	private AssetType assetType;

	private MusicCategory musicCategory;

	private String showSegmentName;

	// LibreTime integration fields
	private String libreTimeSmartBlockId;
	private String libreTimePlaylistId;
	private String breakContentType; // STATIC, DYNAMIC, PLAYLIST
	private String breakCriteriaJson; // JSON for smart block criteria

}

