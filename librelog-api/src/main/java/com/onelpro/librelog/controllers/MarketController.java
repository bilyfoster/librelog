package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.MarketRequestDTO;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Market;
import com.onelpro.librelog.models.Organization;
import com.onelpro.librelog.repositories.MarketRepository;
import com.onelpro.librelog.repositories.OrganizationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for market management endpoints.
 */
@RestController
@RequestMapping("/api/markets")
@Tag(name = "Markets", description = "Market management endpoints")
public class MarketController {

	private static final Logger logger = LoggerFactory.getLogger(MarketController.class);

	private final MarketRepository marketRepository;
	private final OrganizationRepository organizationRepository;

	public MarketController(MarketRepository marketRepository, OrganizationRepository organizationRepository) {
		this.marketRepository = marketRepository;
		this.organizationRepository = organizationRepository;
	}

	@PostMapping
	@Operation(summary = "Create a new market", description = "Creates a new market")
	@ApiResponse(responseCode = "201", description = "Market created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Organization not found")
	public ResponseEntity<Market> create(@Valid @RequestBody MarketRequestDTO request) {
		logger.info("POST /api/markets - Creating market: {}", request.getName());
		
		Organization organization = organizationRepository.findById(request.getOrganizationId())
				.orElseThrow(() -> new NotFoundException("Organization not found with ID: " + request.getOrganizationId()));

		Market market = Market.builder()
				.organization(organization)
				.name(request.getName())
				.description(request.getDescription())
				.city(request.getCity())
				.state(request.getState())
				.country(request.getCountry())
				.timezone(request.getTimezone())
				.isActive(request.getIsActive())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		market = marketRepository.save(market);
		return ResponseEntity.status(HttpStatus.CREATED).body(market);
	}

	@GetMapping
	@Operation(summary = "Get all markets", description = "Retrieves all markets, optionally filtered by organization")
	public ResponseEntity<List<Market>> getAll(@RequestParam(required = false) UUID organizationId) {
		logger.debug("GET /api/markets - Fetching markets");
		List<Market> markets;
		if (organizationId != null) {
			markets = marketRepository.findByOrganizationId(organizationId);
		} else {
			markets = marketRepository.findAll();
		}
		return ResponseEntity.ok(markets);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get market by ID", description = "Retrieves a market by its UUID")
	@ApiResponse(responseCode = "200", description = "Market found")
	@ApiResponse(responseCode = "404", description = "Market not found")
	public ResponseEntity<Market> getById(@PathVariable UUID id) {
		logger.debug("GET /api/markets/{} - Fetching market", id);
		Market market = marketRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Market not found with ID: " + id));
		return ResponseEntity.ok(market);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update market", description = "Updates an existing market")
	@ApiResponse(responseCode = "200", description = "Market updated successfully")
	@ApiResponse(responseCode = "404", description = "Market not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@Transactional
	public ResponseEntity<Market> update(
			@PathVariable UUID id,
			@Valid @RequestBody MarketRequestDTO request) {
		logger.info("PUT /api/markets/{} - Updating market", id);
		
		Market market = marketRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Market not found with ID: " + id));

		Organization organization = organizationRepository.findById(request.getOrganizationId())
				.orElseThrow(() -> new NotFoundException("Organization not found with ID: " + request.getOrganizationId()));

		market.setOrganization(organization);
		market.setName(request.getName());
		market.setDescription(request.getDescription());
		market.setCity(request.getCity());
		market.setState(request.getState());
		market.setCountry(request.getCountry());
		market.setTimezone(request.getTimezone());
		market.setIsActive(request.getIsActive());
		market.setUpdatedAt(LocalDateTime.now());

		market = marketRepository.save(market);
		return ResponseEntity.ok(market);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete market", description = "Deletes a market by its UUID")
	@ApiResponse(responseCode = "204", description = "Market deleted successfully")
	@ApiResponse(responseCode = "404", description = "Market not found")
	@Transactional
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/markets/{} - Deleting market", id);
		if (!marketRepository.existsById(id)) {
			throw new NotFoundException("Market not found with ID: " + id);
		}
		marketRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}

