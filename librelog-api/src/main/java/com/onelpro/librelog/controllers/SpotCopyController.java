package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.SpotCopyRequestDTO;
import com.onelpro.librelog.dto.SpotCopyResponseDTO;
import com.onelpro.librelog.services.SpotCopyService;
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
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for spot copy/script management endpoints.
 */
@RestController
@RequestMapping("/api/copy")
@Tag(name = "Spot Copy", description = "Spot copy and script management")
public class SpotCopyController {

	private static final Logger logger = LoggerFactory.getLogger(SpotCopyController.class);

	private final SpotCopyService spotCopyService;

	public SpotCopyController(SpotCopyService spotCopyService) {
		this.spotCopyService = spotCopyService;
	}

	@PostMapping
	@Operation(
			summary = "Create new spot copy",
			description = "Creates a new copy/script version for a campaign"
	)
	@ApiResponse(responseCode = "201", description = "Copy created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<SpotCopyResponseDTO> create(@Valid @RequestBody SpotCopyRequestDTO request) {
		logger.info("POST /api/copy - Creating spot copy for campaign: {}", request.getCampaignId());
		SpotCopyResponseDTO response = spotCopyService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get spot copy by ID",
			description = "Retrieves a spot copy by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Copy found")
	@ApiResponse(responseCode = "404", description = "Copy not found")
	public ResponseEntity<SpotCopyResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/copy/{} - Fetching spot copy", id);
		SpotCopyResponseDTO response = spotCopyService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/campaign/{campaignId}")
	@Operation(
			summary = "Get copy by campaign",
			description = "Retrieves all copy versions for a specific campaign"
	)
	@ApiResponse(responseCode = "200", description = "Copy retrieved successfully")
	public ResponseEntity<List<SpotCopyResponseDTO>> getByCampaignId(@PathVariable UUID campaignId) {
		logger.debug("GET /api/copy/campaign/{} - Fetching spot copies", campaignId);
		List<SpotCopyResponseDTO> response = spotCopyService.getByCampaignId(campaignId);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/campaign/{campaignId}/latest-approved")
	@Operation(
			summary = "Get latest approved copy",
			description = "Retrieves the latest approved copy for a campaign"
	)
	@ApiResponse(responseCode = "200", description = "Copy retrieved successfully")
	@ApiResponse(responseCode = "404", description = "No approved copy found")
	public ResponseEntity<SpotCopyResponseDTO> getLatestApproved(@PathVariable UUID campaignId) {
		logger.debug("GET /api/copy/campaign/{}/latest-approved - Fetching latest approved copy", campaignId);
		SpotCopyResponseDTO response = spotCopyService.getLatestApprovedByCampaignId(campaignId);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update spot copy",
			description = "Updates an existing spot copy (only if not approved)"
	)
	@ApiResponse(responseCode = "200", description = "Copy updated successfully")
	@ApiResponse(responseCode = "404", description = "Copy not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "409", description = "Cannot update approved copy")
	public ResponseEntity<SpotCopyResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody SpotCopyRequestDTO request) {
		logger.info("PUT /api/copy/{} - Updating spot copy", id);
		SpotCopyResponseDTO response = spotCopyService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete spot copy",
			description = "Deletes a spot copy by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Copy deleted successfully")
	@ApiResponse(responseCode = "404", description = "Copy not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/copy/{} - Deleting spot copy", id);
		spotCopyService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/approve")
	@Operation(
			summary = "Approve spot copy",
			description = "Approves a spot copy for use"
	)
	@ApiResponse(responseCode = "200", description = "Copy approved successfully")
	@ApiResponse(responseCode = "404", description = "Copy not found")
	public ResponseEntity<SpotCopyResponseDTO> approve(@PathVariable UUID id) {
		logger.info("POST /api/copy/{}/approve - Approving spot copy", id);
		SpotCopyResponseDTO response = spotCopyService.approve(id);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{id}/reject")
	@Operation(
			summary = "Reject spot copy",
			description = "Rejects a spot copy with a reason"
	)
	@ApiResponse(responseCode = "200", description = "Copy rejected successfully")
	@ApiResponse(responseCode = "404", description = "Copy not found")
	public ResponseEntity<SpotCopyResponseDTO> reject(
			@PathVariable UUID id,
			@RequestBody Map<String, String> request) {
		logger.info("POST /api/copy/{}/reject - Rejecting spot copy", id);
		String reason = request.get("reason");
		SpotCopyResponseDTO response = spotCopyService.reject(id, reason);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/pending-approval")
	@Operation(
			summary = "Get copy pending approval",
			description = "Retrieves all spot copy versions pending approval"
	)
	@ApiResponse(responseCode = "200", description = "Copy retrieved successfully")
	public ResponseEntity<List<SpotCopyResponseDTO>> getPendingApproval() {
		logger.debug("GET /api/copy/pending-approval - Fetching pending approvals");
		List<SpotCopyResponseDTO> response = spotCopyService.getPendingApproval();
		return ResponseEntity.ok(response);
	}

}
