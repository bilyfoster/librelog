package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO representing the structure for exporting a clock template to WideOrbit format.
 * This format is designed to be compatible with WideOrbit's clock template structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WideOrbitExportDTO {
	private UUID id;
	private String name;
	private String description;
	private String channelName;
	private String stationCallSign;
	private List<WideOrbitElementDTO> elements;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class WideOrbitElementDTO {
		private String type; // BREAK, FIXED_ASSET, AUTOMATION_COMMAND
		private String name;
		private LocalTime startTime;
		private Integer durationSeconds; // For breaks and fixed assets
		private String assetType; // IM, ID, CM, PR, VT, SH
		private String musicCategory; // S1, S2, S3
		private String showSegmentName; // SH_MORNING_SEG1, etc.
		private String assetIdentifier; // For fixed assets
		private String commandType; // For automation commands
		private Map<String, Object> parameters; // For automation command parameters
		private String timingType; // HARD_START, SOFT_START
		private String transitionCode; // SEGUE, OVERLAP, HARD_START
		private String availType; // For breaks
	}
}

