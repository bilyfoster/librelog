package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for custom role response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomRoleResponseDTO {

	private UUID id;
	private String name;
	private String description;
	private Map<String, Object> permissions;
	private UUID createdByUserId;
	private String createdByUserEmail;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Long assignedUserCount; // Number of users assigned to this role

}

