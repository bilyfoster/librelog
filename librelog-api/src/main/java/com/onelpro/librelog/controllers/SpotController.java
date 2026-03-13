package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.SpotRequestDTO;
import com.onelpro.librelog.dto.SpotResponseDTO;
import com.onelpro.librelog.services.SpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for spot management endpoints.
 */
@RestController
@RequestMapping("/api/spots")
@Tag(name = "Spots", description = "Spot scheduling and management endpoints")
public class SpotController {

	private static final Logger logger = LoggerFactory.getLogger(SpotController.class);

	private final SpotService spotService;

	public SpotController(SpotService spotService) {
		this.spotService = spotService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new spot",
			description = "Creates a new spot occurrence for a campaign"
	)
	@ApiResponse(responseCode = "201", description = "Spot created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<SpotResponseDTO> create(@Valid @RequestBody SpotRequestDTO request) {
		logger.info("POST /api/spots - Creating spot for campaign: {}", request.getCampaignId());
		SpotResponseDTO response = spotService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get spot by ID",
			description = "Retrieves a spot by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Spot found")
	@ApiResponse(responseCode = "404", description = "Spot not found")
	public ResponseEntity<SpotResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/spots/{} - Fetching spot", id);
		SpotResponseDTO response = spotService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/campaign/{campaignId}")
	@Operation(
			summary = "Get spots by campaign",
			description = "Retrieves all spots for a specific campaign"
	)
	@ApiResponse(responseCode = "200", description = "Spots retrieved successfully")
	public ResponseEntity<List<SpotResponseDTO>> getByCampaignId(@PathVariable UUID campaignId) {
		logger.debug("GET /api/spots/campaign/{} - Fetching spots", campaignId);
		List<SpotResponseDTO> response = spotService.getByCampaignId(campaignId);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/station/{stationId}/date/{date}")
	@Operation(
			summary = "Get spots by station and date",
			description = "Retrieves all spots scheduled for a station on a specific date"
	)
	@ApiResponse(responseCode = "200", description = "Spots retrieved successfully")
	public ResponseEntity<List<SpotResponseDTO>> getByStationAndDate(
			@PathVariable UUID stationId,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		logger.debug("GET /api/spots/station/{}/date/{} - Fetching spots", stationId, date);
		List<SpotResponseDTO> response = spotService.getByStationAndDate(stationId, date);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/station/{stationId}/range")
	@Operation(
			summary = "Get spots by date range",
			description = "Retrieves all spots for a station within a date range"
	)
	@ApiResponse(responseCode = "200", description = "Spots retrieved successfully")
	public ResponseEntity<List<SpotResponseDTO>> getByStationAndDateRange(
			@PathVariable UUID stationId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		logger.debug("GET /api/spots/station/{}/range - Fetching spots from {} to {}", stationId, startDate, endDate);
		List<SpotResponseDTO> response = spotService.getByStationAndDateRange(stationId, startDate, endDate);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update spot",
			description = "Updates an existing spot"
	)
	@ApiResponse(responseCode = "200", description = "Spot updated successfully")
	@ApiResponse(responseCode = "404", description = "Spot not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<SpotResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody SpotRequestDTO request) {
		logger.info("PUT /api/spots/{} - Updating spot", id);
		SpotResponseDTO response = spotService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete spot",
			description = "Deletes a spot by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Spot deleted successfully")
	@ApiResponse(responseCode = "404", description = "Spot not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/spots/{} - Deleting spot", id);
		spotService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/status")
	@Operation(
			summary = "Update spot status",
			description = "Updates the status of a spot"
	)
	@ApiResponse(responseCode = "200", description = "Status updated successfully")
	@ApiResponse(responseCode = "404", description = "Spot not found")
	public ResponseEntity<SpotResponseDTO> updateStatus(
			@PathVariable UUID id,
			@RequestParam String status) {
		logger.info("PATCH /api/spots/{}/status - Updating status to {}", id, status);
		SpotResponseDTO response = spotService.updateStatus(id, status);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{id}/air")
	@Operation(
			summary = "Mark spot as aired",
			description = "Marks a spot as having been aired"
	)
	@ApiResponse(responseCode = "200", description = "Spot marked as aired")
	@ApiResponse(responseCode = "404", description = "Spot not found")
	public ResponseEntity<SpotResponseDTO> markAsAired(@PathVariable UUID id) {
		logger.info("POST /api/spots/{}/air - Marking as aired", id);
		SpotResponseDTO response = spotService.markAsAired(id);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{missedSpotId}/makegood")
	@Operation(
			summary = "Create makegood spot",
			description = "Creates a makegood spot for a missed spot"
	)
	@ApiResponse(responseCode = "201", description = "Makegood created successfully")
	@ApiResponse(responseCode = "404", description = "Missed spot not found")
	public ResponseEntity<SpotResponseDTO> createMakegood(
			@PathVariable UUID missedSpotId,
			@Valid @RequestBody SpotRequestDTO request) {
		logger.info("POST /api/spots/{}/makegood - Creating makegood", missedSpotId);
		SpotResponseDTO response = spotService.createMakegood(missedSpotId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

}
