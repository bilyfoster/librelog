package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.ClockTemplateRequestDTO;
import com.onelpro.librelog.dto.ClockTemplateResponseDTO;
import com.onelpro.librelog.dto.ClockValidationResultDTO;
import com.onelpro.librelog.services.ClockService;
import com.onelpro.librelog.services.ClockValidationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for clock template management endpoints.
 */
@RestController
@RequestMapping("/api/clocks")
@Tag(name = "Clock Templates", description = "Clock template management endpoints")
public class ClockController {

	private static final Logger logger = LoggerFactory.getLogger(ClockController.class);

	private final ClockService clockService;
	private final ClockValidationService clockValidationService;

	public ClockController(ClockService clockService, ClockValidationService clockValidationService) {
		this.clockService = clockService;
		this.clockValidationService = clockValidationService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new clock template",
			description = "Creates a new clock template for a channel"
	)
	@ApiResponse(responseCode = "201", description = "Clock template created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Channel not found")
	public ResponseEntity<ClockTemplateResponseDTO> create(@Valid @RequestBody ClockTemplateRequestDTO request) {
		logger.info("POST /api/clocks - Creating clock template: {}", request.getName());
		ClockTemplateResponseDTO response = clockService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get clock template by ID",
			description = "Retrieves a clock template by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Clock template found")
	@ApiResponse(responseCode = "404", description = "Clock template not found")
	public ResponseEntity<ClockTemplateResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/clocks/{} - Fetching clock template", id);
		ClockTemplateResponseDTO response = clockService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "Get all clock templates",
			description = "Retrieves all clock templates, optionally filtered by channel ID"
	)
	@ApiResponse(responseCode = "200", description = "Clock templates retrieved successfully")
	public ResponseEntity<List<ClockTemplateResponseDTO>> getAll(@RequestParam(required = false) UUID channelId) {
		logger.debug("GET /api/clocks - Fetching clock templates");
		List<ClockTemplateResponseDTO> response;
		if (channelId != null) {
			response = clockService.getByChannelId(channelId);
		} else {
			response = clockService.getAll();
		}
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update clock template",
			description = "Updates an existing clock template"
	)
	@ApiResponse(responseCode = "200", description = "Clock template updated successfully")
	@ApiResponse(responseCode = "404", description = "Clock template not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<ClockTemplateResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody ClockTemplateRequestDTO request) {
		logger.info("PUT /api/clocks/{} - Updating clock template", id);
		ClockTemplateResponseDTO response = clockService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete clock template",
			description = "Deletes a clock template by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Clock template deleted successfully")
	@ApiResponse(responseCode = "404", description = "Clock template not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/clocks/{} - Deleting clock template", id);
		clockService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/validate")
	@Operation(
			summary = "Validate clock template",
			description = "Validates a clock template for conflicts, overlaps, and timing issues"
	)
	@ApiResponse(responseCode = "200", description = "Validation completed")
	@ApiResponse(responseCode = "404", description = "Clock template not found")
	public ResponseEntity<ClockValidationResultDTO> validate(@PathVariable UUID id) {
		logger.info("POST /api/clocks/{}/validate - Validating clock template", id);
		ClockValidationResultDTO result = clockValidationService.validateClock(id);
		return ResponseEntity.ok(result);
	}

}

