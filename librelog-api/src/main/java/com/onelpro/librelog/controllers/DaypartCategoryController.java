package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.DaypartCategoryRequestDTO;
import com.onelpro.librelog.dto.DaypartCategoryResponseDTO;
import com.onelpro.librelog.services.DaypartCategoryService;
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
 * REST controller for daypart category management endpoints.
 */
@RestController
@RequestMapping("/api/daypart-categories")
@Tag(name = "Daypart Categories", description = "Daypart category management endpoints")
public class DaypartCategoryController {

	private static final Logger logger = LoggerFactory.getLogger(DaypartCategoryController.class);

	private final DaypartCategoryService daypartCategoryService;

	public DaypartCategoryController(DaypartCategoryService daypartCategoryService) {
		this.daypartCategoryService = daypartCategoryService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new daypart category",
			description = "Creates a new daypart category (e.g., Drive Time, Prime Time, Overnight)"
	)
	@ApiResponse(responseCode = "201", description = "Daypart category created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data or name already exists")
	public ResponseEntity<DaypartCategoryResponseDTO> create(@Valid @RequestBody DaypartCategoryRequestDTO request) {
		logger.info("POST /api/daypart-categories - Creating daypart category: {}", request.getName());
		DaypartCategoryResponseDTO response = daypartCategoryService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get daypart category by ID",
			description = "Retrieves a daypart category by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Daypart category found")
	@ApiResponse(responseCode = "404", description = "Daypart category not found")
	public ResponseEntity<DaypartCategoryResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/daypart-categories/{} - Fetching daypart category", id);
		DaypartCategoryResponseDTO response = daypartCategoryService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "Get all daypart categories",
			description = "Retrieves all daypart categories"
	)
	@ApiResponse(responseCode = "200", description = "Daypart categories retrieved successfully")
	public ResponseEntity<List<DaypartCategoryResponseDTO>> getAll() {
		logger.debug("GET /api/daypart-categories - Fetching all daypart categories");
		List<DaypartCategoryResponseDTO> response = daypartCategoryService.getAll();
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update daypart category",
			description = "Updates an existing daypart category"
	)
	@ApiResponse(responseCode = "200", description = "Daypart category updated successfully")
	@ApiResponse(responseCode = "404", description = "Daypart category not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data or name already exists")
	public ResponseEntity<DaypartCategoryResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody DaypartCategoryRequestDTO request) {
		logger.info("PUT /api/daypart-categories/{} - Updating daypart category", id);
		DaypartCategoryResponseDTO response = daypartCategoryService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete daypart category",
			description = "Deletes a daypart category by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Daypart category deleted successfully")
	@ApiResponse(responseCode = "404", description = "Daypart category not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/daypart-categories/{} - Deleting daypart category", id);
		daypartCategoryService.delete(id);
		return ResponseEntity.noContent().build();
	}

}

