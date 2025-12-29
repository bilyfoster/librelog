package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for exporting clock templates to LibreTime format.
 * LibreTime uses a show instance format with scheduled items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibreTimeExportDTO {

	private String name;
	private String description;
	private List<LibreTimeShowInstance> showInstances;

	/**
	 * Represents a show instance in LibreTime format.
	 * A show instance is a scheduled time slot with items.
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class LibreTimeShowInstance {
		private String startTime; // ISO 8601 format
		private String endTime; // ISO 8601 format
		private String showName;

		@Builder.Default
		private List<LibreTimeItem> items = new ArrayList<>();
	}

	/**
	 * Represents an item (break, fixed asset, or command) in LibreTime format.
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class LibreTimeItem {
		private String type; // "break", "fixed_asset", "command"
		private String name;
		private String startTime; // Time offset from show start (HH:MM:SS)
		private Integer durationSeconds;
		private String assetType; // For fixed assets
		private String commandType; // For automation commands
		private String cueIn; // Cue in time for audio items
		private String cueOut; // Cue out time for audio items
		private String fadeIn; // Fade in duration
		private String fadeOut; // Fade out duration
		private String transition; // Transition code (segue, overlap, etc.)
	}

}

