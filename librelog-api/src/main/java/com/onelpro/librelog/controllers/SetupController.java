package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.UserResponseDTO;
import com.onelpro.librelog.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for system setup and status endpoints.
 */
@RestController
@RequestMapping("/api/setup")
@Tag(name = "Setup", description = "System setup and status endpoints")
public class SetupController {

	private static final Logger logger = LoggerFactory.getLogger(SetupController.class);

	private final UserService userService;

	public SetupController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/status")
	@Operation(
			summary = "Get setup status",
			description = "Returns the system setup status including whether initial configuration is complete"
	)
	@ApiResponse(responseCode = "200", description = "Setup status retrieved successfully")
	public ResponseEntity<Map<String, Object>> getSetupStatus() {
		logger.debug("GET /api/setup/status - Checking setup status");

		// Check if any users exist (indicates initial setup is done)
		List<UserResponseDTO> users = userService.getAll();
		boolean setupComplete = !users.isEmpty();
		boolean hasAdmin = users.stream().anyMatch(u -> u.getRole().name().equals("ADMIN"));

		Map<String, Object> status = new HashMap<>();
		status.put("setupComplete", setupComplete);
		status.put("hasAdminUser", hasAdmin);
		status.put("userCount", users.size());
		status.put("requiresInitialSetup", !setupComplete);

		return ResponseEntity.ok(status);
	}

}
