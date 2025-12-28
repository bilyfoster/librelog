package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.FixedAssetRequestDTO;
import com.onelpro.librelog.dto.FixedAssetResponseDTO;
import com.onelpro.librelog.services.FixedAssetService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for fixed asset management endpoints.
 */
@RestController
@RequestMapping("/api/fixed-assets")
@Tag(name = "Fixed Assets", description = "Fixed asset management endpoints")
public class FixedAssetController {

	private static final Logger logger = LoggerFactory.getLogger(FixedAssetController.class);

	private final FixedAssetService fixedAssetService;

	public FixedAssetController(FixedAssetService fixedAssetService) {
		this.fixedAssetService = fixedAssetService;
	}

	@PostMapping
	@Operation(
			summary = "Create a new fixed asset",
			description = "Creates a new fixed asset for a clock template"
	)
	@ApiResponse(responseCode = "201", description = "Fixed asset created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Clock template not found")
	public ResponseEntity<FixedAssetResponseDTO> create(@Valid @RequestBody FixedAssetRequestDTO request) {
		logger.info("POST /api/fixed-assets - Creating fixed asset: {}", request.getName());
		FixedAssetResponseDTO response = fixedAssetService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Get fixed asset by ID",
			description = "Retrieves a fixed asset by its UUID"
	)
	@ApiResponse(responseCode = "200", description = "Fixed asset found")
	@ApiResponse(responseCode = "404", description = "Fixed asset not found")
	public ResponseEntity<FixedAssetResponseDTO> getById(@PathVariable UUID id) {
		logger.debug("GET /api/fixed-assets/{} - Fetching fixed asset", id);
		FixedAssetResponseDTO response = fixedAssetService.getById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/clock-templates/{clockId}")
	@Operation(
			summary = "Get all fixed assets for a clock template",
			description = "Retrieves all fixed assets associated with a specific clock template"
	)
	@ApiResponse(responseCode = "200", description = "Fixed assets retrieved successfully")
	@ApiResponse(responseCode = "404", description = "Clock template not found")
	public ResponseEntity<List<FixedAssetResponseDTO>> getByClockTemplateId(@PathVariable UUID clockId) {
		logger.debug("GET /api/fixed-assets/clock-templates/{} - Fetching fixed assets", clockId);
		List<FixedAssetResponseDTO> response = fixedAssetService.getByClockTemplateId(clockId);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@Operation(
			summary = "Update fixed asset",
			description = "Updates an existing fixed asset"
	)
	@ApiResponse(responseCode = "200", description = "Fixed asset updated successfully")
	@ApiResponse(responseCode = "404", description = "Fixed asset not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	public ResponseEntity<FixedAssetResponseDTO> update(
			@PathVariable UUID id,
			@Valid @RequestBody FixedAssetRequestDTO request) {
		logger.info("PUT /api/fixed-assets/{} - Updating fixed asset", id);
		FixedAssetResponseDTO response = fixedAssetService.update(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(
			summary = "Delete fixed asset",
			description = "Deletes a fixed asset by its UUID"
	)
	@ApiResponse(responseCode = "204", description = "Fixed asset deleted successfully")
	@ApiResponse(responseCode = "404", description = "Fixed asset not found")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/fixed-assets/{} - Deleting fixed asset", id);
		fixedAssetService.delete(id);
		return ResponseEntity.noContent().build();
	}

}

