package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.ChannelRequestDTO;
import com.onelpro.librelog.dto.ChannelResponseDTO;
import com.onelpro.librelog.services.ChannelService;
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
 * REST controller for channel management endpoints.
 */
@RestController
@RequestMapping("/api/channels")
@Tag(name = "Channels", description = "Channel management endpoints")
public class ChannelController {

	private static final Logger logger = LoggerFactory.getLogger(ChannelController.class);

	private final ChannelService channelService;

	public ChannelController(ChannelService channelService) {
		this.channelService = channelService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new channel",
			description = "Creates a new channel for a station"
	)
	@ApiResponse(responseCode = "201", description = "Channel created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Station not found")
	public ResponseEntity<ChannelResponseDTO> create(@Valid @RequestBody ChannelRequestDTO request) {
		logger.info("POST /api/channels - Creating channel: {}", request.getName());
		ChannelResponseDTO response = channelService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get channel by ID",
			description = "Retrieves a channel by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Channel found")
	@ApiResponse(responseCode = "404", description = "Channel not found")
	public ResponseEntity<ChannelResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/channels/{} - Fetching channel", id);
		ChannelResponseDTO response = channelService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "Get all channels",
			description = "Retrieves all channels, optionally filtered by station ID"
	)
	@ApiResponse(responseCode = "200", description = "Channels retrieved successfully")
	public ResponseEntity<List<ChannelResponseDTO>> getAll(@RequestParam(required = false) UUID stationId) {
		logger.debug("GET /api/channels - Fetching channels, stationId: {}", stationId);
		List<ChannelResponseDTO> response;
		if (stationId != null) {
			response = channelService.getByStationId(stationId);
		} else {
			response = channelService.getAll();
		}
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update channel",
			description = "Updates an existing channel"
	)
	@ApiResponse(responseCode = "200", description = "Channel updated successfully")
	@ApiResponse(responseCode = "404", description = "Channel not found or Station not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<ChannelResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody ChannelRequestDTO request) {
		logger.info("PUT /api/channels/{} - Updating channel", id);
		ChannelResponseDTO response = channelService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete channel",
			description = "Deletes a channel by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Channel deleted successfully")
	@ApiResponse(responseCode = "404", description = "Channel not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/channels/{} - Deleting channel", id);
		channelService.delete(id);
		return ResponseEntity.noContent().build();
	}

}

