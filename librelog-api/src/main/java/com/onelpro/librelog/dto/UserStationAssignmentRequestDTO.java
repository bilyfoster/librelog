package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * DTO for creating or updating a user-station assignment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStationAssignmentRequestDTO {

	@NotNull(message = "User ID is required")
	private UUID userId;

	@NotNull(message = "Station ID is required")
	private UUID stationId;

	@NotNull(message = "Permission level is required")
	private PermissionLevel permissionLevel;

	/**
	 * Custom permissions map for granular control when permissionLevel is CUSTOM.
	 * Format: Map<ModuleType, Set<ActionType>> or similar JSON structure.
	 */
	private Map<String, Object> customPermissions;

}

