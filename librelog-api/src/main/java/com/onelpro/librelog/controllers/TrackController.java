package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.TrackRequestDTO;
import com.onelpro.librelog.dto.TrackResponseDTO;
import com.onelpro.librelog.enums.TrackType;
import com.onelpro.librelog.services.TrackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for track/music library management endpoints.
 */
@RestController
@RequestMapping("/api/tracks")
@Tag(name = "Tracks", description = "Music library and track management endpoints")
public class TrackController {

	private static final Logger logger = LoggerFactory.getLogger(TrackController.class);

	private final TrackService trackService;

	public TrackController(TrackService trackService) {
		this.trackService = trackService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new track",
			description = "Creates a new track in the music library"
	)
	@ApiResponse(responseCode = "201", description = "Track created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<TrackResponseDTO> create(@Valid @RequestBody TrackRequestDTO request) {
		logger.info("POST /api/tracks - Creating track: {}", request.getTitle());
		TrackResponseDTO response = trackService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get track by ID",
			description = "Retrieves a track by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Track found")
	@ApiResponse(responseCode = "404", description = "Track not found")
	public ResponseEntity<TrackResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/tracks/{} - Fetching track", id);
		TrackResponseDTO response = trackService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "Get all tracks",
			description = "Retrieves all tracks, optionally filtered by station ID"
	)
	@ApiResponse(responseCode = "200", description = "Tracks retrieved successfully")
	public ResponseEntity<List<TrackResponseDTO>> getAll(
			@RequestParam(required = false) UUID stationId) {
		logger.debug("GET /api/tracks - Fetching tracks");
		List<TrackResponseDTO> response;
		if (stationId != null) {
			response = trackService.getByStationId(stationId);
		} else {
			response = trackService.getAll();
		}
		return ResponseEntity.ok(response);
	}

	@GetMapping("/station/{stationId}/type/{type}")
	@Operation(
			summary = "Get tracks by type",
			description = "Retrieves tracks for a station filtered by type (MUSIC, JINGLE, etc.)"
	)
	@ApiResponse(responseCode = "200", description = "Tracks retrieved successfully")
	public ResponseEntity<List<TrackResponseDTO>> getByType(
			@PathVariable UUID stationId,
			@PathVariable TrackType type) {
		logger.debug("GET /api/tracks/station/{}/type/{} - Fetching tracks", stationId, type);
		List<TrackResponseDTO> response = trackService.getByType(stationId, type);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update track",
			description = "Updates an existing track"
	)
	@ApiResponse(responseCode = "200", description = "Track updated successfully")
	@ApiResponse(responseCode = "404", description = "Track not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<TrackResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody TrackRequestDTO request) {
		logger.info("PUT /api/tracks/{} - Updating track", id);
		TrackResponseDTO response = trackService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete track",
			description = "Deletes a track by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Track deleted successfully")
	@ApiResponse(responseCode = "404", description = "Track not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/tracks/{} - Deleting track", id);
		trackService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/search")
	@Operation(
			summary = "Search tracks",
			description = "Searches tracks by title or artist"
	)
	@ApiResponse(responseCode = "200", description = "Search results retrieved")
	public ResponseEntity<List<TrackResponseDTO>> search(
			@RequestParam String query) {
		logger.debug("GET /api/tracks/search - Searching: {}", query);
		List<TrackResponseDTO> response = trackService.search(query);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{id}/play")
	@Operation(
			summary = "Record track play",
			description = "Records that a track was played (increments play count)"
	)
	@ApiResponse(responseCode = "200", description = "Play recorded successfully")
	@ApiResponse(responseCode = "404", description = "Track not found")
	public ResponseEntity<TrackResponseDTO> recordPlay(@PathVariable UUID id) {
		logger.info("POST /api/tracks/{}/play - Recording play", id);
		TrackResponseDTO response = trackService.recordPlay(id);
		return ResponseEntity.ok(response);
	}

}
