package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.SpotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for spot responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotResponseDTO {

	private UUID id;
	private UUID campaignId;
	private String campaignName;
	private UUID stationId;
	private String stationName;
	private LocalDate scheduledDate;
	private String scheduledTime;
	private Integer spotLength;
	private SpotStatus status;
	private LocalDateTime actualAirTime;
	private String daypart;
	private String breakName;
	private Integer breakPosition;
	private UUID assetId;
	private String assetName;
	private String notes;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

}
