package com.onelpro.librelog.services.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelpro.librelog.dto.ConnectionTestResponseDTO;
import com.onelpro.librelog.dto.LibreTimeIntegrationConfigRequestDTO;
import com.onelpro.librelog.dto.LibreTimeIntegrationConfigResponseDTO;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.integrations.LibreTimeHttpClient;
import com.onelpro.librelog.models.LibreTimeIntegrationConfig;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.repositories.LibreTimeIntegrationConfigRepository;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.services.LibreTimeIntegrationConfigService;
import com.onelpro.librelog.utils.EncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of LibreTime integration configuration service.
 * Handles CRUD operations, encryption, validation, and connection testing.
 */
@Service
public class LibreTimeIntegrationConfigServiceImpl implements LibreTimeIntegrationConfigService {

	private static final Logger logger = LoggerFactory.getLogger(LibreTimeIntegrationConfigServiceImpl.class);

	private final LibreTimeIntegrationConfigRepository configRepository;
	private final UserRepository userRepository;
	private final LibreTimeHttpClient httpClient;
	private final ObjectMapper objectMapper;

	public LibreTimeIntegrationConfigServiceImpl(
			LibreTimeIntegrationConfigRepository configRepository,
			UserRepository userRepository,
			LibreTimeHttpClient httpClient,
			ObjectMapper objectMapper) {
		this.configRepository = configRepository;
		this.userRepository = userRepository;
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public LibreTimeIntegrationConfigResponseDTO getConfig() {
		logger.debug("Fetching LibreTime integration configuration");
		LibreTimeIntegrationConfig config = configRepository.findFirstByOrderByCreatedAtAsc()
				.orElse(null);

		if (config == null) {
			return null;
		}

		return mapToResponseDTO(config);
	}

	@Override
	@Transactional
	public LibreTimeIntegrationConfigResponseDTO saveConfig(LibreTimeIntegrationConfigRequestDTO request, UUID userId) {
		logger.info("Saving new LibreTime integration configuration by user: {}", userId);

		// Check if config already exists
		if (configRepository.findFirstByOrderByCreatedAtAsc().isPresent()) {
			throw new BadRequestException("Integration configuration already exists. Use update instead.");
		}

		// Validate request
		validateConfigRequest(request);

		// Get user
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

		// Create entity
		LibreTimeIntegrationConfig config = mapToEntity(request, user, null);
		config.setCreatedAt(LocalDateTime.now());
		config.setUpdatedAt(LocalDateTime.now());

		// Encrypt sensitive fields
		config.setJwtToken(EncryptionUtils.encrypt(request.getJwtToken()));
		if (request.getWebhookSecret() != null && !request.getWebhookSecret().isEmpty()) {
			config.setWebhookSecret(EncryptionUtils.encrypt(request.getWebhookSecret()));
		}

		// Convert supported formats list to JSON string
		if (request.getSupportedFormats() != null) {
			try {
				config.setSupportedFormats(objectMapper.writeValueAsString(request.getSupportedFormats()));
			} catch (Exception e) {
				logger.error("Failed to serialize supported formats: {}", e.getMessage());
				throw new BadRequestException("Invalid supported formats format");
			}
		}

		LibreTimeIntegrationConfig savedConfig = configRepository.save(config);
		logger.info("LibreTime integration configuration saved successfully with ID: {}", savedConfig.getId());

		return mapToResponseDTO(savedConfig);
	}

	@Override
	@Transactional
	public LibreTimeIntegrationConfigResponseDTO updateConfig(LibreTimeIntegrationConfigRequestDTO request, UUID userId) {
		logger.info("Updating LibreTime integration configuration by user: {}", userId);

		// Get existing config
		LibreTimeIntegrationConfig config = configRepository.findFirstByOrderByCreatedAtAsc()
				.orElseThrow(() -> new NotFoundException("Integration configuration not found. Create it first."));

		// Validate request
		validateConfigRequest(request);

		// Get user
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

		// Update entity
		config.setApiBaseUrl(request.getApiBaseUrl());
		config.setSyncEnabled(request.getSyncEnabled());
		config.setSyncFrequency(request.getSyncFrequency());
		config.setSyncDirection(request.getSyncDirection());
		config.setConflictResolution(request.getConflictResolution());
		config.setWebhookUrl(request.getWebhookUrl());
		config.setWebhookEnabled(request.getWebhookEnabled());
		config.setMaxFileSizeMb(request.getMaxFileSizeMb());
		config.setUpdatedAt(LocalDateTime.now());
		config.setUpdatedBy(user);

		// Encrypt sensitive fields if provided
		if (request.getJwtToken() != null && !request.getJwtToken().isEmpty()) {
			config.setJwtToken(EncryptionUtils.encrypt(request.getJwtToken()));
		}
		if (request.getWebhookSecret() != null && !request.getWebhookSecret().isEmpty()) {
			config.setWebhookSecret(EncryptionUtils.encrypt(request.getWebhookSecret()));
		}

		// Convert supported formats list to JSON string
		if (request.getSupportedFormats() != null) {
			try {
				config.setSupportedFormats(objectMapper.writeValueAsString(request.getSupportedFormats()));
			} catch (Exception e) {
				logger.error("Failed to serialize supported formats: {}", e.getMessage());
				throw new BadRequestException("Invalid supported formats format");
			}
		}

		LibreTimeIntegrationConfig updatedConfig = configRepository.save(config);
		logger.info("LibreTime integration configuration updated successfully with ID: {}", updatedConfig.getId());

		return mapToResponseDTO(updatedConfig);
	}

	@Override
	public ConnectionTestResponseDTO testConnection() {
		logger.info("Testing LibreTime API connection");

		LibreTimeIntegrationConfig config = configRepository.findFirstByOrderByCreatedAtAsc()
				.orElseThrow(() -> new NotFoundException("Integration configuration not found. Please configure it first."));

		long startTime = System.currentTimeMillis();

		try {
			// Configure HTTP client with current config
			httpClient.setBaseUrl(config.getApiBaseUrl());
			String decryptedToken = EncryptionUtils.decrypt(config.getJwtToken());
			httpClient.setJwtToken(decryptedToken);

			// Validate URL format
			if (!httpClient.validateBaseUrl(config.getApiBaseUrl())) {
				return ConnectionTestResponseDTO.builder()
						.success(false)
						.message("Invalid API base URL format")
						.testedAt(LocalDateTime.now())
						.errorDetails("The provided URL is not a valid HTTP/HTTPS URL")
						.build();
			}

			// Test connection
			Boolean result = httpClient.testConnection().block();
			long responseTime = System.currentTimeMillis() - startTime;

			if (Boolean.TRUE.equals(result)) {
				logger.info("LibreTime connection test successful in {}ms", responseTime);
				return ConnectionTestResponseDTO.builder()
						.success(true)
						.message("Connection successful")
						.responseTimeMs((int) responseTime)
						.testedAt(LocalDateTime.now())
						.build();
			} else {
				logger.warn("LibreTime connection test failed");
				return ConnectionTestResponseDTO.builder()
						.success(false)
						.message("Connection failed")
						.responseTimeMs((int) responseTime)
						.testedAt(LocalDateTime.now())
						.errorDetails("Unable to connect to LibreTime API. Please check URL and authentication token.")
						.build();
			}
		} catch (Exception e) {
			long responseTime = System.currentTimeMillis() - startTime;
			logger.error("LibreTime connection test error: {}", e.getMessage());
			return ConnectionTestResponseDTO.builder()
					.success(false)
					.message("Connection test error")
					.responseTimeMs((int) responseTime)
					.testedAt(LocalDateTime.now())
					.errorDetails(e.getMessage())
					.build();
		}
	}

	/**
	 * Validates the configuration request.
	 * 
	 * @param request The configuration request to validate
	 */
	private void validateConfigRequest(LibreTimeIntegrationConfigRequestDTO request) {
		// Validate URL format
		if (request.getApiBaseUrl() != null && !request.getApiBaseUrl().isEmpty()) {
			if (!httpClient.validateBaseUrl(request.getApiBaseUrl())) {
				throw new BadRequestException("Invalid API base URL format. Must be a valid HTTP or HTTPS URL.");
			}
		}

		// Validate enum values are not null
		if (request.getSyncFrequency() == null) {
			throw new BadRequestException("Sync frequency is required");
		}
		if (request.getSyncDirection() == null) {
			throw new BadRequestException("Sync direction is required");
		}
		if (request.getConflictResolution() == null) {
			throw new BadRequestException("Conflict resolution strategy is required");
		}

		// Validate file size limits
		if (request.getMaxFileSizeMb() != null) {
			if (request.getMaxFileSizeMb() < 1 || request.getMaxFileSizeMb() > 5000) {
				throw new BadRequestException("Max file size must be between 1 and 5000 MB");
			}
		}
	}

	/**
	 * Maps request DTO to entity.
	 * 
	 * @param request The request DTO
	 * @param user The user creating/updating
	 * @param existingConfig Existing config if updating, null if creating
	 * @return Entity
	 */
	private LibreTimeIntegrationConfig mapToEntity(
			LibreTimeIntegrationConfigRequestDTO request,
			User user,
			LibreTimeIntegrationConfig existingConfig) {
		LibreTimeIntegrationConfig.LibreTimeIntegrationConfigBuilder builder = existingConfig != null
				? existingConfig.toBuilder()
				: LibreTimeIntegrationConfig.builder();

		return builder
				.apiBaseUrl(request.getApiBaseUrl())
				.syncEnabled(request.getSyncEnabled() != null ? request.getSyncEnabled() : false)
				.syncFrequency(request.getSyncFrequency())
				.syncDirection(request.getSyncDirection())
				.conflictResolution(request.getConflictResolution())
				.webhookUrl(request.getWebhookUrl())
				.webhookEnabled(request.getWebhookEnabled() != null ? request.getWebhookEnabled() : false)
				.maxFileSizeMb(request.getMaxFileSizeMb() != null ? request.getMaxFileSizeMb() : 500)
				.createdBy(user)
				.updatedBy(user)
				.build();
	}

	/**
	 * Maps entity to response DTO.
	 * 
	 * @param config The entity
	 * @return Response DTO with masked sensitive data
	 */
	private LibreTimeIntegrationConfigResponseDTO mapToResponseDTO(LibreTimeIntegrationConfig config) {
		// Decrypt and mask sensitive fields
		String maskedJwtToken = null;
		if (config.getJwtToken() != null) {
			try {
				String decrypted = EncryptionUtils.decrypt(config.getJwtToken());
				maskedJwtToken = EncryptionUtils.maskForDisplay(decrypted);
			} catch (Exception e) {
				logger.warn("Failed to decrypt JWT token for display: {}", e.getMessage());
				maskedJwtToken = "****";
			}
		}

		String maskedWebhookSecret = null;
		if (config.getWebhookSecret() != null) {
			try {
				String decrypted = EncryptionUtils.decrypt(config.getWebhookSecret());
				maskedWebhookSecret = EncryptionUtils.maskForDisplay(decrypted);
			} catch (Exception e) {
				logger.warn("Failed to decrypt webhook secret for display: {}", e.getMessage());
				maskedWebhookSecret = "****";
			}
		}

		// Parse supported formats from JSON string
		List<String> supportedFormats = null;
		if (config.getSupportedFormats() != null && !config.getSupportedFormats().isEmpty()) {
			try {
				supportedFormats = objectMapper.readValue(config.getSupportedFormats(), new TypeReference<List<String>>() {});
			} catch (Exception e) {
				logger.warn("Failed to parse supported formats: {}", e.getMessage());
			}
		}

		return LibreTimeIntegrationConfigResponseDTO.builder()
				.id(config.getId())
				.apiBaseUrl(config.getApiBaseUrl())
				.jwtToken(maskedJwtToken)
				.syncEnabled(config.getSyncEnabled())
				.syncFrequency(config.getSyncFrequency())
				.syncDirection(config.getSyncDirection())
				.conflictResolution(config.getConflictResolution())
				.webhookUrl(config.getWebhookUrl())
				.webhookSecret(maskedWebhookSecret)
				.webhookEnabled(config.getWebhookEnabled())
				.maxFileSizeMb(config.getMaxFileSizeMb())
				.supportedFormats(supportedFormats)
				.createdAt(config.getCreatedAt())
				.updatedAt(config.getUpdatedAt())
				.createdById(config.getCreatedBy() != null ? config.getCreatedBy().getId() : null)
				.updatedById(config.getUpdatedBy() != null ? config.getUpdatedBy().getId() : null)
				.build();
	}

}

