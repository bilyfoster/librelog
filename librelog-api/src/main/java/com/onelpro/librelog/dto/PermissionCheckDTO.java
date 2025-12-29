package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.ActionType;
import com.onelpro.librelog.enums.ModuleType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for permission check request and response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCheckDTO {

	@NotNull(message = "User ID is required")
	private UUID userId;

	/**
	 * Optional station ID for property-based permission checks.
	 */
	private UUID stationId;

	@NotNull(message = "Module type is required")
	private ModuleType moduleType;

	@NotNull(message = "Action type is required")
	private ActionType actionType;

	/**
	 * Result of the permission check (true if user has permission, false otherwise).
	 */
	private Boolean result;

}

