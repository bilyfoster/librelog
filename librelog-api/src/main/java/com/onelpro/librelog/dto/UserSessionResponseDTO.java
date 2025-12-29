package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for user session response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionResponseDTO {

	private UUID id;
	private UUID userId;
	private String userEmail;
	private LocalDateTime loginTimestamp;
	private LocalDateTime lastActivityTimestamp;
	private String ipAddress;
	private String userAgent;
	private UUID currentStationId;
	private String currentStationName;
	private UUID currentResourceId;
	private Boolean isActive;
	private LocalDateTime expiresAt;

	/**
	 * Calculated session duration from login timestamp to now (or last activity).
	 */
	private Duration sessionDuration;

}

