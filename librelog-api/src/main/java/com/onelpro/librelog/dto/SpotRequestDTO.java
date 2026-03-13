package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.SpotStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for creating/updating a spot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotRequestDTO {

	@NotNull(message = "Campaign ID is required")
	private UUID campaignId;

	@NotNull(message = "Station ID is required")
	private UUID stationId;

	@NotNull(message = "Scheduled date is required")
	private LocalDate scheduledDate;

	@NotBlank(message = "Scheduled time is required")
	private String scheduledTime;

	@NotNull(message = "Spot length is required")
	private Integer spotLength;

	private SpotStatus status;

	private String daypart;

	private String breakName;

	private Integer breakPosition;

	private UUID assetId;

	private String assetName;

	private String notes;

}
