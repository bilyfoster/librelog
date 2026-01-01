package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.ConnectionTestResponseDTO;
import com.onelpro.librelog.dto.LibreTimeIntegrationConfigRequestDTO;
import com.onelpro.librelog.dto.LibreTimeIntegrationConfigResponseDTO;
import com.onelpro.librelog.services.LibreTimeIntegrationConfigService;
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

import java.util.UUID;

/**
 * REST controller for LibreTime integration configuration management.
 */
@RestController
@RequestMapping("/api/libretime/integration")
@Tag(name = "LibreTime Integration", description = "LibreTime integration configuration and management endpoints")
public class LibreTimeIntegrationController {

	private static final Logger logger = LoggerFactory.getLogger(LibreTimeIntegrationController.class);

	private final LibreTimeIntegrationConfigService configService;

	public LibreTimeIntegrationController(LibreTimeIntegrationConfigService configService) {
		this.configService = configService;
	}

	@GetMapping("/config")
	@Operation(
			summary = "Get integration configuration",
			description = "Retrieves the LibreTime integration configuration for a specific station. Sensitive fields are masked."
	)
	@ApiResponse(responseCode = "200", description = "Configuration retrieved successfully")
	@ApiResponse(responseCode = "404", description = "Configuration not found")
	@PreAuthorize("hasPermission(#stationId, 'LIBRETIME_INTEGRATION_VIEW')")
	public ResponseEntity<LibreTimeIntegrationConfigResponseDTO> getConfig(
			@RequestParam UUID stationId) {
		logger.info("GET /api/libretime/integration/config - Retrieving integration configuration for station: {}", stationId);
		LibreTimeIntegrationConfigResponseDTO config = configService.getConfig(stationId);
		if (config == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(config);
	}

	@PutMapping("/config")
	@Operation(
			summary = "Update integration configuration",
			description = "Updates the LibreTime integration configuration for a specific station. Sensitive fields (JWT token, webhook secret) are encrypted before storage."
	)
	@ApiResponse(responseCode = "200", description = "Configuration updated successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Configuration not found")
	@PreAuthorize("hasPermission(#stationId, 'LIBRETIME_INTEGRATION_CONFIGURE')")
	public ResponseEntity<LibreTimeIntegrationConfigResponseDTO> updateConfig(
			@RequestParam UUID stationId,
			@Valid @RequestBody LibreTimeIntegrationConfigRequestDTO request,
			@RequestHeader(value = "X-User-Id", required = false) UUID userId) {
		logger.info("PUT /api/libretime/integration/config - Updating integration configuration for station: {}", stationId);
		
		// TODO: Get actual user ID from security context instead of header
		if (userId == null) {
			userId = UUID.randomUUID(); // Placeholder
		}
		
		LibreTimeIntegrationConfigResponseDTO updatedConfig = configService.updateConfig(stationId, request, userId);
		return ResponseEntity.ok(updatedConfig);
	}

	@PostMapping("/config")
	@Operation(
			summary = "Create integration configuration",
			description = "Creates a new LibreTime integration configuration for a specific station. Sensitive fields (JWT token, webhook secret) are encrypted before storage."
	)
	@ApiResponse(responseCode = "201", description = "Configuration created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data or configuration already exists")
	@PreAuthorize("hasPermission(#stationId, 'LIBRETIME_INTEGRATION_CONFIGURE')")
	public ResponseEntity<LibreTimeIntegrationConfigResponseDTO> createConfig(
			@RequestParam UUID stationId,
			@Valid @RequestBody LibreTimeIntegrationConfigRequestDTO request,
			@RequestHeader(value = "X-User-Id", required = false) UUID userId) {
		logger.info("POST /api/libretime/integration/config - Creating integration configuration for station: {}", stationId);
		
		// TODO: Get actual user ID from security context instead of header
		if (userId == null) {
			userId = UUID.randomUUID(); // Placeholder
		}
		
		LibreTimeIntegrationConfigResponseDTO createdConfig = configService.saveConfig(stationId, request, userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdConfig);
	}

	@PostMapping("/test-connection")
	@Operation(
			summary = "Test LibreTime API connection",
			description = "Tests the connection to LibreTime API using the configuration for a specific station. Returns connection status and response time."
	)
	@ApiResponse(responseCode = "200", description = "Connection test completed")
	@ApiResponse(responseCode = "404", description = "Configuration not found")
	@PreAuthorize("hasPermission(#stationId, 'LIBRETIME_INTEGRATION_VIEW')")
	public ResponseEntity<ConnectionTestResponseDTO> testConnection(
			@RequestParam UUID stationId) {
		logger.info("POST /api/libretime/integration/test-connection - Testing LibreTime API connection for station: {}", stationId);
		ConnectionTestResponseDTO result = configService.testConnection(stationId);
		return ResponseEntity.ok(result);
	}

}

