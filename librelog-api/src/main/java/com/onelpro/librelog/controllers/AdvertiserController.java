package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.AdvertiserRequestDTO;
import com.onelpro.librelog.dto.AdvertiserResponseDTO;
import com.onelpro.librelog.services.AdvertiserService;
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
 * REST controller for advertiser management endpoints.
 */
@RestController
@RequestMapping("/api/advertisers")
@Tag(name = "Advertisers", description = "Advertiser management endpoints")
public class AdvertiserController {

	private static final Logger logger = LoggerFactory.getLogger(AdvertiserController.class);

	private final AdvertiserService advertiserService;

	public AdvertiserController(AdvertiserService advertiserService) {
		this.advertiserService = advertiserService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new advertiser",
			description = "Creates a new advertiser client"
	)
	@ApiResponse(responseCode = "201", description = "Advertiser created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Agency or sales rep not found")
	public ResponseEntity<AdvertiserResponseDTO> create(@Valid @RequestBody AdvertiserRequestDTO request) {
		logger.info("POST /api/advertisers - Creating advertiser: {}", request.getName());
		AdvertiserResponseDTO response = advertiserService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get advertiser by ID",
			description = "Retrieves an advertiser by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Advertiser found")
	@ApiResponse(responseCode = "404", description = "Advertiser not found")
	public ResponseEntity<AdvertiserResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/advertisers/{} - Fetching advertiser", id);
		AdvertiserResponseDTO response = advertiserService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "Get all advertisers",
			description = "Retrieves all advertisers, optionally filtered by agency ID or sales rep ID"
	)
	@ApiResponse(responseCode = "200", description = "Advertisers retrieved successfully")
	public ResponseEntity<List<AdvertiserResponseDTO>> getAll(
			@RequestParam(required = false) UUID agencyId,
			@RequestParam(required = false) UUID salesRepId) {
		logger.debug("GET /api/advertisers - Fetching advertisers");
		List<AdvertiserResponseDTO> response;
		if (agencyId != null) {
			response = advertiserService.getByAgencyId(agencyId);
		} else if (salesRepId != null) {
			response = advertiserService.getBySalesRepId(salesRepId);
		} else {
			response = advertiserService.getAll();
		}
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update advertiser",
			description = "Updates an existing advertiser"
	)
	@ApiResponse(responseCode = "200", description = "Advertiser updated successfully")
	@ApiResponse(responseCode = "404", description = "Advertiser not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<AdvertiserResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody AdvertiserRequestDTO request) {
		logger.info("PUT /api/advertisers/{} - Updating advertiser", id);
		AdvertiserResponseDTO response = advertiserService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete advertiser",
			description = "Deletes an advertiser by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Advertiser deleted successfully")
	@ApiResponse(responseCode = "404", description = "Advertiser not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/advertisers/{} - Deleting advertiser", id);
		advertiserService.delete(id);
		return ResponseEntity.noContent().build();
	}

}

