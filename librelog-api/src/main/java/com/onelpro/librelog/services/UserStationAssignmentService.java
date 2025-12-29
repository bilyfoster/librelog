package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.UserStationAssignmentRequestDTO;
import com.onelpro.librelog.dto.UserStationAssignmentResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for user-station assignment operations.
 * Handles property-based access control assignments.
 */
public interface UserStationAssignmentService {

	/**
	 * Assigns a user to a station with specific permissions.
	 *
	 * @param request the assignment request
	 * @return the created assignment
	 */
	UserStationAssignmentResponseDTO assignUserToStation(UserStationAssignmentRequestDTO request);

	/**
	 * Removes a user from a station.
	 *
	 * @param userId the user ID
	 * @param stationId the station ID
	 */
	void removeUserFromStation(UUID userId, UUID stationId);

	/**
	 * Gets all station assignments for a user.
	 *
	 * @param userId the user ID
	 * @return list of station assignments
	 */
	List<UserStationAssignmentResponseDTO> getUserStationAssignments(UUID userId);

	/**
	 * Gets all user assignments for a station.
	 *
	 * @param stationId the station ID
	 * @return list of user assignments
	 */
	List<UserStationAssignmentResponseDTO> getStationUserAssignments(UUID stationId);

	/**
	 * Updates the permission level and custom permissions for a user-station assignment.
	 *
	 * @param userId the user ID
	 * @param stationId the station ID
	 * @param permissionLevel the new permission level
	 * @param customPermissions the new custom permissions (optional)
	 * @return the updated assignment
	 */
	UserStationAssignmentResponseDTO updatePermissionLevel(UUID userId, UUID stationId,
	                                                         com.onelpro.librelog.enums.PermissionLevel permissionLevel,
	                                                         java.util.Map<String, Object> customPermissions);

}

