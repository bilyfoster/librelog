package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.ConnectionTestResponseDTO;
import com.onelpro.librelog.dto.LibreTimeIntegrationConfigRequestDTO;
import com.onelpro.librelog.dto.LibreTimeIntegrationConfigResponseDTO;

import java.util.UUID;

/**
 * Service interface for LibreTime integration configuration management.
 */
public interface LibreTimeIntegrationConfigService {

	/**
	 * Retrieves the integration configuration for a specific station.
	 * 
	 * @param stationId The station ID
	 * @return The integration configuration, or null if not configured
	 */
	LibreTimeIntegrationConfigResponseDTO getConfig(UUID stationId);

	/**
	 * Saves a new integration configuration for a station.
	 * 
	 * @param stationId The station ID
	 * @param request The configuration request DTO
	 * @param userId The ID of the user creating the configuration
	 * @return The saved configuration response DTO
	 */
	LibreTimeIntegrationConfigResponseDTO saveConfig(UUID stationId, LibreTimeIntegrationConfigRequestDTO request, UUID userId);

	/**
	 * Updates an existing integration configuration for a station.
	 * 
	 * @param stationId The station ID
	 * @param request The configuration request DTO
	 * @param userId The ID of the user updating the configuration
	 * @return The updated configuration response DTO
	 */
	LibreTimeIntegrationConfigResponseDTO updateConfig(UUID stationId, LibreTimeIntegrationConfigRequestDTO request, UUID userId);

	/**
	 * Tests the connection to LibreTime API using the configuration for a station.
	 * 
	 * @param stationId The station ID
	 * @return Connection test response with success status and details
	 */
	ConnectionTestResponseDTO testConnection(UUID stationId);

	/**
	 * Gets the decrypted JWT token for internal service use.
	 * This method should only be used by other services that need to make API calls.
	 * 
	 * @param stationId The station ID
	 * @return Decrypted JWT token, or null if not configured
	 */
	String getDecryptedJwtToken(UUID stationId);

	/**
	 * Gets the API base URL for internal service use.
	 * 
	 * @param stationId The station ID
	 * @return API base URL, or null if not configured
	 */
	String getApiBaseUrl(UUID stationId);

}

