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
	 * Retrieves the current integration configuration.
	 * 
	 * @return The integration configuration, or null if not configured
	 */
	LibreTimeIntegrationConfigResponseDTO getConfig();

	/**
	 * Saves a new integration configuration.
	 * 
	 * @param request The configuration request DTO
	 * @param userId The ID of the user creating the configuration
	 * @return The saved configuration response DTO
	 */
	LibreTimeIntegrationConfigResponseDTO saveConfig(LibreTimeIntegrationConfigRequestDTO request, UUID userId);

	/**
	 * Updates an existing integration configuration.
	 * 
	 * @param request The configuration request DTO
	 * @param userId The ID of the user updating the configuration
	 * @return The updated configuration response DTO
	 */
	LibreTimeIntegrationConfigResponseDTO updateConfig(LibreTimeIntegrationConfigRequestDTO request, UUID userId);

	/**
	 * Tests the connection to LibreTime API using the current configuration.
	 * 
	 * @return Connection test response with success status and details
	 */
	ConnectionTestResponseDTO testConnection();

	/**
	 * Gets the decrypted JWT token for internal service use.
	 * This method should only be used by other services that need to make API calls.
	 * 
	 * @return Decrypted JWT token, or null if not configured
	 */
	String getDecryptedJwtToken();

	/**
	 * Gets the API base URL for internal service use.
	 * 
	 * @return API base URL, or null if not configured
	 */
	String getApiBaseUrl();

}

