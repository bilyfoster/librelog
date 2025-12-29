package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.StationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for station response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationResponseDTO {
	private UUID id;
	private UUID organizationId;
	private String organizationName;
	private UUID marketId;
	private String marketName;
	private UUID clusterId;
	private String clusterName;
	private String callSign;
	private String name;
	private String frequency;
	private StationType stationType;
	private Boolean isActive;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

