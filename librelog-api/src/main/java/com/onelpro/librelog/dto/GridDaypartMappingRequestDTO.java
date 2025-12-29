package com.onelpro.librelog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for grid daypart mapping creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GridDaypartMappingRequestDTO {

	@NotNull(message = "Grid ID is required")
	private UUID gridId;

	@NotNull(message = "Daypart ID is required")
	private UUID daypartId;

	@NotNull(message = "Day of week is required")
	@Min(value = 0, message = "Day of week must be between 0 (Sunday) and 6 (Saturday)")
	@Max(value = 6, message = "Day of week must be between 0 (Sunday) and 6 (Saturday)")
	private Integer dayOfWeek;

}

