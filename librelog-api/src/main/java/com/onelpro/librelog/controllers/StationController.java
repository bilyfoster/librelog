package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.StationRequestDTO;
import com.onelpro.librelog.dto.StationResponseDTO;
import com.onelpro.librelog.services.StationService;
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
 * REST controller for station management endpoints.
 */
@RestController
@RequestMapping("/api/stations")
@Tag(name = "Stations", description = "Station management endpoints")
public class StationController {

	private static final Logger logger = LoggerFactory.getLogger(StationController.class);

	private final StationService stationService;

	public StationController(StationService stationService) {
		this.stationService = stationService;
	}

	@PostMapping
	@Operation(summary = "Create a new station", description = "Creates a new broadcast station")
	@ApiResponse(responseCode = "201", description = "Station created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data or call sign already exists")
	public ResponseEntity<StationResponseDTO> create(@Valid @RequestBody StationRequestDTO request) {
		logger.info("POST /api/stations - Creating station: {}", request.getCallSign());
		StationResponseDTO response = stationService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get station by ID", description = "Retrieves a station by its UUID")
	@ApiResponse(responseCode = "200", description = "Station found")
	@ApiResponse(responseCode = "404", description = "Station not found")
	public ResponseEntity<StationResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/stations/{} - Fetching station", id);
		StationResponseDTO response = stationService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(summary = "Get all stations", description = "Retrieves all stations the user has access to based on their station assignments")
	@ApiResponse(responseCode = "200", description = "Stations retrieved successfully")
	public ResponseEntity<List<StationResponseDTO>> getAll() {
		logger.debug("GET /api/stations - Fetching all stations");
		List<StationResponseDTO> response = stationService.getAll();
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update station", description = "Updates an existing station")
	@ApiResponse(responseCode = "200", description = "Station updated successfully")
	@ApiResponse(responseCode = "404", description = "Station not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<StationResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody StationRequestDTO request) {
		logger.info("PUT /api/stations/{} - Updating station", id);
		StationResponseDTO response = stationService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete station", description = "Deletes a station by its UUID")
	@ApiResponse(responseCode = "204", description = "Station deleted successfully")
	@ApiResponse(responseCode = "404", description = "Station not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/stations/{} - Deleting station", id);
		stationService.delete(id);
		return ResponseEntity.noContent().build();
	}

}

