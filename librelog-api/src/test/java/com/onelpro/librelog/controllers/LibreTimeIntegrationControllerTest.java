package com.onelpro.librelog.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelpro.librelog.dto.ConnectionTestResponseDTO;
import com.onelpro.librelog.dto.LibreTimeIntegrationConfigRequestDTO;
import com.onelpro.librelog.dto.LibreTimeIntegrationConfigResponseDTO;
import com.onelpro.librelog.enums.ConflictResolution;
import com.onelpro.librelog.enums.SyncDirection;
import com.onelpro.librelog.enums.SyncFrequency;
import com.onelpro.librelog.config.GlobalExceptionHandler;
import com.onelpro.librelog.services.LibreTimeIntegrationConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LibreTimeIntegrationControllerTest {

	@Mock
	private LibreTimeIntegrationConfigService configService;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;
	private UUID stationId;
	private LibreTimeIntegrationConfigResponseDTO configDTO;
	private LibreTimeIntegrationConfigRequestDTO requestDTO;

	@BeforeEach
	void setUp() {
		LibreTimeIntegrationController controller = new LibreTimeIntegrationController(configService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
		objectMapper = new ObjectMapper();
		stationId = UUID.randomUUID();

		configDTO = LibreTimeIntegrationConfigResponseDTO.builder()
				.stationId(stationId)
				.apiBaseUrl("https://studio-qa.gayphx.com/api/v2/")
				.syncEnabled(true)
				.syncFrequency(SyncFrequency.HOURLY)
				.maxFileSizeMb(500)
				.supportedFormats(Arrays.asList("MP3", "WAV"))
				.build();

		requestDTO = LibreTimeIntegrationConfigRequestDTO.builder()
				.apiBaseUrl("https://studio-qa.gayphx.com/api/v2/")
				.jwtToken("test-jwt-token")
				.syncEnabled(true)
				.syncFrequency(SyncFrequency.HOURLY)
				.syncDirection(SyncDirection.LIBRELOG_TO_LIBRETIME)
				.conflictResolution(ConflictResolution.KEEP_LATEST)
				.webhookEnabled(false)
				.maxFileSizeMb(500)
				.supportedFormats(Arrays.asList("MP3", "WAV"))
				.build();
	}

	@Test
	void getConfig_When_ConfigExists_Expect_ReturnsConfig() throws Exception {
		when(configService.getConfig(stationId)).thenReturn(configDTO);

		mockMvc.perform(get("/api/libretime/integration/config")
						.param("stationId", stationId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stationId").value(stationId.toString()))
				.andExpect(jsonPath("$.apiBaseUrl").value("https://studio-qa.gayphx.com/api/v2/"))
				.andExpect(jsonPath("$.syncEnabled").value(true));
	}

	@Test
	void getConfig_When_ConfigNotExists_Expect_ReturnsNotFound() throws Exception {
		when(configService.getConfig(stationId)).thenReturn(null);

		mockMvc.perform(get("/api/libretime/integration/config")
						.param("stationId", stationId.toString()))
				.andExpect(status().isNotFound());
	}

	@Test
	void createConfig_When_ValidRequest_Expect_ReturnsCreated() throws Exception {
		when(configService.saveConfig(eq(stationId), any(LibreTimeIntegrationConfigRequestDTO.class), any(UUID.class)))
				.thenReturn(configDTO);

		mockMvc.perform(post("/api/libretime/integration/config")
						.param("stationId", stationId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(requestDTO))
						.header("X-User-Id", UUID.randomUUID().toString()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.stationId").value(stationId.toString()));
	}

	@Test
	void createConfig_When_InvalidRequest_Expect_ReturnsBadRequest() throws Exception {
		// Missing required fields
		LibreTimeIntegrationConfigRequestDTO invalidRequest = new LibreTimeIntegrationConfigRequestDTO();

		mockMvc.perform(post("/api/libretime/integration/config")
						.param("stationId", stationId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidRequest)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateConfig_When_ValidRequest_Expect_ReturnsOk() throws Exception {
		when(configService.updateConfig(eq(stationId), any(LibreTimeIntegrationConfigRequestDTO.class), any(UUID.class)))
				.thenReturn(configDTO);

		mockMvc.perform(put("/api/libretime/integration/config")
						.param("stationId", stationId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(requestDTO))
						.header("X-User-Id", UUID.randomUUID().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stationId").value(stationId.toString()));
	}

	@Test
	void updateConfig_When_InvalidRequest_Expect_ReturnsBadRequest() throws Exception {
		LibreTimeIntegrationConfigRequestDTO invalidRequest = new LibreTimeIntegrationConfigRequestDTO();

		mockMvc.perform(put("/api/libretime/integration/config")
						.param("stationId", stationId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidRequest)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testConnection_When_ConfigExists_Expect_ReturnsResult() throws Exception {
		ConnectionTestResponseDTO testResult = ConnectionTestResponseDTO.builder()
				.success(true)
				.message("Connection successful")
				.responseTimeMs(100)
				.testedAt(LocalDateTime.now())
				.build();

		when(configService.testConnection(stationId)).thenReturn(testResult);

		mockMvc.perform(post("/api/libretime/integration/test-connection")
						.param("stationId", stationId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Connection successful"));
	}

	@Test
	void testConnection_When_ConfigNotExists_Expect_ReturnsError() throws Exception {
		ConnectionTestResponseDTO testResult = ConnectionTestResponseDTO.builder()
				.success(false)
				.message("Configuration not found")
				.testedAt(LocalDateTime.now())
				.build();

		when(configService.testConnection(stationId)).thenReturn(testResult);

		mockMvc.perform(post("/api/libretime/integration/test-connection")
						.param("stationId", stationId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(false));
	}

}

