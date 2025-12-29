package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.ClusterRequestDTO;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Cluster;
import com.onelpro.librelog.models.Market;
import com.onelpro.librelog.models.Organization;
import com.onelpro.librelog.repositories.ClusterRepository;
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
 * REST controller for cluster management endpoints.
 */
@RestController
@RequestMapping("/api/clusters")
@Tag(name = "Clusters", description = "Cluster management endpoints")
public class ClusterController {

	private static final Logger logger = LoggerFactory.getLogger(ClusterController.class);

	private final ClusterRepository clusterRepository;
	private final OrganizationRepository organizationRepository;
	private final MarketRepository marketRepository;

	public ClusterController(ClusterRepository clusterRepository, OrganizationRepository organizationRepository, MarketRepository marketRepository) {
		this.clusterRepository = clusterRepository;
		this.organizationRepository = organizationRepository;
		this.marketRepository = marketRepository;
	}

	@PostMapping
	@Operation(summary = "Create a new cluster", description = "Creates a new cluster")
	@ApiResponse(responseCode = "201", description = "Cluster created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@ApiResponse(responseCode = "404", description = "Organization or Market not found")
	public ResponseEntity<Cluster> create(@Valid @RequestBody ClusterRequestDTO request) {
		logger.info("POST /api/clusters - Creating cluster: {}", request.getName());
		
		Organization organization = organizationRepository.findById(request.getOrganizationId())
				.orElseThrow(() -> new NotFoundException("Organization not found with ID: " + request.getOrganizationId()));

		Market market = null;
		if (request.getMarketId() != null) {
			market = marketRepository.findById(request.getMarketId())
					.orElseThrow(() -> new NotFoundException("Market not found with ID: " + request.getMarketId()));
		}

		Cluster cluster = Cluster.builder()
				.organization(organization)
				.market(market)
				.name(request.getName())
				.description(request.getDescription())
				.isActive(request.getIsActive())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		cluster = clusterRepository.save(cluster);
		return ResponseEntity.status(HttpStatus.CREATED).body(cluster);
	}

	@GetMapping
	@Operation(summary = "Get all clusters", description = "Retrieves all clusters, optionally filtered by organization")
	public ResponseEntity<List<Cluster>> getAll(@RequestParam(required = false) UUID organizationId) {
		logger.debug("GET /api/clusters - Fetching clusters");
		List<Cluster> clusters;
		if (organizationId != null) {
			clusters = clusterRepository.findByOrganizationId(organizationId);
		} else {
			clusters = clusterRepository.findAll();
		}
		return ResponseEntity.ok(clusters);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get cluster by ID", description = "Retrieves a cluster by its UUID")
	@ApiResponse(responseCode = "200", description = "Cluster found")
	@ApiResponse(responseCode = "404", description = "Cluster not found")
	public ResponseEntity<Cluster> getById(@PathVariable UUID id) {
		logger.debug("GET /api/clusters/{} - Fetching cluster", id);
		Cluster cluster = clusterRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Cluster not found with ID: " + id));
		return ResponseEntity.ok(cluster);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update cluster", description = "Updates an existing cluster")
	@ApiResponse(responseCode = "200", description = "Cluster updated successfully")
	@ApiResponse(responseCode = "404", description = "Cluster not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@Transactional
	public ResponseEntity<Cluster> update(
			@PathVariable UUID id,
			@Valid @RequestBody ClusterRequestDTO request) {
		logger.info("PUT /api/clusters/{} - Updating cluster", id);
		
		Cluster cluster = clusterRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Cluster not found with ID: " + id));

		Organization organization = organizationRepository.findById(request.getOrganizationId())
				.orElseThrow(() -> new NotFoundException("Organization not found with ID: " + request.getOrganizationId()));

		Market market = null;
		if (request.getMarketId() != null) {
			market = marketRepository.findById(request.getMarketId())
					.orElseThrow(() -> new NotFoundException("Market not found with ID: " + request.getMarketId()));
		}

		cluster.setOrganization(organization);
		cluster.setMarket(market);
		cluster.setName(request.getName());
		cluster.setDescription(request.getDescription());
		cluster.setIsActive(request.getIsActive());
		cluster.setUpdatedAt(LocalDateTime.now());

		cluster = clusterRepository.save(cluster);
		return ResponseEntity.ok(cluster);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete cluster", description = "Deletes a cluster by its UUID")
	@ApiResponse(responseCode = "204", description = "Cluster deleted successfully")
	@ApiResponse(responseCode = "404", description = "Cluster not found")
	@Transactional
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/clusters/{} - Deleting cluster", id);
		if (!clusterRepository.existsById(id)) {
			throw new NotFoundException("Cluster not found with ID: " + id);
		}
		clusterRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}

