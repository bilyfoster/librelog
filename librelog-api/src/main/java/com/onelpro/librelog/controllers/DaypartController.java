package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.DaypartRequestDTO;
import com.onelpro.librelog.dto.DaypartResponseDTO;
import com.onelpro.librelog.services.DaypartService;
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
 * REST controller for daypart management endpoints.
 */
@RestController
@RequestMapping("/api/dayparts")
@Tag(name = "Dayparts", description = "Daypart management endpoints")
public class DaypartController {

	private static final Logger logger = LoggerFactory.getLogger(DaypartController.class);

	private final DaypartService daypartService;

	public DaypartController(DaypartService daypartService) {
		this.daypartService = daypartService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new daypart",
			description = "Creates a new daypart with start and end times"
	)
	@ApiResponse(responseCode = "201", description = "Daypart created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Daypart category not found")
	public ResponseEntity<DaypartResponseDTO> create(@Valid @RequestBody DaypartRequestDTO request) {
		logger.info("POST /api/dayparts - Creating daypart: {}", request.getName());
		DaypartResponseDTO response = daypartService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get daypart by ID",
			description = "Retrieves a daypart by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Daypart found")
	@ApiResponse(responseCode = "404", description = "Daypart not found")
	public ResponseEntity<DaypartResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/dayparts/{} - Fetching daypart", id);
		DaypartResponseDTO response = daypartService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "Get all dayparts",
			description = "Retrieves all dayparts"
	)
	@ApiResponse(responseCode = "200", description = "Dayparts retrieved successfully")
	public ResponseEntity<List<DaypartResponseDTO>> getAll() {
		logger.debug("GET /api/dayparts - Fetching all dayparts");
		List<DaypartResponseDTO> response = daypartService.getAll();
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update daypart",
			description = "Updates an existing daypart"
	)
	@ApiResponse(responseCode = "200", description = "Daypart updated successfully")
	@ApiResponse(responseCode = "404", description = "Daypart not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<DaypartResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody DaypartRequestDTO request) {
		logger.info("PUT /api/dayparts/{} - Updating daypart", id);
		DaypartResponseDTO response = daypartService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete daypart",
			description = "Deletes a daypart by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Daypart deleted successfully")
	@ApiResponse(responseCode = "404", description = "Daypart not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/dayparts/{} - Deleting daypart", id);
		daypartService.delete(id);
		return ResponseEntity.noContent().build();
	}

}

