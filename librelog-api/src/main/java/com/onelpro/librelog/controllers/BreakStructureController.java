package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.BreakStructureRequestDTO;
import com.onelpro.librelog.dto.BreakStructureResponseDTO;
import com.onelpro.librelog.services.BreakStructureService;
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
 * REST controller for break structure management endpoints.
 */
@RestController
@RequestMapping("/api/breaks")
@Tag(name = "Break Structures", description = "Break structure management endpoints")
public class BreakStructureController {

	private static final Logger logger = LoggerFactory.getLogger(BreakStructureController.class);

	private final BreakStructureService breakStructureService;

	public BreakStructureController(BreakStructureService breakStructureService) {
		this.breakStructureService = breakStructureService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new break structure",
			description = "Creates a new break structure for a clock template"
	)
	@ApiResponse(responseCode = "201", description = "Break structure created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Clock template or avail type not found")
	public ResponseEntity<BreakStructureResponseDTO> create(@Valid @RequestBody BreakStructureRequestDTO request) {
		logger.info("POST /api/breaks - Creating break structure: {}", request.getName());
		BreakStructureResponseDTO response = breakStructureService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get break structure by ID",
			description = "Retrieves a break structure by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Break structure found")
	@ApiResponse(responseCode = "404", description = "Break structure not found")
	public ResponseEntity<BreakStructureResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/breaks/{} - Fetching break structure", id);
		BreakStructureResponseDTO response = breakStructureService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/clock-templates/{clockId}")
	@Operation(
			summary = "Get all break structures for a clock template",
			description = "Retrieves all break structures associated with a specific clock template"
	)
	@ApiResponse(responseCode = "200", description = "Break structures retrieved successfully")
	@ApiResponse(responseCode = "404", description = "Clock template not found")
	public ResponseEntity<List<BreakStructureResponseDTO>> getByClockTemplateId(@PathVariable UUID clockId) {
		logger.debug("GET /api/breaks/clock-templates/{} - Fetching break structures", clockId);
		List<BreakStructureResponseDTO> response = breakStructureService.getByClockTemplateId(clockId);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update break structure",
			description = "Updates an existing break structure"
	)
	@ApiResponse(responseCode = "200", description = "Break structure updated successfully")
	@ApiResponse(responseCode = "404", description = "Break structure not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<BreakStructureResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody BreakStructureRequestDTO request) {
		logger.info("PUT /api/breaks/{} - Updating break structure", id);
		BreakStructureResponseDTO response = breakStructureService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete break structure",
			description = "Deletes a break structure by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Break structure deleted successfully")
	@ApiResponse(responseCode = "404", description = "Break structure not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/breaks/{} - Deleting break structure", id);
		breakStructureService.delete(id);
		return ResponseEntity.noContent().build();
	}

}

