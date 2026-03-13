package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.CampaignRequestDTO;
import com.onelpro.librelog.dto.CampaignResponseDTO;
import com.onelpro.librelog.services.CampaignService;
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
 * REST controller for campaign management endpoints.
 */
@RestController
@RequestMapping("/api/campaigns")
@Tag(name = "Campaigns", description = "Campaign management endpoints for advertising traffic")
public class CampaignController {

	private static final Logger logger = LoggerFactory.getLogger(CampaignController.class);

	private final CampaignService campaignService;

	public CampaignController(CampaignService campaignService) {
		this.campaignService = campaignService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new campaign",
			description = "Creates a new advertising campaign for traffic scheduling"
	)
	@ApiResponse(responseCode = "201", description = "Campaign created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<CampaignResponseDTO> create(@Valid @RequestBody CampaignRequestDTO request) {
		logger.info("POST /api/campaigns - Creating campaign: {}", request.getName());
		CampaignResponseDTO response = campaignService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get campaign by ID",
			description = "Retrieves a campaign by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Campaign found")
	@ApiResponse(responseCode = "404", description = "Campaign not found")
	public ResponseEntity<CampaignResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/campaigns/{} - Fetching campaign", id);
		CampaignResponseDTO response = campaignService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "Get all campaigns",
			description = "Retrieves all campaigns, optionally filtered by station ID"
	)
	@ApiResponse(responseCode = "200", description = "Campaigns retrieved successfully")
	public ResponseEntity<List<CampaignResponseDTO>> getAll(
			@RequestParam(required = false) UUID stationId) {
		logger.debug("GET /api/campaigns - Fetching campaigns");
		List<CampaignResponseDTO> response;
		if (stationId != null) {
			response = campaignService.getByStationId(stationId);
		} else {
			response = campaignService.getAll();
		}
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update campaign",
			description = "Updates an existing campaign"
	)
	@ApiResponse(responseCode = "200", description = "Campaign updated successfully")
	@ApiResponse(responseCode = "404", description = "Campaign not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<CampaignResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody CampaignRequestDTO request) {
		logger.info("PUT /api/campaigns/{} - Updating campaign", id);
		CampaignResponseDTO response = campaignService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete campaign",
			description = "Deletes a campaign by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Campaign deleted successfully")
	@ApiResponse(responseCode = "404", description = "Campaign not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/campaigns/{} - Deleting campaign", id);
		campaignService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/status")
	@Operation(
			summary = "Update campaign status",
			description = "Updates the status of a campaign (DRAFT, ACTIVE, PAUSED, etc.)"
	)
	@ApiResponse(responseCode = "200", description = "Status updated successfully")
	@ApiResponse(responseCode = "404", description = "Campaign not found")
	public ResponseEntity<CampaignResponseDTO> updateStatus(
			@PathVariable UUID id,
			@RequestParam String status) {
		logger.info("PATCH /api/campaigns/{}/status - Updating status to {}", id, status);
		CampaignResponseDTO response = campaignService.updateStatus(id, status);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/station/{stationId}/active")
	@Operation(
			summary = "Get active campaigns",
			description = "Retrieves all active campaigns for a station"
	)
	@ApiResponse(responseCode = "200", description = "Active campaigns retrieved successfully")
	public ResponseEntity<List<CampaignResponseDTO>> getActiveCampaigns(
			@PathVariable UUID stationId) {
		logger.debug("GET /api/campaigns/station/{}/active - Fetching active campaigns", stationId);
		List<CampaignResponseDTO> response = campaignService.getActiveCampaigns(stationId);
		return ResponseEntity.ok(response);
	}

}
