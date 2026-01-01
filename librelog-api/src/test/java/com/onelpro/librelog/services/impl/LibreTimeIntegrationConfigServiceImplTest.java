package com.onelpro.librelog.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelpro.librelog.dto.ConnectionTestResponseDTO;
import com.onelpro.librelog.dto.LibreTimeIntegrationConfigRequestDTO;
import com.onelpro.librelog.dto.LibreTimeIntegrationConfigResponseDTO;
import com.onelpro.librelog.enums.ConflictResolution;
import com.onelpro.librelog.enums.SyncDirection;
import com.onelpro.librelog.enums.SyncFrequency;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.integrations.LibreTimeHttpClient;
import com.onelpro.librelog.models.LibreTimeIntegrationConfig;
import com.onelpro.librelog.models.User;
import com.onelpro.librelog.repositories.LibreTimeIntegrationConfigRepository;
import com.onelpro.librelog.repositories.UserRepository;
import com.onelpro.librelog.utils.EncryptionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibreTimeIntegrationConfigServiceImplTest {

	@Mock
	private LibreTimeIntegrationConfigRepository configRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private LibreTimeHttpClient httpClient;

	@Mock
	private ObjectProvider<ObjectMapper> objectMapperProvider;

	@InjectMocks
	private LibreTimeIntegrationConfigServiceImpl configService;

	private LibreTimeIntegrationConfigRequestDTO requestDTO;
	private LibreTimeIntegrationConfig configEntity;
	private User testUser;
	private UUID userId;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();
		testUser = User.builder()
				.id(userId)
				.email("test@example.com")
				.build();

		requestDTO = LibreTimeIntegrationConfigRequestDTO.builder()
				.apiBaseUrl("https://studio-qa.gayphx.com/api/v2/")
				.jwtToken("test-jwt-token")
				.syncEnabled(true)
				.syncFrequency(SyncFrequency.HOURLY)
				.syncDirection(SyncDirection.BIDIRECTIONAL)
				.conflictResolution(ConflictResolution.LAST_WRITE_WINS)
				.webhookUrl("https://example.com/webhook")
				.webhookSecret("test-webhook-secret")
				.webhookEnabled(true)
				.maxFileSizeMb(500)
				.supportedFormats(Arrays.asList("MP3", "WAV", "FLAC"))
				.build();

		configEntity = LibreTimeIntegrationConfig.builder()
				.id(UUID.randomUUID())
				.apiBaseUrl(requestDTO.getApiBaseUrl())
				.jwtToken(EncryptionUtils.encrypt(requestDTO.getJwtToken()))
				.syncEnabled(requestDTO.getSyncEnabled())
				.syncFrequency(requestDTO.getSyncFrequency())
				.syncDirection(requestDTO.getSyncDirection())
				.conflictResolution(requestDTO.getConflictResolution())
				.webhookUrl(requestDTO.getWebhookUrl())
				.webhookSecret(EncryptionUtils.encrypt(requestDTO.getWebhookSecret()))
				.webhookEnabled(requestDTO.getWebhookEnabled())
				.maxFileSizeMb(requestDTO.getMaxFileSizeMb())
				.supportedFormats("[\"MP3\",\"WAV\",\"FLAC\"]")
				.createdAt(LocalDateTime.now())
				.createdBy(testUser)
				.build();

		objectMapper = new ObjectMapper();
		// Mock ObjectProvider.getIfAvailable(Supplier) - this is what the service calls
		when(objectMapperProvider.getIfAvailable(any(java.util.function.Supplier.class))).thenReturn(objectMapper);
		
		// Create the service manually to ensure ObjectMapper is available
		configService = new LibreTimeIntegrationConfigServiceImpl(
				configRepository,
				userRepository,
				httpClient,
				objectMapperProvider
		);
	}

	@Test
	void getConfig_When_ConfigExists_Expect_ReturnsConfig() {
		when(configRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configEntity));

		LibreTimeIntegrationConfigResponseDTO result = configService.getConfig();

		assertNotNull(result);
		assertEquals(configEntity.getApiBaseUrl(), result.getApiBaseUrl());
		assertEquals(configEntity.getSyncEnabled(), result.getSyncEnabled());
		assertEquals(configEntity.getSyncFrequency(), result.getSyncFrequency());
		verify(configRepository).findFirstByOrderByCreatedAtAsc();
	}

	@Test
	void getConfig_When_ConfigDoesNotExist_Expect_ReturnsNull() {
		when(configRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());

		LibreTimeIntegrationConfigResponseDTO result = configService.getConfig();

		assertNull(result);
		verify(configRepository).findFirstByOrderByCreatedAtAsc();
	}

	@Test
	void saveConfig_When_ValidRequest_Expect_SavesConfig() {
		when(configRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
		when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
		when(httpClient.validateBaseUrl(anyString())).thenReturn(true);
		when(configRepository.save(any(LibreTimeIntegrationConfig.class))).thenReturn(configEntity);

		LibreTimeIntegrationConfigResponseDTO result = configService.saveConfig(requestDTO, userId);

		assertNotNull(result);
		verify(configRepository).save(any(LibreTimeIntegrationConfig.class));
		verify(configRepository).findFirstByOrderByCreatedAtAsc();
		verify(httpClient).validateBaseUrl(requestDTO.getApiBaseUrl());
	}

	@Test
	void saveConfig_When_ConfigAlreadyExists_Expect_ThrowsBadRequestException() {
		when(configRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configEntity));

		assertThrows(BadRequestException.class, () -> configService.saveConfig(requestDTO, userId));
		verify(configRepository, never()).save(any());
	}

	@Test
	void saveConfig_When_InvalidUrl_Expect_ThrowsBadRequestException() {
		requestDTO.setApiBaseUrl("invalid-url");
		when(configRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
		when(httpClient.validateBaseUrl(anyString())).thenReturn(false);

		assertThrows(BadRequestException.class, () -> configService.saveConfig(requestDTO, userId));
		verify(configRepository, never()).save(any());
	}

	@Test
	void updateConfig_When_ConfigExists_Expect_UpdatesConfig() {
		when(configRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configEntity));
		when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
		when(httpClient.validateBaseUrl(anyString())).thenReturn(true);
		when(configRepository.save(any(LibreTimeIntegrationConfig.class))).thenReturn(configEntity);

		LibreTimeIntegrationConfigResponseDTO result = configService.updateConfig(requestDTO, userId);

		assertNotNull(result);
		verify(configRepository).save(any(LibreTimeIntegrationConfig.class));
		verify(httpClient).validateBaseUrl(requestDTO.getApiBaseUrl());
	}

	@Test
	void updateConfig_When_ConfigDoesNotExist_Expect_ThrowsNotFoundException() {
		when(configRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> configService.updateConfig(requestDTO, userId));
		verify(configRepository, never()).save(any());
	}

	@Test
	void testConnection_When_ConnectionSuccessful_Expect_ReturnsSuccess() {
		when(configRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configEntity));
		when(httpClient.validateBaseUrl(anyString())).thenReturn(true);
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		when(httpClient.testConnection()).thenReturn(Mono.just(true));

		ConnectionTestResponseDTO result = configService.testConnection();

		assertNotNull(result);
		assertTrue(result.getSuccess());
		verify(httpClient).setBaseUrl(configEntity.getApiBaseUrl());
		verify(httpClient).setJwtToken(anyString());
		verify(httpClient).validateBaseUrl(configEntity.getApiBaseUrl());
		verify(httpClient).testConnection();
	}

	@Test
	void testConnection_When_ConnectionFails_Expect_ReturnsFailure() {
		when(configRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configEntity));
		when(httpClient.validateBaseUrl(anyString())).thenReturn(true);
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		when(httpClient.testConnection()).thenReturn(Mono.just(false));

		ConnectionTestResponseDTO result = configService.testConnection();

		assertNotNull(result);
		assertFalse(result.getSuccess());
		verify(httpClient).setBaseUrl(configEntity.getApiBaseUrl());
		verify(httpClient).setJwtToken(anyString());
		verify(httpClient).validateBaseUrl(configEntity.getApiBaseUrl());
		verify(httpClient).testConnection();
	}

	@Test
	void testConnection_When_ConfigDoesNotExist_Expect_ThrowsNotFoundException() {
		when(configRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> configService.testConnection());
		verify(httpClient, never()).testConnection();
	}

	@Test
	void getDecryptedJwtToken_When_ConfigExists_Expect_ReturnsDecryptedToken() {
		when(configRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configEntity));

		String result = configService.getDecryptedJwtToken();

		assertNotNull(result);
		assertEquals("test-jwt-token", result);
	}

	@Test
	void getDecryptedJwtToken_When_ConfigDoesNotExist_Expect_ReturnsNull() {
		when(configRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());

		// The service returns null when config doesn't exist (doesn't throw exception)
		String result = configService.getDecryptedJwtToken();
		assertNull(result);
	}

	@Test
	void saveConfig_When_InvalidFileSize_Expect_ThrowsBadRequestException() {
		requestDTO.setMaxFileSizeMb(6000); // Exceeds max
		when(configRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
		when(httpClient.validateBaseUrl(anyString())).thenReturn(true);

		assertThrows(BadRequestException.class, () -> configService.saveConfig(requestDTO, userId));
		verify(configRepository, never()).save(any());
	}

	@Test
	void saveConfig_When_MissingRequiredFields_Expect_ThrowsBadRequestException() {
		requestDTO.setSyncFrequency(null);
		when(configRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
		when(httpClient.validateBaseUrl(anyString())).thenReturn(true);

		assertThrows(BadRequestException.class, () -> configService.saveConfig(requestDTO, userId));
		verify(configRepository, never()).save(any());
	}

}

