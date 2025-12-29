package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.AgencyRequestDTO;
import com.onelpro.librelog.dto.AgencyResponseDTO;
import com.onelpro.librelog.services.AgencyService;
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
 * REST controller for agency management endpoints.
 */
@RestController
@RequestMapping("/api/agencies")
@Tag(name = "Agencies", description = "Agency management endpoints")
public class AgencyController {

	private static final Logger logger = LoggerFactory.getLogger(AgencyController.class);

	private final AgencyService agencyService;

	public AgencyController(AgencyService agencyService) {
		this.agencyService = agencyService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new agency",
			description = "Creates a new advertising agency"
	)
	@ApiResponse(responseCode = "201", description = "Agency created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data or name already exists")
	public ResponseEntity<AgencyResponseDTO> create(@Valid @RequestBody AgencyRequestDTO request) {
		logger.info("POST /api/agencies - Creating agency: {}", request.getName());
		AgencyResponseDTO response = agencyService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get agency by ID",
			description = "Retrieves an agency by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Agency found")
	@ApiResponse(responseCode = "404", description = "Agency not found")
	public ResponseEntity<AgencyResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/agencies/{} - Fetching agency", id);
		AgencyResponseDTO response = agencyService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "Get all agencies",
			description = "Retrieves all agencies"
	)
	@ApiResponse(responseCode = "200", description = "Agencies retrieved successfully")
	public ResponseEntity<List<AgencyResponseDTO>> getAll() {
		logger.debug("GET /api/agencies - Fetching all agencies");
		List<AgencyResponseDTO> response = agencyService.getAll();
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update agency",
			description = "Updates an existing agency"
	)
	@ApiResponse(responseCode = "200", description = "Agency updated successfully")
	@ApiResponse(responseCode = "404", description = "Agency not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<AgencyResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody AgencyRequestDTO request) {
		logger.info("PUT /api/agencies/{} - Updating agency", id);
		AgencyResponseDTO response = agencyService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete agency",
			description = "Deletes an agency by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Agency deleted successfully")
	@ApiResponse(responseCode = "404", description = "Agency not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/agencies/{} - Deleting agency", id);
		agencyService.delete(id);
		return ResponseEntity.noContent().build();
	}

}

