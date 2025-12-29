package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.GridDaypartMappingRequestDTO;
import com.onelpro.librelog.dto.GridRequestDTO;
import com.onelpro.librelog.dto.GridResponseDTO;
import com.onelpro.librelog.services.GridService;
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
 * REST controller for grid management endpoints.
 */
@RestController
@RequestMapping("/api/grids")
@Tag(name = "Grids", description = "Weekly grid and daypart mapping management endpoints")
public class GridController {

	private static final Logger logger = LoggerFactory.getLogger(GridController.class);

	private final GridService gridService;

	public GridController(GridService gridService) {
		this.gridService = gridService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new grid",
			description = "Creates a new weekly grid for a channel"
	)
	@ApiResponse(responseCode = "201", description = "Grid created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Channel not found")
	public ResponseEntity<GridResponseDTO> create(@Valid @RequestBody GridRequestDTO request) {
		logger.info("POST /api/grids - Creating grid: {}", request.getName());
		GridResponseDTO response = gridService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get grid by ID",
			description = "Retrieves a grid by its UUID, including all daypart mappings"
	)
	@ApiResponse(responseCode = "200", description = "Grid found")
	@ApiResponse(responseCode = "404", description = "Grid not found")
	public ResponseEntity<GridResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/grids/{} - Fetching grid", id);
		GridResponseDTO response = gridService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "Get all grids",
			description = "Retrieves all grids, optionally filtered by channel ID"
	)
	@ApiResponse(responseCode = "200", description = "Grids retrieved successfully")
	public ResponseEntity<List<GridResponseDTO>> getAll(@RequestParam(required = false) UUID channelId) {
		logger.debug("GET /api/grids - Fetching grids");
		List<GridResponseDTO> response;
		if (channelId != null) {
			response = gridService.getByChannelId(channelId);
		} else {
			response = gridService.getAll();
		}
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update grid",
			description = "Updates an existing grid"
	)
	@ApiResponse(responseCode = "200", description = "Grid updated successfully")
	@ApiResponse(responseCode = "404", description = "Grid not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<GridResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody GridRequestDTO request) {
		logger.info("PUT /api/grids/{} - Updating grid", id);
		GridResponseDTO response = gridService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete grid",
			description = "Deletes a grid by its UUID, including all daypart mappings"
	)
	@ApiResponse(responseCode = "204", description = "Grid deleted successfully")
	@ApiResponse(responseCode = "404", description = "Grid not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/grids/{} - Deleting grid", id);
		gridService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/daypart-mappings")
	@Operation(
			summary = "Add daypart mapping to grid",
			description = "Assigns a daypart to a specific day of the week within a grid"
	)
	@ApiResponse(responseCode = "201", description = "Daypart mapping created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Grid or daypart not found")
	@ApiResponse(responseCode = "409", description = "Daypart mapping already exists")
	public ResponseEntity<GridResponseDTO.GridDaypartMappingResponseDTO> addDaypartMapping(
			@Valid @RequestBody GridDaypartMappingRequestDTO request) {
		logger.info("POST /api/grids/daypart-mappings - Adding daypart mapping");
		GridResponseDTO.GridDaypartMappingResponseDTO response = gridService.addDaypartMapping(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@DeleteMapping("/daypart-mappings/{mappingId}")
	@Operation(
			summary = "Remove daypart mapping from grid",
			description = "Removes a daypart mapping from a grid"
	)
	@ApiResponse(responseCode = "204", description = "Daypart mapping deleted successfully")
	@ApiResponse(responseCode = "404", description = "Daypart mapping not found")
	public ResponseEntity<Void> removeDaypartMapping(@PathVariable UUID mappingId) {
		logger.info("DELETE /api/grids/daypart-mappings/{} - Removing daypart mapping", mappingId);
		gridService.removeDaypartMapping(mappingId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{gridId}/daypart-mappings")
	@Operation(
			summary = "Get daypart mappings for a grid",
			description = "Retrieves all daypart mappings for a specific grid"
	)
	@ApiResponse(responseCode = "200", description = "Daypart mappings retrieved successfully")
	public ResponseEntity<List<GridResponseDTO.GridDaypartMappingResponseDTO>> getDaypartMappings(
			@PathVariable UUID gridId) {
		logger.debug("GET /api/grids/{}/daypart-mappings - Fetching daypart mappings", gridId);
		List<GridResponseDTO.GridDaypartMappingResponseDTO> response = gridService.getDaypartMappingsByGridId(gridId);
		return ResponseEntity.ok(response);
	}

}

