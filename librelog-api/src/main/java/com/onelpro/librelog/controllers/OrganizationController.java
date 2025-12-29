package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.OrganizationRequestDTO;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.Organization;
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
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for organization management endpoints.
 */
@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organizations", description = "Organization management endpoints")
public class OrganizationController {

	private static final Logger logger = LoggerFactory.getLogger(OrganizationController.class);

	private final OrganizationRepository organizationRepository;

	public OrganizationController(OrganizationRepository organizationRepository) {
		this.organizationRepository = organizationRepository;
	}

	@PostMapping
	@Operation(summary = "Create a new organization", description = "Creates a new organization")
	@ApiResponse(responseCode = "201", description = "Organization created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data or name already exists")
	public ResponseEntity<Organization> create(@Valid @RequestBody OrganizationRequestDTO request) {
		logger.info("POST /api/organizations - Creating organization: {}", request.getName());
		
		if (organizationRepository.existsByName(request.getName())) {
			throw new BadRequestException("Organization with name " + request.getName() + " already exists");
		}

		Organization organization = Organization.builder()
				.name(request.getName())
				.legalName(request.getLegalName())
				.taxId(request.getTaxId())
				.addressLine1(request.getAddressLine1())
				.addressLine2(request.getAddressLine2())
				.city(request.getCity())
				.state(request.getState())
				.zipCode(request.getZipCode())
				.country(request.getCountry())
				.phone(request.getPhone())
				.email(request.getEmail())
				.website(request.getWebsite())
				.isActive(request.getIsActive())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		organization = organizationRepository.save(organization);
		return ResponseEntity.status(HttpStatus.CREATED).body(organization);
	}

	@GetMapping
	@Operation(summary = "Get all organizations", description = "Retrieves all organizations")
	public ResponseEntity<List<Organization>> getAll() {
		logger.debug("GET /api/organizations - Fetching all organizations");
		List<Organization> organizations = organizationRepository.findAll();
		return ResponseEntity.ok(organizations);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get organization by ID", description = "Retrieves an organization by its UUID")
	@ApiResponse(responseCode = "200", description = "Organization found")
	@ApiResponse(responseCode = "404", description = "Organization not found")
	public ResponseEntity<Organization> getById(@PathVariable UUID id) {
		logger.debug("GET /api/organizations/{} - Fetching organization", id);
		Organization organization = organizationRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Organization not found with ID: " + id));
		return ResponseEntity.ok(organization);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update organization", description = "Updates an existing organization")
	@ApiResponse(responseCode = "200", description = "Organization updated successfully")
	@ApiResponse(responseCode = "404", description = "Organization not found")
	@ApiResponse(responseCode = "400", description = "Invalid request data")
	@Transactional
	public ResponseEntity<Organization> update(
			@PathVariable UUID id,
			@Valid @RequestBody OrganizationRequestDTO request) {
		logger.info("PUT /api/organizations/{} - Updating organization", id);
		
		Organization organization = organizationRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Organization not found with ID: " + id));

		if (!organization.getName().equals(request.getName()) &&
				organizationRepository.existsByName(request.getName())) {
			throw new BadRequestException("Organization with name " + request.getName() + " already exists");
		}

		organization.setName(request.getName());
		organization.setLegalName(request.getLegalName());
		organization.setTaxId(request.getTaxId());
		organization.setAddressLine1(request.getAddressLine1());
		organization.setAddressLine2(request.getAddressLine2());
		organization.setCity(request.getCity());
		organization.setState(request.getState());
		organization.setZipCode(request.getZipCode());
		organization.setCountry(request.getCountry());
		organization.setPhone(request.getPhone());
		organization.setEmail(request.getEmail());
		organization.setWebsite(request.getWebsite());
		organization.setIsActive(request.getIsActive());
		organization.setUpdatedAt(LocalDateTime.now());

		organization = organizationRepository.save(organization);
		return ResponseEntity.ok(organization);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete organization", description = "Deletes an organization by its UUID")
	@ApiResponse(responseCode = "204", description = "Organization deleted successfully")
	@ApiResponse(responseCode = "404", description = "Organization not found")
	@Transactional
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		logger.info("DELETE /api/organizations/{} - Deleting organization", id);
		if (!organizationRepository.existsById(id)) {
			throw new NotFoundException("Organization not found with ID: " + id);
		}
		organizationRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}

