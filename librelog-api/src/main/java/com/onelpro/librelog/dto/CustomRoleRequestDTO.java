package com.onelpro.librelog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for creating or updating a custom role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomRoleRequestDTO {

	@NotBlank(message = "Role name is required")
	private String name;

	private String description;

	/**
	 * Permissions map defining module-level and action-level permissions.
	 * Format: Map<ModuleType, Set<ActionType>> or similar JSON structure.
	 * Example: {"ORDERS": ["VIEW", "CREATE"], "LOGS": ["VIEW"]}
	 */
	private Map<String, Object> permissions;

}

