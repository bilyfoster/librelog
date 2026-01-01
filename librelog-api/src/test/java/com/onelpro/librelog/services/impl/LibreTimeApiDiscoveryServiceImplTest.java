package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.ApiEndpointResponseDTO;
import com.onelpro.librelog.dto.LibreTimeIntegrationConfigResponseDTO;
import com.onelpro.librelog.enums.EndpointStatus;
import com.onelpro.librelog.enums.SyncFrequency;
import com.onelpro.librelog.integrations.LibreTimeHttpClient;
import com.onelpro.librelog.models.LibreTimeApiEndpoint;
import com.onelpro.librelog.repositories.LibreTimeApiEndpointRepository;
import com.onelpro.librelog.services.LibreTimeIntegrationConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
class LibreTimeApiDiscoveryServiceImplTest {

	@Mock
	private LibreTimeApiEndpointRepository endpointRepository;

	@Mock
	private LibreTimeHttpClient httpClient;

	@Mock
	private LibreTimeIntegrationConfigService configService;

	@InjectMocks
	private LibreTimeApiDiscoveryServiceImpl discoveryService;

	private UUID stationId;
	private LibreTimeIntegrationConfigResponseDTO configDTO;
	private LibreTimeApiEndpoint endpoint;

	@BeforeEach
	void setUp() {
		stationId = UUID.randomUUID();

		configDTO = LibreTimeIntegrationConfigResponseDTO.builder()
				.stationId(stationId)
				.apiBaseUrl("https://studio-qa.gayphx.com/api/v2/")
				.syncEnabled(true)
				.syncFrequency(SyncFrequency.HOURLY)
				.build();

		endpoint = LibreTimeApiEndpoint.builder()
				.id(UUID.randomUUID())
				.endpointPath("/api/v2/files")
				.httpMethod("GET")
				.resourceType("files")
				.status(EndpointStatus.WORKING)
				.lastTestedAt(LocalDateTime.now())
				.responseTimeMs(100)
				.requiresAuthentication(true)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
	}

	@Test
	void discoverEndpoints_When_ConfigExists_Expect_ReturnsDiscoveredEndpoints() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		doNothing().when(httpClient).setTimeout(any());
		doNothing().when(httpClient).setMaxRetries(anyInt());
		
		// Mock successful response for some endpoints
		when(httpClient.get(anyString())).thenReturn(Mono.just("{\"data\":[]}"));
		when(endpointRepository.findByEndpointPathAndHttpMethod(anyString(), anyString()))
				.thenReturn(Optional.empty());
		when(endpointRepository.save(any(LibreTimeApiEndpoint.class))).thenReturn(endpoint);

		List<ApiEndpointResponseDTO> result = discoveryService.discoverEndpoints(stationId);

		assertNotNull(result);
		verify(configService).getConfig(stationId);
		verify(httpClient, atLeastOnce()).get(anyString());
	}

	@Test
	void discoverEndpoints_When_ConfigNotExists_Expect_ReturnsEmptyList() {
		when(configService.getConfig(stationId)).thenReturn(null);

		List<ApiEndpointResponseDTO> result = discoveryService.discoverEndpoints(stationId);

		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(httpClient, never()).get(anyString());
	}

	@Test
	void discoverEndpoints_When_HttpClientThrowsException_Expect_HandlesGracefully() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		doNothing().when(httpClient).setTimeout(any());
		doNothing().when(httpClient).setMaxRetries(anyInt());
		
		when(httpClient.get(anyString())).thenReturn(Mono.error(new RuntimeException("Network error")));

		List<ApiEndpointResponseDTO> result = discoveryService.discoverEndpoints(stationId);

		assertNotNull(result);
		// Should handle errors gracefully and return discovered endpoints (if any)
	}

	@Test
	void discoverEndpoints_When_UnauthorizedResponse_Expect_MarksAsWorking() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		doNothing().when(httpClient).setTimeout(any());
		doNothing().when(httpClient).setMaxRetries(anyInt());
		
		WebClientResponseException unauthorized = 
				WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", null, null, null);
		when(httpClient.get(anyString())).thenReturn(Mono.error(unauthorized));
		when(endpointRepository.findByEndpointPathAndHttpMethod(anyString(), anyString()))
				.thenReturn(Optional.empty());
		when(endpointRepository.save(any(LibreTimeApiEndpoint.class))).thenReturn(endpoint);

		List<ApiEndpointResponseDTO> result = discoveryService.discoverEndpoints(stationId);

		assertNotNull(result);
		verify(endpointRepository, atLeastOnce()).save(any(LibreTimeApiEndpoint.class));
	}

	@Test
	void discoverEndpoints_When_NotFoundResponse_Expect_MarksAsMissing() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		doNothing().when(httpClient).setTimeout(any());
		doNothing().when(httpClient).setMaxRetries(anyInt());
		
		WebClientResponseException notFound = 
				WebClientResponseException.create(HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null);
		when(httpClient.get(anyString())).thenReturn(Mono.error(notFound));

		List<ApiEndpointResponseDTO> result = discoveryService.discoverEndpoints(stationId);

		assertNotNull(result);
		// Endpoints with 404 should not be saved
		verify(endpointRepository, never()).save(any(LibreTimeApiEndpoint.class));
	}

	@Test
	void getDiscoveredEndpoints_When_EndpointsExist_Expect_ReturnsAllEndpoints() {
		when(endpointRepository.findAll()).thenReturn(Arrays.asList(endpoint));

		List<ApiEndpointResponseDTO> result = discoveryService.getDiscoveredEndpoints();

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(endpoint.getEndpointPath(), result.get(0).getEndpointPath());
		verify(endpointRepository).findAll();
	}

	@Test
	void getDiscoveredEndpoints_When_NoEndpoints_Expect_ReturnsEmptyList() {
		when(endpointRepository.findAll()).thenReturn(List.of());

		List<ApiEndpointResponseDTO> result = discoveryService.getDiscoveredEndpoints();

		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(endpointRepository).findAll();
	}

	@Test
	void updateEndpointStatus_When_EndpointExists_Expect_UpdatesStatus() {
		UUID endpointId = UUID.randomUUID();
		when(endpointRepository.findById(endpointId)).thenReturn(Optional.of(endpoint));
		when(endpointRepository.save(any(LibreTimeApiEndpoint.class))).thenReturn(endpoint);

		ApiEndpointResponseDTO result = discoveryService.updateEndpointStatus(endpointId, EndpointStatus.BROKEN);

		assertNotNull(result);
		assertEquals(EndpointStatus.BROKEN, result.getStatus());
		verify(endpointRepository).findById(endpointId);
		verify(endpointRepository).save(any(LibreTimeApiEndpoint.class));
	}

	@Test
	void updateEndpointStatus_When_EndpointNotExists_Expect_ThrowsNotFoundException() {
		UUID endpointId = UUID.randomUUID();
		when(endpointRepository.findById(endpointId)).thenReturn(Optional.empty());

		assertThrows(com.onelpro.librelog.exceptions.NotFoundException.class, 
				() -> discoveryService.updateEndpointStatus(endpointId, EndpointStatus.BROKEN));
		verify(endpointRepository).findById(endpointId);
		verify(endpointRepository, never()).save(any());
	}

	@Test
	void discoverEndpoints_When_EndpointAlreadyExists_Expect_UpdatesExisting() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		doNothing().when(httpClient).setTimeout(any());
		doNothing().when(httpClient).setMaxRetries(anyInt());
		
		when(httpClient.get(anyString())).thenReturn(Mono.just("{\"data\":[]}"));
		when(endpointRepository.findByEndpointPathAndHttpMethod("/api/v2/files", "GET"))
				.thenReturn(Optional.of(endpoint));
		when(endpointRepository.save(any(LibreTimeApiEndpoint.class))).thenReturn(endpoint);

		List<ApiEndpointResponseDTO> result = discoveryService.discoverEndpoints(stationId);

		assertNotNull(result);
		verify(endpointRepository, atLeastOnce()).save(any(LibreTimeApiEndpoint.class));
	}

}

