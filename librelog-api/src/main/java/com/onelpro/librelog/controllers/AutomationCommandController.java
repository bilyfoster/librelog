package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.AutomationCommandRequestDTO;
import com.onelpro.librelog.dto.AutomationCommandResponseDTO;
import com.onelpro.librelog.services.AutomationCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for automation command management endpoints.
 */
@RestController
@RequestMapping("/api/automation-commands")
@Tag(name = "Automation Commands", description = "Automation command management endpoints")
public class AutomationCommandController {

	private static final Logger logger = LoggerFactory.getLogger(AutomationCommandController.class);

	private final AutomationCommandService automationCommandService;

	public AutomationCommandController(AutomationCommandService automationCommandService) {
		this.automationCommandService = automationCommandService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new automation command",
			description = "Creates a new automation command for a clock template"
	)
	@ApiResponse(responseCode = "201", description = "Automation command created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Clock template not found")
	public ResponseEntity<AutomationCommandResponseDTO> create(@Valid @RequestBody AutomationCommandRequestDTO request) {
		logger.info("POST /api/automation-commands - Creating automation command: {}", request.getCommandType());
		AutomationCommandResponseDTO response = automationCommandService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get automation command by ID",
			description = "Retrieves an automation command by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Automation command found")
	@ApiResponse(responseCode = "404", description = "Automation command not found")
	public ResponseEntity<AutomationCommandResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/automation-commands/{} - Fetching automation command", id);
		AutomationCommandResponseDTO response = automationCommandService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/clock-templates/{clockId}")
	@Operation(
			summary = "Get all automation commands for a clock template",
			description = "Retrieves all automation commands associated with a specific clock template"
	)
	@ApiResponse(responseCode = "200", description = "Automation commands retrieved successfully")
	@ApiResponse(responseCode = "404", description = "Clock template not found")
	public ResponseEntity<List<AutomationCommandResponseDTO>> getByClockTemplateId(@PathVariable UUID clockId) {
		logger.debug("GET /api/automation-commands/clock-templates/{} - Fetching automation commands", clockId);
		List<AutomationCommandResponseDTO> response = automationCommandService.getByClockTemplateId(clockId);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update automation command",
			description = "Updates an existing automation command"
	)
	@ApiResponse(responseCode = "200", description = "Automation command updated successfully")
	@ApiResponse(responseCode = "404", description = "Automation command not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<AutomationCommandResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody AutomationCommandRequestDTO request) {
		logger.info("PUT /api/automation-commands/{} - Updating automation command", id);
		AutomationCommandResponseDTO response = automationCommandService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete automation command",
			description = "Deletes an automation command by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Automation command deleted successfully")
	@ApiResponse(responseCode = "404", description = "Automation command not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/automation-commands/{} - Deleting automation command", id);
		automationCommandService.delete(id);
		return ResponseEntity.noContent().build();
	}

}

