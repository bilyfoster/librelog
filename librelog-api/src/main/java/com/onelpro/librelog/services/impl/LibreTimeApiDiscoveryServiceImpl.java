package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.ApiEndpointResponseDTO;
import com.onelpro.librelog.enums.EndpointStatus;
import com.onelpro.librelog.integrations.LibreTimeHttpClient;
import com.onelpro.librelog.services.LibreTimeIntegrationConfigService;
import com.onelpro.librelog.models.LibreTimeApiEndpoint;
import com.onelpro.librelog.repositories.LibreTimeApiEndpointRepository;
import com.onelpro.librelog.services.LibreTimeApiDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the LibreTime API endpoint discovery service.
 * Discovers available endpoints by testing common REST patterns.
 */
@Service
public class LibreTimeApiDiscoveryServiceImpl implements LibreTimeApiDiscoveryService {

	private static final Logger logger = LoggerFactory.getLogger(LibreTimeApiDiscoveryServiceImpl.class);

	// Common LibreTime API resource types to test
	private static final List<String> RESOURCE_TYPES = Arrays.asList(
			"files", "playlists", "smart-blocks", "shows", "show-instances",
			"streams", "webstreams", "podcasts", "podcast-episodes", "tracks",
			"library", "schedule", "status", "version", "users", "roles"
	);

	// Common HTTP methods to test
	private static final List<String> HTTP_METHODS = Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH");

	private final LibreTimeApiEndpointRepository endpointRepository;
	private final LibreTimeHttpClient httpClient;
	private final LibreTimeIntegrationConfigService configService;

	public LibreTimeApiDiscoveryServiceImpl(
			LibreTimeApiEndpointRepository endpointRepository,
			LibreTimeHttpClient httpClient,
			LibreTimeIntegrationConfigService configService) {
		this.endpointRepository = endpointRepository;
		this.httpClient = httpClient;
		this.configService = configService;
	}

	@Override
	@Transactional
	public List<ApiEndpointResponseDTO> discoverEndpoints(UUID stationId) {
		logger.info("Starting LibreTime API endpoint discovery for station: {}", stationId);

		// Ensure HTTP client is configured
		try {
			String jwtToken = configService.getDecryptedJwtToken(stationId);
			com.onelpro.librelog.dto.LibreTimeIntegrationConfigResponseDTO config = configService.getConfig(stationId);
			if (config == null) {
				logger.warn("LibreTime integration not configured. Cannot discover endpoints.");
				return List.of();
			}
			httpClient.setBaseUrl(config.getApiBaseUrl());
			httpClient.setJwtToken(jwtToken);
			httpClient.setTimeout(java.time.Duration.ofSeconds(10)); // Short timeout for discovery
			httpClient.setMaxRetries(1);
		} catch (Exception e) {
			logger.error("Failed to configure HTTP client for discovery: {}", e.getMessage());
			return List.of();
		}

		List<ApiEndpointResponseDTO> discoveredEndpoints = new ArrayList<>();

		// Test common REST patterns
		for (String resourceType : RESOURCE_TYPES) {
			// Test GET /api/v2/{resource}
			String getPath = "/api/v2/" + resourceType;
			LibreTimeApiEndpoint endpoint = testAndSaveEndpoint(getPath, "GET", resourceType);
			if (endpoint != null) {
				discoveredEndpoints.add(mapToDTO(endpoint));
			}

			// Test GET /api/v2/{resource}/{id} (with a test ID)
			String getByIdPath = "/api/v2/" + resourceType + "/1";
			endpoint = testAndSaveEndpoint(getByIdPath, "GET", resourceType);
			if (endpoint != null) {
				discoveredEndpoints.add(mapToDTO(endpoint));
			}

			// Test POST /api/v2/{resource}
			String postPath = "/api/v2/" + resourceType;
			endpoint = testAndSaveEndpoint(postPath, "POST", resourceType);
			if (endpoint != null) {
				discoveredEndpoints.add(mapToDTO(endpoint));
			}
		}

		// Test common utility endpoints
		String[] utilityEndpoints = { "/api/v2/status", "/api/v2/version", "/api/v2/health" };
		for (String path : utilityEndpoints) {
			LibreTimeApiEndpoint endpoint = testAndSaveEndpoint(path, "GET", null);
			if (endpoint != null) {
				discoveredEndpoints.add(mapToDTO(endpoint));
			}
		}

		logger.info("Endpoint discovery completed. Found {} endpoints", discoveredEndpoints.size());
		return discoveredEndpoints;
	}

	@Override
	public List<ApiEndpointResponseDTO> getDiscoveredEndpoints() {
		logger.debug("Fetching all discovered endpoints");
		return endpointRepository.findAll().stream()
				.map(this::mapToDTO)
				.toList();
	}

	@Override
	@Transactional
	public ApiEndpointResponseDTO updateEndpointStatus(UUID endpointId, EndpointStatus status) {
		logger.info("Updating endpoint {} status to {}", endpointId, status);
		LibreTimeApiEndpoint endpoint = endpointRepository.findById(endpointId)
				.orElseThrow(() -> new com.onelpro.librelog.exceptions.NotFoundException("Endpoint not found: " + endpointId));

		endpoint.setStatus(status);
		endpoint.setUpdatedAt(LocalDateTime.now());
		LibreTimeApiEndpoint updated = endpointRepository.save(endpoint);
		return mapToDTO(updated);
	}

	/**
	 * Tests an endpoint and saves it to the database if it exists.
	 * 
	 * @param path The endpoint path
	 * @param method The HTTP method
	 * @param resourceType The resource type (optional)
	 * @return The endpoint entity if discovered, null otherwise
	 */
	private LibreTimeApiEndpoint testAndSaveEndpoint(String path, String method, String resourceType) {
		try {
			long startTime = System.currentTimeMillis();
			boolean exists = false;
			EndpointStatus status = EndpointStatus.UNKNOWN;
			Integer responseTimeMs = null;
			Boolean requiresAuth = true; // Assume auth required by default

			// Try to make a request
			try {
				Mono<String> response = httpClient.get(path);
				String result = response.block(java.time.Duration.ofSeconds(5));
				if (result != null) {
					exists = true;
					status = EndpointStatus.WORKING;
					responseTimeMs = (int) (System.currentTimeMillis() - startTime);
				}
			} catch (WebClientResponseException e) {
				// 401/403 means endpoint exists but needs auth
				if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
					exists = true;
					status = EndpointStatus.WORKING; // Endpoint exists, just needs auth
					responseTimeMs = (int) (System.currentTimeMillis() - startTime);
					requiresAuth = true;
				}
				// 404 means endpoint doesn't exist
				else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
					exists = false;
					status = EndpointStatus.MISSING;
				}
				// Other errors might indicate endpoint exists but has issues
				else if (e.getStatusCode().is4xxClientError()) {
					exists = true;
					status = EndpointStatus.BROKEN;
					responseTimeMs = (int) (System.currentTimeMillis() - startTime);
				} else if (e.getStatusCode().is5xxServerError()) {
					exists = true;
					status = EndpointStatus.BROKEN;
					responseTimeMs = (int) (System.currentTimeMillis() - startTime);
				}
			} catch (Exception e) {
				// Connection errors, timeouts, etc.
				logger.debug("Error testing endpoint {} {}: {}", method, path, e.getMessage());
				exists = false;
				status = EndpointStatus.UNKNOWN;
			}

			// Only save if endpoint appears to exist
			if (exists) {
				Optional<LibreTimeApiEndpoint> existing = endpointRepository.findByEndpointPathAndHttpMethod(path, method);
				if (existing.isPresent()) {
					LibreTimeApiEndpoint endpoint = existing.get();
					endpoint.setStatus(status);
					endpoint.setLastTestedAt(LocalDateTime.now());
					endpoint.setResponseTimeMs(responseTimeMs);
					endpoint.setRequiresAuthentication(requiresAuth);
					endpoint.setUpdatedAt(LocalDateTime.now());
					return endpointRepository.save(endpoint);
				} else {
					LibreTimeApiEndpoint endpoint = LibreTimeApiEndpoint.builder()
							.endpointPath(path)
							.httpMethod(method)
							.resourceType(resourceType)
							.status(status)
							.lastTestedAt(LocalDateTime.now())
							.responseTimeMs(responseTimeMs)
							.requiresAuthentication(requiresAuth)
							.createdAt(LocalDateTime.now())
							.build();
					return endpointRepository.save(endpoint);
				}
			}

			return null;
		} catch (Exception e) {
			logger.error("Error testing endpoint {} {}: {}", method, path, e.getMessage());
			return null;
		}
	}

	private ApiEndpointResponseDTO mapToDTO(LibreTimeApiEndpoint endpoint) {
		return ApiEndpointResponseDTO.builder()
				.id(endpoint.getId())
				.endpointPath(endpoint.getEndpointPath())
				.httpMethod(endpoint.getHttpMethod())
				.resourceType(endpoint.getResourceType())
				.status(endpoint.getStatus())
				.lastTestedAt(endpoint.getLastTestedAt())
				.responseTimeMs(endpoint.getResponseTimeMs())
				.requiresAuthentication(endpoint.getRequiresAuthentication())
				.description(endpoint.getDescription())
				.documentationUrl(endpoint.getDocumentationUrl())
				.createdAt(endpoint.getCreatedAt())
				.updatedAt(endpoint.getUpdatedAt())
				.build();
	}

}

