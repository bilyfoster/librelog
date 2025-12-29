package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.DaypartAssignmentRequestDTO;
import com.onelpro.librelog.dto.DaypartAssignmentResponseDTO;
import com.onelpro.librelog.services.DaypartAssignmentService;
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
 * REST controller for daypart assignment management endpoints.
 */
@RestController
@RequestMapping("/api/daypart-assignments")
@Tag(name = "Daypart Assignments", description = "Daypart-to-clock template assignment management endpoints")
public class DaypartAssignmentController {

	private static final Logger logger = LoggerFactory.getLogger(DaypartAssignmentController.class);

	private final DaypartAssignmentService daypartAssignmentService;

	public DaypartAssignmentController(DaypartAssignmentService daypartAssignmentService) {
		this.daypartAssignmentService = daypartAssignmentService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new daypart assignment",
			description = "Assigns a clock template to a daypart"
	)
	@ApiResponse(responseCode = "201", description = "Daypart assignment created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Daypart or clock template not found")
	@ApiResponse(responseCode = "409", description = "Daypart assignment already exists")
	public ResponseEntity<DaypartAssignmentResponseDTO> create(@Valid @RequestBody DaypartAssignmentRequestDTO request) {
		logger.info("POST /api/daypart-assignments - Creating daypart assignment");
		DaypartAssignmentResponseDTO response = daypartAssignmentService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get daypart assignment by ID",
			description = "Retrieves a daypart assignment by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Daypart assignment found")
	@ApiResponse(responseCode = "404", description = "Daypart assignment not found")
	public ResponseEntity<DaypartAssignmentResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/daypart-assignments/{} - Fetching daypart assignment", id);
		DaypartAssignmentResponseDTO response = daypartAssignmentService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "Get daypart assignments",
			description = "Retrieves daypart assignments, optionally filtered by daypart ID or clock template ID"
	)
	@ApiResponse(responseCode = "200", description = "Daypart assignments retrieved successfully")
	public ResponseEntity<List<DaypartAssignmentResponseDTO>> getAll(
			@RequestParam(required = false) UUID daypartId,
			@RequestParam(required = false) UUID clockTemplateId) {
		logger.debug("GET /api/daypart-assignments - Fetching daypart assignments");
		List<DaypartAssignmentResponseDTO> response;
		if (daypartId != null) {
			response = daypartAssignmentService.getByDaypartId(daypartId);
		} else if (clockTemplateId != null) {
			response = daypartAssignmentService.getByClockTemplateId(clockTemplateId);
		} else {
			// Return all assignments (if needed, add getAll method to service)
			response = List.of();
		}
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update daypart assignment",
			description = "Updates an existing daypart assignment"
	)
	@ApiResponse(responseCode = "200", description = "Daypart assignment updated successfully")
	@ApiResponse(responseCode = "404", description = "Daypart assignment not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "409", description = "Daypart assignment already exists")
	public ResponseEntity<DaypartAssignmentResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody DaypartAssignmentRequestDTO request) {
		logger.info("PUT /api/daypart-assignments/{} - Updating daypart assignment", id);
		DaypartAssignmentResponseDTO response = daypartAssignmentService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete daypart assignment",
			description = "Deletes a daypart assignment by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Daypart assignment deleted successfully")
	@ApiResponse(responseCode = "404", description = "Daypart assignment not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/daypart-assignments/{} - Deleting daypart assignment", id);
		daypartAssignmentService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/daypart/{daypartId}")
	@Operation(
			summary = "Delete all assignments for a daypart",
			description = "Deletes all daypart assignments for a specific daypart"
	)
	@ApiResponse(responseCode = "204", description = "Daypart assignments deleted successfully")
	public ResponseEntity<Void> deleteByDaypartId(@PathVariable UUID daypartId) {
		logger.info("DELETE /api/daypart-assignments/daypart/{} - Deleting all assignments for daypart", daypartId);
		daypartAssignmentService.deleteByDaypartId(daypartId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/clock-template/{clockTemplateId}")
	@Operation(
			summary = "Delete all assignments for a clock template",
			description = "Deletes all daypart assignments for a specific clock template"
	)
	@ApiResponse(responseCode = "204", description = "Daypart assignments deleted successfully")
	public ResponseEntity<Void> deleteByClockTemplateId(@PathVariable UUID clockTemplateId) {
		logger.info("DELETE /api/daypart-assignments/clock-template/{} - Deleting all assignments for clock template", clockTemplateId);
		daypartAssignmentService.deleteByClockTemplateId(clockTemplateId);
		return ResponseEntity.noContent().build();
	}

}

