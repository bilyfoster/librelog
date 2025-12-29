package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.UserRole;
import com.onelpro.librelog.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for user response data.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

	private UUID id;
	private String email;
	private UserStatus status;
	private UserRole role;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	/**
	 * Optional summary of station assignments (only included when requested).
	 * Contains basic information about assigned stations.
	 */
	private List<StationAssignmentSummaryDTO> stationAssignmentsSummary;

	/**
	 * DTO for station assignment summary information.
	 */
	@Data
	@lombok.Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class StationAssignmentSummaryDTO {
		private UUID stationId;
		private String stationName;
		private String permissionLevel;
	}

}
