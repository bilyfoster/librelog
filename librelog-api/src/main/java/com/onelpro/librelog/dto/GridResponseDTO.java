package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for grid response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GridResponseDTO {

	private UUID id;
	private String name;
	private UUID channelId;
	private String channelName;
	private String description;
	private Boolean isActive;
	private List<GridDaypartMappingResponseDTO> daypartMappings;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	/**
	 * DTO for grid daypart mapping response data.
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class GridDaypartMappingResponseDTO {
		private UUID id;
		private UUID daypartId;
		private String daypartName;
		private String daypartTimeRange;
		private Integer dayOfWeek; // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
		private String dayOfWeekName; // e.g., "Monday"
	}

}

