package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for clock template response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClockTemplateResponseDTO {
	private UUID id;
	private UUID channelId;
	private String channelName;
	private UUID stationId;
	private String stationCallSign;
	private String stationName;
	private String name;
	private String description;
	private Boolean isActive;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

