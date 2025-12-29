package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.CustomRoleRequestDTO;
import com.onelpro.librelog.dto.CustomRoleResponseDTO;
import com.onelpro.librelog.services.CustomRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for custom role management endpoints.
 */
@RestController
@RequestMapping("/api/custom-roles")
@Tag(name = "Custom Roles", description = "Custom role management endpoints")
public class CustomRoleController {

	private static final Logger logger = LoggerFactory.getLogger(CustomRoleController.class);

	private final CustomRoleService customRoleService;

	public CustomRoleController(CustomRoleService customRoleService) {
		this.customRoleService = customRoleService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Create custom role",
			description = "Creates a new custom role with granular permissions. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "201", description = "Role created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data or duplicate role name")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<CustomRoleResponseDTO> createRole(@Valid @RequestBody CustomRoleRequestDTO request) {
		logger.info("Create custom role request: name={}", request.getName());
		UUID currentUserId = getCurrentUserId();
		CustomRoleResponseDTO response = customRoleService.createRole(request, currentUserId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Get role by ID",
			description = "Retrieves a custom role by its ID. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Role found")
	@ApiResponse(responseCode = "404", description = "Role not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<CustomRoleResponseDTO> getRoleById(@PathVariable UUID id) {
		logger.info("Get custom role request for ID: {}", id);
		CustomRoleResponseDTO response = customRoleService.getRoleById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Get all custom roles",
			description = "Retrieves all custom roles in the system. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Roles retrieved successfully")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<List<CustomRoleResponseDTO>> getAllRoles() {
		logger.info("Get all custom roles request");
		List<CustomRoleResponseDTO> roles = customRoleService.getAllRoles();
		return ResponseEntity.ok(roles);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Update custom role",
			description = "Updates an existing custom role. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Role updated successfully")
	@ApiResponse(responseCode = "404", description = "Role not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<CustomRoleResponseDTO> updateRole(
			@PathVariable UUID id,
			@Valid @RequestBody CustomRoleRequestDTO request) {
		logger.info("Update custom role request for ID: {}", id);
		CustomRoleResponseDTO response = customRoleService.updateRole(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Delete custom role",
			description = "Deletes a custom role by its ID. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "204", description = "Role deleted successfully")
	@ApiResponse(responseCode = "404", description = "Role not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
		logger.info("Delete custom role request for ID: {}", id);
		customRoleService.deleteRole(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/clone")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Clone custom role",
			description = "Clones an existing custom role with a new name. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "201", description = "Role cloned successfully")
	@ApiResponse(responseCode = "404", description = "Source role not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data or duplicate role name")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<CustomRoleResponseDTO> cloneRole(
			@PathVariable UUID id,
			@RequestParam String newName) {
		logger.info("Clone custom role request: id={}, newName={}", id, newName);
		UUID currentUserId = getCurrentUserId();
		CustomRoleResponseDTO response = customRoleService.cloneRole(id, newName, currentUserId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Gets the current user ID from the security context.
	 */
	private UUID getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof String) {
			try {
				return UUID.fromString((String) principal);
			} catch (IllegalArgumentException e) {
				logger.warn("Invalid UUID format in authentication principal: {}", principal);
				return null;
			}
		}

		return null;
	}

}

