package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.VoiceTrackRequestDTO;
import com.onelpro.librelog.dto.VoiceTrackResponseDTO;
import com.onelpro.librelog.services.VoiceTrackService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for voice track management endpoints.
 */
@RestController
@RequestMapping("/api/voice")
@Tag(name = "Voice Tracks", description = "Voice track (DJ recorded segment) management")
public class VoiceTrackController {

	private static final Logger logger = LoggerFactory.getLogger(VoiceTrackController.class);

	private final VoiceTrackService voiceTrackService;

	public VoiceTrackController(VoiceTrackService voiceTrackService) {
		this.voiceTrackService = voiceTrackService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new voice track",
			description = "Creates a new voice track entry for recording"
	)
	@ApiResponse(responseCode = "201", description = "Voice track created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<VoiceTrackResponseDTO> create(@Valid @RequestBody VoiceTrackRequestDTO request) {
		logger.info("POST /api/voice - Creating voice track: {}", request.getTitle());
		VoiceTrackResponseDTO response = voiceTrackService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get voice track by ID",
			description = "Retrieves a voice track by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Voice track found")
	@ApiResponse(responseCode = "404", description = "Voice track not found")
	public ResponseEntity<VoiceTrackResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/voice/{} - Fetching voice track", id);
		VoiceTrackResponseDTO response = voiceTrackService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "Get all voice tracks",
			description = "Retrieves all voice tracks, optionally filtered by station ID"
	)
	@ApiResponse(responseCode = "200", description = "Voice tracks retrieved successfully")
	public ResponseEntity<List<VoiceTrackResponseDTO>> getAll(
			@RequestParam(required = false) UUID stationId) {
		logger.debug("GET /api/voice - Fetching voice tracks");
		List<VoiceTrackResponseDTO> response;
		if (stationId != null) {
			response = voiceTrackService.getByStationId(stationId);
		} else {
			response = voiceTrackService.getAll();
		}
		return ResponseEntity.ok(response);
	}

	@GetMapping("/station/{stationId}/date/{date}")
	@Operation(
			summary = "Get voice tracks by date",
			description = "Retrieves voice tracks scheduled for a specific date"
	)
	@ApiResponse(responseCode = "200", description = "Voice tracks retrieved successfully")
	public ResponseEntity<List<VoiceTrackResponseDTO>> getByStationAndDate(
			@PathVariable UUID stationId,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		logger.debug("GET /api/voice/station/{}/date/{} - Fetching voice tracks", stationId, date);
		List<VoiceTrackResponseDTO> response = voiceTrackService.getByStationAndDate(stationId, date);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/station/{stationId}/upcoming")
	@Operation(
			summary = "Get upcoming voice tracks",
			description = "Retrieves upcoming scheduled voice tracks for a station"
	)
	@ApiResponse(responseCode = "200", description = "Voice tracks retrieved successfully")
	public ResponseEntity<List<VoiceTrackResponseDTO>> getUpcoming(@PathVariable UUID stationId) {
		logger.debug("GET /api/voice/station/{}/upcoming - Fetching upcoming voice tracks", stationId);
		List<VoiceTrackResponseDTO> response = voiceTrackService.getUpcoming(stationId);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update voice track",
			description = "Updates an existing voice track"
	)
	@ApiResponse(responseCode = "200", description = "Voice track updated successfully")
	@ApiResponse(responseCode = "404", description = "Voice track not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<VoiceTrackResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody VoiceTrackRequestDTO request) {
		logger.info("PUT /api/voice/{} - Updating voice track", id);
		VoiceTrackResponseDTO response = voiceTrackService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete voice track",
			description = "Deletes a voice track by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Voice track deleted successfully")
	@ApiResponse(responseCode = "404", description = "Voice track not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/voice/{} - Deleting voice track", id);
		voiceTrackService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/status")
	@Operation(
			summary = "Update voice track status",
			description = "Updates the status of a voice track (DRAFT, RECORDED, APPROVED, SCHEDULED, AIRED)"
	)
	@ApiResponse(responseCode = "200", description = "Status updated successfully")
	@ApiResponse(responseCode = "404", description = "Voice track not found")
	public ResponseEntity<VoiceTrackResponseDTO> updateStatus(
			@PathVariable UUID id,
			@RequestParam String status) {
		logger.info("PATCH /api/voice/{}/status - Updating status to {}", id, status);
		VoiceTrackResponseDTO response = voiceTrackService.updateStatus(id, status);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{id}/record")
	@Operation(
			summary = "Mark voice track as recorded",
			description = "Marks a voice track as recorded with file URL and duration"
	)
	@ApiResponse(responseCode = "200", description = "Voice track marked as recorded")
	@ApiResponse(responseCode = "404", description = "Voice track not found")
	public ResponseEntity<VoiceTrackResponseDTO> markAsRecorded(
			@PathVariable UUID id,
			@RequestBody Map<String, Object> request) {
		logger.info("POST /api/voice/{}/record - Marking as recorded", id);
		String fileUrl = (String) request.get("fileUrl");
		Integer durationSeconds = (Integer) request.get("durationSeconds");
		VoiceTrackResponseDTO response = voiceTrackService.markAsRecorded(id, fileUrl, durationSeconds);
		return ResponseEntity.ok(response);
	}

}
