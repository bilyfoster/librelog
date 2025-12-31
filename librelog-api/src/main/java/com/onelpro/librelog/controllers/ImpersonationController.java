package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.ImpersonationRequestDTO;
import com.onelpro.librelog.dto.UserResponseDTO;
import com.onelpro.librelog.services.ImpersonationService;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for user impersonation endpoints.
 */
@RestController
@RequestMapping("/api/impersonation")
@Tag(name = "Impersonation", description = "User impersonation endpoints for troubleshooting")
public class ImpersonationController {

	private static final Logger logger = LoggerFactory.getLogger(ImpersonationController.class);

	private final ImpersonationService impersonationService;

	public ImpersonationController(ImpersonationService impersonationService) {
		this.impersonationService = impersonationService;
	}

	@PostMapping("/start")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Start impersonation",
			description = "Starts impersonating a target user for troubleshooting purposes. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Impersonation started successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request or cannot impersonate this user")
	@ApiResponse(responseCode = "404", description = "Target user not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<Map<String, Object>> startImpersonation(
			@Valid @RequestBody ImpersonationRequestDTO request) {
		logger.info("Start impersonation request: targetUserId={}", request.getTargetUserId());
		UUID adminUserId = getCurrentUserId();
		if (adminUserId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		String token = impersonationService.startImpersonation(adminUserId, request.getTargetUserId());

		Map<String, Object> response = new HashMap<>();
		response.put("impersonationToken", token);
		response.put("targetUserId", request.getTargetUserId());
		response.put("adminUserId", adminUserId);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/stop")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Stop impersonation",
			description = "Stops the current impersonation session. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "204", description = "Impersonation stopped successfully")
	@ApiResponse(responseCode = "400", description = "No active impersonation found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<Void> stopImpersonation() {
		logger.info("Stop impersonation request");
		UUID adminUserId = getCurrentUserId();
		if (adminUserId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		impersonationService.stopImpersonation(adminUserId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/status")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Check impersonation status",
			description = "Checks if the current admin is impersonating a user and returns the impersonated user details. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Status retrieved successfully")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<Map<String, Object>> getImpersonationStatus() {
		logger.info("Get impersonation status request");
		UUID adminUserId = getCurrentUserId();
		if (adminUserId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		UserResponseDTO impersonatedUser = impersonationService.getImpersonatedUser(adminUserId);

		Map<String, Object> response = new HashMap<>();
		response.put("isImpersonating", impersonatedUser != null);
		if (impersonatedUser != null) {
			response.put("impersonatedUser", impersonatedUser);
		}

		return ResponseEntity.ok(response);
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


