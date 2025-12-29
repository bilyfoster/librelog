package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.*;
import com.onelpro.librelog.services.ClockBuilderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for clock builder structure management endpoints.
 */
@RestController
@RequestMapping("/api/clock-templates")
@Tag(name = "Clock Builder", description = "Clock template structure management endpoints")
public class ClockBuilderController {

	private static final Logger logger = LoggerFactory.getLogger(ClockBuilderController.class);

	private final ClockBuilderService clockBuilderService;

	public ClockBuilderController(ClockBuilderService clockBuilderService) {
		this.clockBuilderService = clockBuilderService;
	}

	@GetMapping("/{id}/structure")
	@Operation(
			summary = "Get full clock structure",
			description = "Retrieves a clock template with all its breaks, fixed assets, and automation commands"
	)
	@ApiResponse(responseCode = "200", description = "Clock structure retrieved successfully")
	@ApiResponse(responseCode = "404", description = "Clock template not found")
	public ResponseEntity<ClockTemplateWithBreaksDTO> getClockStructure(@PathVariable UUID id) {
		logger.debug("GET /api/clock-templates/{}/structure - Fetching clock structure", id);
		ClockTemplateWithBreaksDTO structure = clockBuilderService.getClockStructure(id);
		return ResponseEntity.ok(structure);
	}

	@PostMapping("/{id}/breaks")
	@Operation(
			summary = "Add break to clock template",
			description = "Adds a commercial break to the specified clock template"
	)
	@ApiResponse(responseCode = "201", description = "Break added successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Clock template not found")
	public ResponseEntity<BreakStructureResponseDTO> addBreak(
			@PathVariable UUID id,
			@Valid @RequestBody BreakStructureRequestDTO request) {
		logger.info("POST /api/clock-templates/{}/breaks - Adding break", id);
		BreakStructureResponseDTO response = clockBuilderService.addBreak(id, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/breaks/{breakId}")
	@Operation(
			summary = "Update break in clock template",
			description = "Updates an existing commercial break in a clock template"
	)
	@ApiResponse(responseCode = "200", description = "Break updated successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Break not found")
	public ResponseEntity<BreakStructureResponseDTO> updateBreak(
			@PathVariable UUID breakId,
			@Valid @RequestBody BreakStructureRequestDTO request) {
		logger.info("PUT /api/clock-templates/breaks/{} - Updating break", breakId);
		BreakStructureResponseDTO response = clockBuilderService.updateBreak(breakId, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/breaks/{breakId}")
	@Operation(
			summary = "Remove break from clock template",
			description = "Removes a commercial break from a clock template"
	)
	@ApiResponse(responseCode = "204", description = "Break removed successfully")
	@ApiResponse(responseCode = "404", description = "Break not found")
	public ResponseEntity<Void> removeBreak(@PathVariable UUID breakId) {
		logger.info("DELETE /api/clock-templates/breaks/{} - Removing break", breakId);
		clockBuilderService.removeBreak(breakId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/fixed-assets")
	@Operation(
			summary = "Add fixed asset to clock template",
			description = "Adds a fixed asset (static cart) to the specified clock template"
	)
	@ApiResponse(responseCode = "201", description = "Fixed asset added successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Clock template not found")
	public ResponseEntity<FixedAssetResponseDTO> addFixedAsset(
			@PathVariable UUID id,
			@Valid @RequestBody FixedAssetRequestDTO request) {
		logger.info("POST /api/clock-templates/{}/fixed-assets - Adding fixed asset", id);
		FixedAssetResponseDTO response = clockBuilderService.addFixedAsset(id, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/fixed-assets/{fixedAssetId}")
	@Operation(
			summary = "Update fixed asset in clock template",
			description = "Updates an existing fixed asset in a clock template"
	)
	@ApiResponse(responseCode = "200", description = "Fixed asset updated successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Fixed asset not found")
	public ResponseEntity<FixedAssetResponseDTO> updateFixedAsset(
			@PathVariable UUID fixedAssetId,
			@Valid @RequestBody FixedAssetRequestDTO request) {
		logger.info("PUT /api/clock-templates/fixed-assets/{} - Updating fixed asset", fixedAssetId);
		FixedAssetResponseDTO response = clockBuilderService.updateFixedAsset(fixedAssetId, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/fixed-assets/{fixedAssetId}")
	@Operation(
			summary = "Remove fixed asset from clock template",
			description = "Removes a fixed asset from a clock template"
	)
	@ApiResponse(responseCode = "204", description = "Fixed asset removed successfully")
	@ApiResponse(responseCode = "404", description = "Fixed asset not found")
	public ResponseEntity<Void> removeFixedAsset(@PathVariable UUID fixedAssetId) {
		logger.info("DELETE /api/clock-templates/fixed-assets/{} - Removing fixed asset", fixedAssetId);
		clockBuilderService.removeFixedAsset(fixedAssetId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/automation-commands")
	@Operation(
			summary = "Add automation command to clock template",
			description = "Adds an automation command to the specified clock template"
	)
	@ApiResponse(responseCode = "201", description = "Automation command added successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Clock template not found")
	public ResponseEntity<AutomationCommandResponseDTO> addAutomationCommand(
			@PathVariable UUID id,
			@Valid @RequestBody AutomationCommandRequestDTO request) {
		logger.info("POST /api/clock-templates/{}/automation-commands - Adding automation command", id);
		AutomationCommandResponseDTO response = clockBuilderService.addAutomationCommand(id, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/automation-commands/{commandId}")
	@Operation(
			summary = "Update automation command in clock template",
			description = "Updates an existing automation command in a clock template"
	)
	@ApiResponse(responseCode = "200", description = "Automation command updated successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Automation command not found")
	public ResponseEntity<AutomationCommandResponseDTO> updateAutomationCommand(
			@PathVariable UUID commandId,
			@Valid @RequestBody AutomationCommandRequestDTO request) {
		logger.info("PUT /api/clock-templates/automation-commands/{} - Updating automation command", commandId);
		AutomationCommandResponseDTO response = clockBuilderService.updateAutomationCommand(commandId, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/automation-commands/{commandId}")
	@Operation(
			summary = "Remove automation command from clock template",
			description = "Removes an automation command from a clock template"
	)
	@ApiResponse(responseCode = "204", description = "Automation command removed successfully")
	@ApiResponse(responseCode = "404", description = "Automation command not found")
	public ResponseEntity<Void> removeAutomationCommand(@PathVariable UUID commandId) {
		logger.info("DELETE /api/clock-templates/automation-commands/{} - Removing automation command", commandId);
		clockBuilderService.removeAutomationCommand(commandId);
		return ResponseEntity.noContent().build();
	}

}

