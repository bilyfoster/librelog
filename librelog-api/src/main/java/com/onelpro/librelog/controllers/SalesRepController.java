package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.SalesRepRequestDTO;
import com.onelpro.librelog.dto.SalesRepResponseDTO;
import com.onelpro.librelog.services.SalesRepService;
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
 * REST controller for sales representative management endpoints.
 */
@RestController
@RequestMapping("/api/sales-reps")
@Tag(name = "Sales Representatives", description = "Sales representative management endpoints")
public class SalesRepController {

	private static final Logger logger = LoggerFactory.getLogger(SalesRepController.class);

	private final SalesRepService salesRepService;

	public SalesRepController(SalesRepService salesRepService) {
		this.salesRepService = salesRepService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new sales representative",
			description = "Creates a new sales representative"
	)
	@ApiResponse(responseCode = "201", description = "Sales rep created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data or email already exists")
	@ApiResponse(responseCode = "404", description = "Sales team, office, or region not found")
	public ResponseEntity<SalesRepResponseDTO> create(@Valid @RequestBody SalesRepRequestDTO request) {
		logger.info("POST /api/sales-reps - Creating sales rep: {} {}", request.getFirstName(), request.getLastName());
		SalesRepResponseDTO response = salesRepService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get sales representative by ID",
			description = "Retrieves a sales representative by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Sales rep found")
	@ApiResponse(responseCode = "404", description = "Sales rep not found")
	public ResponseEntity<SalesRepResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/sales-reps/{} - Fetching sales rep", id);
		SalesRepResponseDTO response = salesRepService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "Get all sales representatives",
			description = "Retrieves all sales representatives, optionally filtered by sales team ID"
	)
	@ApiResponse(responseCode = "200", description = "Sales reps retrieved successfully")
	public ResponseEntity<List<SalesRepResponseDTO>> getAll(@RequestParam(required = false) UUID salesTeamId) {
		logger.debug("GET /api/sales-reps - Fetching sales reps");
		List<SalesRepResponseDTO> response;
		if (salesTeamId != null) {
			response = salesRepService.getBySalesTeamId(salesTeamId);
		} else {
			response = salesRepService.getAll();
		}
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update sales representative",
			description = "Updates an existing sales representative"
	)
	@ApiResponse(responseCode = "200", description = "Sales rep updated successfully")
	@ApiResponse(responseCode = "404", description = "Sales rep not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<SalesRepResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody SalesRepRequestDTO request) {
		logger.info("PUT /api/sales-reps/{} - Updating sales rep", id);
		SalesRepResponseDTO response = salesRepService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete sales representative",
			description = "Deletes a sales representative by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Sales rep deleted successfully")
	@ApiResponse(responseCode = "404", description = "Sales rep not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/sales-reps/{} - Deleting sales rep", id);
		salesRepService.delete(id);
		return ResponseEntity.noContent().build();
	}

}

