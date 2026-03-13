package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.ProfileUpdateRequestDTO;
import com.onelpro.librelog.dto.UserDetailResponseDTO;
import com.onelpro.librelog.dto.UserRequestDTO;
import com.onelpro.librelog.dto.UserResponseDTO;
import com.onelpro.librelog.dto.UserStationAssignmentRequestDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for user management operations.
 */
public interface UserService {

	UserResponseDTO create(UserRequestDTO request);

	UserResponseDTO getById(UUID id);

	List<UserResponseDTO> getAll();

	UserResponseDTO update(UUID id, UserRequestDTO request);

	void delete(UUID id);

	/**
	 * Gets detailed user information including assignments, sessions, and audit logs.
	 *
	 * @param userId the user ID
	 * @return detailed user information
	 */
	UserDetailResponseDTO getUserDetail(UUID userId);

	/**
	 * Gets user information with station assignments.
	 *
	 * @param userId the user ID
	 * @return user information with station assignments
	 */
	UserResponseDTO getUserWithAssignments(UUID userId);

	/**
	 * Updates a user and their station assignments in a single transaction.
	 *
	 * @param userId the user ID
	 * @param request the user update request
	 * @param stationAssignments the list of station assignments to update
	 * @return the updated user information
	 */
	UserResponseDTO updateUserWithStations(UUID userId, UserRequestDTO request,
	                                       List<UserStationAssignmentRequestDTO> stationAssignments);

	/**
	 * Updates the current user's profile (self-service).
	 * Users can update their email and password.
	 *
	 * @param userId the user ID
	 * @param request the profile update request
	 * @return the updated user information
	 */
	UserResponseDTO updateProfile(UUID userId, ProfileUpdateRequestDTO request);

}

