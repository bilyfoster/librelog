package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.UserSessionResponseDTO;
import com.onelpro.librelog.services.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for session management endpoints.
 */
@RestController
@RequestMapping("/api/sessions")
@Tag(name = "Sessions", description = "User session management endpoints")
public class SessionController {

	private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

	private final SessionService sessionService;

	public SessionController(SessionService sessionService) {
		this.sessionService = sessionService;
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Get all active sessions",
			description = "Retrieves all currently active user sessions. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Active sessions retrieved successfully")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<List<UserSessionResponseDTO>> getActiveSessions() {
		logger.info("Get all active sessions request");
		List<UserSessionResponseDTO> sessions = sessionService.getActiveSessions();
		return ResponseEntity.ok(sessions);
	}

	@GetMapping("/user/{userId}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Get user's sessions",
			description = "Retrieves all sessions for a specific user. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "User sessions retrieved successfully")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<List<UserSessionResponseDTO>> getUserSessions(@PathVariable UUID userId) {
		logger.info("Get user sessions request for userId: {}", userId);
		List<UserSessionResponseDTO> sessions = sessionService.getUserSessions(userId);
		return ResponseEntity.ok(sessions);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Terminate session",
			description = "Terminates a specific user session by its ID. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "204", description = "Session terminated successfully")
	@ApiResponse(responseCode = "404", description = "Session not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<Void> terminateSession(@PathVariable UUID id) {
		logger.info("Terminate session request for ID: {}", id);
		sessionService.terminateSession(id);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/user/{userId}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Terminate all user's sessions",
			description = "Terminates all active sessions for a specific user. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "204", description = "All sessions terminated successfully")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<Void> terminateAllUserSessions(@PathVariable UUID userId) {
		logger.info("Terminate all user sessions request for userId: {}", userId);
		sessionService.terminateAllUserSessions(userId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{id}/resource")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Update current resource",
			description = "Updates the current resource (station/log) being accessed in a session. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Resource updated successfully")
	@ApiResponse(responseCode = "404", description = "Session not found")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<Void> updateCurrentResource(
			@PathVariable UUID id,
			@RequestParam(required = false) UUID stationId,
			@RequestParam(required = false) UUID resourceId) {
		logger.info("Update current resource request: sessionId={}, stationId={}, resourceId={}", 
				id, stationId, resourceId);
		sessionService.updateCurrentResource(id, stationId, resourceId);
		return ResponseEntity.ok().build();
	}

}

