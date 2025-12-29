package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.AvailTypeRequestDTO;
import com.onelpro.librelog.dto.AvailTypeResponseDTO;
import com.onelpro.librelog.services.AvailTypeService;
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
 * REST controller for avail type management endpoints.
 */
@RestController
@RequestMapping("/api/avail-types")
@Tag(name = "Avail Types", description = "Avail type management endpoints")
public class AvailTypeController {

	private static final Logger logger = LoggerFactory.getLogger(AvailTypeController.class);

	private final AvailTypeService availTypeService;

	public AvailTypeController(AvailTypeService availTypeService) {
		this.availTypeService = availTypeService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new avail type",
			description = "Creates a new avail type for commercial break categorization"
	)
	@ApiResponse(responseCode = "201", description = "Avail type created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "409", description = "Avail type with name already exists")
	public ResponseEntity<AvailTypeResponseDTO> create(@Valid @RequestBody AvailTypeRequestDTO request) {
		logger.info("POST /api/avail-types - Creating avail type: {}", request.getName());
		AvailTypeResponseDTO response = availTypeService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get avail type by ID",
			description = "Retrieves an avail type by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Avail type found")
	@ApiResponse(responseCode = "404", description = "Avail type not found")
	public ResponseEntity<AvailTypeResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/avail-types/{} - Fetching avail type", id);
		AvailTypeResponseDTO response = availTypeService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "Get all avail types",
			description = "Retrieves all avail types, optionally filtered to active only"
	)
	@ApiResponse(responseCode = "200", description = "Avail types retrieved successfully")
	public ResponseEntity<List<AvailTypeResponseDTO>> getAll(@RequestParam(required = false) Boolean active) {
		logger.debug("GET /api/avail-types - Fetching avail types");
		List<AvailTypeResponseDTO> response;
		if (active != null && active) {
			response = availTypeService.getActive();
		} else {
			response = availTypeService.getAll();
		}
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update avail type",
			description = "Updates an existing avail type"
	)
	@ApiResponse(responseCode = "200", description = "Avail type updated successfully")
	@ApiResponse(responseCode = "404", description = "Avail type not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "409", description = "Avail type with name already exists")
	public ResponseEntity<AvailTypeResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody AvailTypeRequestDTO request) {
		logger.info("PUT /api/avail-types/{} - Updating avail type", id);
		AvailTypeResponseDTO response = availTypeService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete avail type",
			description = "Deletes an avail type by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Avail type deleted successfully")
	@ApiResponse(responseCode = "404", description = "Avail type not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/avail-types/{} - Deleting avail type", id);
		availTypeService.delete(id);
		return ResponseEntity.noContent().build();
	}

}

