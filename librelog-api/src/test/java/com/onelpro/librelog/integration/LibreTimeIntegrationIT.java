package com.onelpro.librelog.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelpro.librelog.config.TestcontainersConfiguration;
import com.onelpro.librelog.dto.*;
import com.onelpro.librelog.enums.AssetType;
import com.onelpro.librelog.enums.SyncFrequency;
import com.onelpro.librelog.enums.SyncType;
import com.onelpro.librelog.models.LibreTimeIntegrationConfig;
import com.onelpro.librelog.models.Station;
import com.onelpro.librelog.repositories.LibreTimeIntegrationConfigRepository;
import com.onelpro.librelog.repositories.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for LibreTime REST API controllers.
 * Tests end-to-end workflows including configuration, file sync, and testing endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class LibreTimeIntegrationIT {

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	private ObjectMapper objectMapper;

	@Autowired
	private StationRepository stationRepository;

	@Autowired
	private LibreTimeIntegrationConfigRepository configRepository;

	private Station testStation;
	private UUID stationId;

	@BeforeEach
	void setUp() {
		// Setup MockMvc
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		
		// Create ObjectMapper
		objectMapper = new ObjectMapper();
		
		// Create a test station
		testStation = Station.builder()
				.name("Test Station")
				.callSign("TEST")
				.build();
		testStation = stationRepository.save(testStation);
		stationId = testStation.getId();
	}

	@Test
	@WithMockUser(authorities = {"LIBRETIME_INTEGRATION_VIEW"})
	void testGetConfig_When_ConfigExists_Expect_ReturnsConfig() throws Exception {
		// Create a test config
		LibreTimeIntegrationConfig config = LibreTimeIntegrationConfig.builder()
				.station(testStation)
				.apiBaseUrl("https://studio-qa.gayphx.com/api/v2/")
				.jwtToken("encrypted-token")
				.syncEnabled(true)
				.syncFrequency(SyncFrequency.HOURLY)
				.build();
		configRepository.save(config);

		mockMvc.perform(get("/api/libretime/integration/config")
						.param("stationId", stationId.toString())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stationId").value(stationId.toString()))
				.andExpect(jsonPath("$.apiBaseUrl").value("https://studio-qa.gayphx.com/api/v2/"))
				.andExpect(jsonPath("$.syncEnabled").value(true));
	}

	@Test
	@WithMockUser(authorities = {"LIBRETIME_INTEGRATION_VIEW"})
	void testGetConfig_When_ConfigNotExists_Expect_ReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/libretime/integration/config")
						.param("stationId", UUID.randomUUID().toString())
						.with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(authorities = {"LIBRETIME_INTEGRATION_CONFIGURE"})
	void testCreateConfig_When_ValidRequest_Expect_ReturnsCreated() throws Exception {
		LibreTimeIntegrationConfigRequestDTO request = LibreTimeIntegrationConfigRequestDTO.builder()
				.apiBaseUrl("https://studio-qa.gayphx.com/api/v2/")
				.jwtToken("test-jwt-token")
				.syncEnabled(true)
				.syncFrequency(SyncFrequency.HOURLY)
				.maxFileSizeMb(500)
				.supportedFormats(Arrays.asList("MP3", "WAV"))
				.build();

		mockMvc.perform(post("/api/libretime/integration/config")
						.param("stationId", stationId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.header("X-User-Id", UUID.randomUUID().toString())
						.with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.stationId").value(stationId.toString()))
				.andExpect(jsonPath("$.apiBaseUrl").value("https://studio-qa.gayphx.com/api/v2/"));
	}

	@Test
	@WithMockUser(authorities = {"LIBRETIME_INTEGRATION_CONFIGURE"})
	void testCreateConfig_When_InvalidRequest_Expect_ReturnsBadRequest() throws Exception {
		LibreTimeIntegrationConfigRequestDTO request = LibreTimeIntegrationConfigRequestDTO.builder()
				.apiBaseUrl("invalid-url") // Invalid URL format
				.build();

		mockMvc.perform(post("/api/libretime/integration/config")
						.param("stationId", stationId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.with(csrf()))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(authorities = {"LIBRETIME_INTEGRATION_VIEW"})
	void testTestConnection_When_ConfigExists_Expect_ReturnsResult() throws Exception {
		// Create a test config
		LibreTimeIntegrationConfig config = LibreTimeIntegrationConfig.builder()
				.station(testStation)
				.apiBaseUrl("https://studio-qa.gayphx.com/api/v2/")
				.jwtToken("encrypted-token")
				.syncEnabled(true)
				.build();
		configRepository.save(config);

		// Note: This will fail if the actual API is not accessible, but tests the endpoint structure
		mockMvc.perform(post("/api/libretime/integration/test-connection")
						.param("stationId", stationId.toString())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").exists());
	}

	@Test
	@WithMockUser(authorities = {"LIBRETIME_INTEGRATION_SYNC"})
	void testUploadFile_When_ValidRequest_Expect_ReturnsResponse() throws Exception {
		// Create a test config
		LibreTimeIntegrationConfig config = LibreTimeIntegrationConfig.builder()
				.station(testStation)
				.apiBaseUrl("https://studio-qa.gayphx.com/api/v2/")
				.jwtToken("encrypted-token")
				.syncEnabled(true)
				.maxFileSizeMb(500)
				.build();
		configRepository.save(config);

		FileUploadRequestDTO request = FileUploadRequestDTO.builder()
				.fileName("test-file.mp3")
				.fileData("test file data".getBytes())
				.assetType(AssetType.IM)
				.contentType("MUSIC")
				.build();

		// Note: This will fail if the actual API is not accessible, but tests the endpoint structure
		mockMvc.perform(post("/api/libretime/files/upload")
						.param("stationId", stationId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").exists());
	}

	@Test
	@WithMockUser(authorities = {"LIBRETIME_INTEGRATION_SYNC"})
	void testUploadFile_When_InvalidRequest_Expect_ReturnsBadRequest() throws Exception {
		FileUploadRequestDTO request = FileUploadRequestDTO.builder()
				.fileName(null) // Missing required field
				.fileData("test".getBytes())
				.build();

		mockMvc.perform(post("/api/libretime/files/upload")
						.param("stationId", stationId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.with(csrf()))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(authorities = {"LIBRETIME_INTEGRATION_VIEW"})
	void testListFiles_When_ConfigExists_Expect_ReturnsList() throws Exception {
		// Create a test config
		LibreTimeIntegrationConfig config = LibreTimeIntegrationConfig.builder()
				.station(testStation)
				.apiBaseUrl("https://studio-qa.gayphx.com/api/v2/")
				.jwtToken("encrypted-token")
				.syncEnabled(true)
				.build();
		configRepository.save(config);

		// Note: This will fail if the actual API is not accessible, but tests the endpoint structure
		mockMvc.perform(get("/api/libretime/files/list")
						.param("stationId", stationId.toString())
						.param("page", "0")
						.param("size", "20")
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").exists());
	}

	@Test
	@WithMockUser(authorities = {"LIBRETIME_INTEGRATION_TEST"})
	void testDiscoverEndpoints_When_ConfigExists_Expect_ReturnsEndpoints() throws Exception {
		// Create a test config
		LibreTimeIntegrationConfig config = LibreTimeIntegrationConfig.builder()
				.station(testStation)
				.apiBaseUrl("https://studio-qa.gayphx.com/api/v2/")
				.jwtToken("encrypted-token")
				.syncEnabled(true)
				.build();
		configRepository.save(config);

		// Note: This will fail if the actual API is not accessible, but tests the endpoint structure
		mockMvc.perform(post("/api/libretime/testing/discover/{stationId}", stationId)
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").exists());
	}

	@Test
	@WithMockUser(authorities = {"LIBRETIME_INTEGRATION_TEST"})
	void testTestConnectionEndpoint_When_ConfigExists_Expect_ReturnsResult() throws Exception {
		// Create a test config
		LibreTimeIntegrationConfig config = LibreTimeIntegrationConfig.builder()
				.station(testStation)
				.apiBaseUrl("https://studio-qa.gayphx.com/api/v2/")
				.jwtToken("encrypted-token")
				.syncEnabled(true)
				.build();
		configRepository.save(config);

		// Note: This will fail if the actual API is not accessible, but tests the endpoint structure
		mockMvc.perform(post("/api/libretime/testing/test-connection/{stationId}", stationId)
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").exists());
	}

	@Test
	@WithMockUser(authorities = {"LIBRETIME_INTEGRATION_VIEW"})
	void testGetSyncHistory_When_HistoryExists_Expect_ReturnsHistory() throws Exception {
		mockMvc.perform(get("/api/libretime/sync/history")
						.param("startDate", "2024-01-01T00:00:00")
						.param("endDate", "2024-12-31T23:59:59")
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").exists());
	}

	@Test
	@WithMockUser(authorities = {"LIBRETIME_INTEGRATION_VIEW"})
	void testGetSyncStatistics_When_Requested_Expect_ReturnsStatistics() throws Exception {
		mockMvc.perform(get("/api/libretime/sync/statistics")
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").exists())
				.andExpect(jsonPath("$.totalFiles").exists())
				.andExpect(jsonPath("$.totalSyncOperations").exists());
	}

	@Test
	@WithMockUser // No specific authorities
	void testGetConfig_When_Unauthorized_Expect_ReturnsForbidden() throws Exception {
		mockMvc.perform(get("/api/libretime/integration/config")
						.param("stationId", stationId.toString())
						.with(csrf()))
				.andExpect(status().isForbidden());
	}

}

