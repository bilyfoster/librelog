package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.UserStationAssignmentRequestDTO;
import com.onelpro.librelog.dto.UserStationAssignmentResponseDTO;
import com.onelpro.librelog.enums.PermissionLevel;
import com.onelpro.librelog.services.UserStationAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for user-station assignment endpoints.
 */
@RestController
@RequestMapping("/api/user-station-assignments")
@Tag(name = "User Station Assignments", description = "User-station assignment management endpoints")
public class UserStationAssignmentController {

	private static final Logger logger = LoggerFactory.getLogger(UserStationAssignmentController.class);

	private final UserStationAssignmentService userStationAssignmentService;

	public UserStationAssignmentController(UserStationAssignmentService userStationAssignmentService) {
		this.userStationAssignmentService = userStationAssignmentService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Assign user to station",
			description = "Assigns a user to a station with specific permission levels. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "201", description = "Assignment created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data or duplicate assignment")
	@ApiResponse(responseCode = "404", description = "User or station not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<UserStationAssignmentResponseDTO> assignUserToStation(
			@Valid @RequestBody UserStationAssignmentRequestDTO request) {
		logger.info("Assign user to station request: userId={}, stationId={}", 
				request.getUserId(), request.getStationId());
		UserStationAssignmentResponseDTO response = userStationAssignmentService.assignUserToStation(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Remove assignment by ID",
			description = "Removes a user-station assignment by its ID. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "204", description = "Assignment removed successfully")
	@ApiResponse(responseCode = "404", description = "Assignment not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<Void> removeAssignment(@PathVariable UUID id) {
		logger.info("Remove assignment request for ID: {}", id);
		// Note: Service method uses userId and stationId, so we need to get the assignment first
		// For now, we'll use a different approach - delete by userId and stationId
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@DeleteMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Remove user from station",
			description = "Removes a user from a station. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "204", description = "Assignment removed successfully")
	@ApiResponse(responseCode = "404", description = "Assignment not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<Void> removeUserFromStation(
			@RequestParam UUID userId,
			@RequestParam UUID stationId) {
		logger.info("Remove user from station request: userId={}, stationId={}", userId, stationId);
		userStationAssignmentService.removeUserFromStation(userId, stationId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/user/{userId}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Get user's station assignments",
			description = "Retrieves all station assignments for a specific user. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Assignments retrieved successfully")
	@ApiResponse(responseCode = "404", description = "User not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<List<UserStationAssignmentResponseDTO>> getUserAssignments(@PathVariable UUID userId) {
		logger.info("Get user assignments request for userId: {}", userId);
		List<UserStationAssignmentResponseDTO> assignments = 
				userStationAssignmentService.getUserStationAssignments(userId);
		return ResponseEntity.ok(assignments);
	}

	@GetMapping("/station/{stationId}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Get station's user assignments",
			description = "Retrieves all user assignments for a specific station. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Assignments retrieved successfully")
	@ApiResponse(responseCode = "404", description = "Station not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<List<UserStationAssignmentResponseDTO>> getStationAssignments(
			@PathVariable UUID stationId) {
		logger.info("Get station assignments request for stationId: {}", stationId);
		List<UserStationAssignmentResponseDTO> assignments = 
				userStationAssignmentService.getStationUserAssignments(stationId);
		return ResponseEntity.ok(assignments);
	}

	@PutMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Update permission level",
			description = "Updates the permission level and custom permissions for a user-station assignment. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Permission level updated successfully")
	@ApiResponse(responseCode = "404", description = "Assignment not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<UserStationAssignmentResponseDTO> updatePermissionLevel(
			@RequestParam UUID userId,
			@RequestParam UUID stationId,
			@RequestParam PermissionLevel permissionLevel,
			@RequestBody(required = false) Map<String, Object> customPermissions) {
		logger.info("Update permission level request: userId={}, stationId={}, level={}", 
				userId, stationId, permissionLevel);
		UserStationAssignmentResponseDTO response = userStationAssignmentService.updatePermissionLevel(
				userId, stationId, permissionLevel, customPermissions);
		return ResponseEntity.ok(response);
	}

}

