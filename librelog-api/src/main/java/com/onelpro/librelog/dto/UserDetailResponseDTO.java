package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * DTO for detailed user response data including assignments, sessions, and audit logs.
 * Extends UserResponseDTO with additional information.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserDetailResponseDTO extends UserResponseDTO {

	/**
	 * List of station assignments for this user.
	 */
	private List<UserStationAssignmentResponseDTO> stationAssignments;

	/**
	 * List of active sessions for this user.
	 */
	private List<UserSessionResponseDTO> activeSessions;

	/**
	 * List of recent audit log entries for this user.
	 */
	private List<AuditLogResponseDTO> recentAuditLogs;

}

