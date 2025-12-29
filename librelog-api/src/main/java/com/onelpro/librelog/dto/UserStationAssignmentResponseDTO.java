package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.PermissionLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for user-station assignment response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStationAssignmentResponseDTO {

	private UUID id;
	private UUID userId;
	private String userEmail;
	private UUID stationId;
	private String stationName;
	private PermissionLevel permissionLevel;
	private Map<String, Object> customPermissions;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

}

