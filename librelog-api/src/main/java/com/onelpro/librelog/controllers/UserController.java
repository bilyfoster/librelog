package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.UserDetailResponseDTO;
import com.onelpro.librelog.dto.UserRequestDTO;
import com.onelpro.librelog.dto.UserResponseDTO;
import com.onelpro.librelog.dto.UserStationAssignmentRequestDTO;
import com.onelpro.librelog.services.UserService;
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
import java.util.UUID;

/**
 * REST controller for user management endpoints.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Create a new user",
			description = "Creates a new user account. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "201", description = "User created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserRequestDTO request) {
		logger.info("Create user request received for email: {}", request.getEmail());
		UserResponseDTO response = userService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Get user by ID",
			description = "Retrieves a user by their ID. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "User found")
	@ApiResponse(responseCode = "404", description = "User not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<UserResponseDTO> getById(@PathVariable UUID id) {
		logger.info("Get user request received for ID: {}", id);
		UserResponseDTO response = userService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Get all users",
			description = "Retrieves all users in the system. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Users retrieved successfully")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<List<UserResponseDTO>> getAll() {
		logger.info("Get all users request received");
		List<UserResponseDTO> users = userService.getAll();
		return ResponseEntity.ok(users);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Update user",
			description = "Updates an existing user. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "User updated successfully")
	@ApiResponse(responseCode = "404", description = "User not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<UserResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody UserRequestDTO request) {
		logger.info("Update user request received for ID: {}", id);
		UserResponseDTO response = userService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Delete user",
			description = "Deletes a user by their ID. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "204", description = "User deleted successfully")
	@ApiResponse(responseCode = "404", description = "User not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("Delete user request received for ID: {}", id);
		userService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/detail")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Get user detail",
			description = "Retrieves detailed user information including station assignments, active sessions, and recent audit logs. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "User detail retrieved successfully")
	@ApiResponse(responseCode = "404", description = "User not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<UserDetailResponseDTO> getUserDetail(@PathVariable UUID id) {
		logger.info("Get user detail request for ID: {}", id);
		UserDetailResponseDTO response = userService.getUserDetail(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}/assignments")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Get user's station assignments",
			description = "Retrieves a user with their station assignment summaries. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "User assignments retrieved successfully")
	@ApiResponse(responseCode = "404", description = "User not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<UserResponseDTO> getUserAssignments(@PathVariable UUID id) {
		logger.info("Get user assignments request for ID: {}", id);
		UserResponseDTO response = userService.getUserWithAssignments(id);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}/assignments")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Update user's station assignments",
			description = "Updates a user and their station assignments in a single transaction. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "User and assignments updated successfully")
	@ApiResponse(responseCode = "404", description = "User not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<UserResponseDTO> updateUserWithAssignments(
			@PathVariable UUID id,
			@Valid @RequestBody UserRequestDTO request,
			@RequestBody(required = false) List<@Valid UserStationAssignmentRequestDTO> stationAssignments) {
		logger.info("Update user with assignments request for ID: {}", id);
		UserResponseDTO response = userService.updateUserWithStations(id, request, stationAssignments);
		return ResponseEntity.ok(response);
	}

}

