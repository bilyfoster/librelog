package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.ApiEndpointResponseDTO;

import java.util.List;

/**
 * Service interface for discovering LibreTime API endpoints.
 */
public interface LibreTimeApiDiscoveryService {

	/**
	 * Discovers available LibreTime API endpoints by testing common REST patterns.
	 * 
	 * @return List of discovered endpoints
	 */
	List<ApiEndpointResponseDTO> discoverEndpoints();

	/**
	 * Gets all discovered endpoints from the database.
	 * 
	 * @return List of discovered endpoints
	 */
	List<ApiEndpointResponseDTO> getDiscoveredEndpoints();

	/**
	 * Updates the status of an endpoint.
	 * 
	 * @param endpointId The endpoint ID
	 * @param status The new status
	 * @return Updated endpoint DTO
	 */
	ApiEndpointResponseDTO updateEndpointStatus(java.util.UUID endpointId, com.onelpro.librelog.enums.EndpointStatus status);

}

