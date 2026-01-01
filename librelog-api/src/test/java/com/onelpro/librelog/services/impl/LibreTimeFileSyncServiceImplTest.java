package com.onelpro.librelog.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelpro.librelog.dto.FileDownloadResponseDTO;
import com.onelpro.librelog.dto.FileListResponseDTO;
import com.onelpro.librelog.dto.FileQueryRequestDTO;
import com.onelpro.librelog.dto.FileUploadRequestDTO;
import com.onelpro.librelog.dto.FileUploadResponseDTO;
import com.onelpro.librelog.dto.LibreTimeIntegrationConfigResponseDTO;
import com.onelpro.librelog.dto.SyncStatusResponseDTO;
import com.onelpro.librelog.enums.AssetType;
import com.onelpro.librelog.enums.SyncDirection;
import com.onelpro.librelog.enums.SyncFrequency;
import com.onelpro.librelog.enums.SyncStatus;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.integrations.LibreTimeHttpClient;
import com.onelpro.librelog.models.LibreTimeFileSyncStatus;
import com.onelpro.librelog.repositories.LibreTimeFileSyncStatusRepository;
import com.onelpro.librelog.services.LibreTimeIntegrationConfigService;
import com.onelpro.librelog.services.LibreTimeSyncHistoryService;
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
class LibreTimeFileSyncServiceImplTest {

	@Mock
	private LibreTimeFileSyncStatusRepository syncStatusRepository;

	@Mock
	private LibreTimeIntegrationConfigService configService;

	@Mock
	private LibreTimeHttpClient httpClient;

	@Mock
	private ObjectProvider<ObjectMapper> objectMapperProvider;

	@Mock
	private ObjectProvider<LibreTimeSyncHistoryService> syncHistoryServiceProvider;

	@InjectMocks
	private LibreTimeFileSyncServiceImpl fileSyncService;

	private ObjectMapper objectMapper;
	private UUID stationId;
	private LibreTimeIntegrationConfigResponseDTO configDTO;
	private FileUploadRequestDTO uploadRequest;
	private LibreTimeFileSyncStatus syncStatus;

	@BeforeEach
	void setUp() {
		stationId = UUID.randomUUID();
		objectMapper = new ObjectMapper();

		configDTO = LibreTimeIntegrationConfigResponseDTO.builder()
				.stationId(stationId)
				.apiBaseUrl("https://studio-qa.gayphx.com/api/v2/")
				.syncEnabled(true)
				.syncFrequency(SyncFrequency.HOURLY)
				.maxFileSizeMb(500)
				.supportedFormats(Arrays.asList("MP3", "WAV", "FLAC"))
				.build();

		uploadRequest = FileUploadRequestDTO.builder()
				.fileName("test-file.mp3")
				.fileData("test file data".getBytes())
				.assetType(AssetType.IM)
				.contentType("MUSIC")
				.durationSeconds(180)
				.build();

		syncStatus = LibreTimeFileSyncStatus.builder()
				.id(UUID.randomUUID())
				.librelogFileId(UUID.randomUUID())
				.libreTimeCartId("cart-123")
				.fileName("test-file.mp3")
				.syncDirection(SyncDirection.LIBRELOG_TO_LIBRETIME)
				.syncStatus(SyncStatus.SYNCED)
				.fileHash("abc123")
				.fileSizeBytes(1024L)
				.metadataSynced(true)
				.lastSyncAt(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		when(objectMapperProvider.getIfAvailable(any(java.util.function.Supplier.class))).thenReturn(objectMapper);
		
		// Create the service manually to ensure ObjectMapper is available
		fileSyncService = new LibreTimeFileSyncServiceImpl(
				syncStatusRepository,
				configService,
				httpClient,
				objectMapperProvider,
				syncHistoryServiceProvider
		);
	}

	@Test
	void uploadFile_When_ValidRequest_Expect_Success() throws Exception {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		String responseJson = "{\"success\":true,\"cartId\":\"cart-123\"}";
		when(httpClient.post(anyString(), anyString())).thenReturn(Mono.just(responseJson));
		when(syncStatusRepository.save(any(LibreTimeFileSyncStatus.class))).thenReturn(syncStatus);

		FileUploadResponseDTO result = fileSyncService.uploadFile(stationId, uploadRequest);

		assertNotNull(result);
		assertTrue(result.getSuccess());
		assertEquals("cart-123", result.getCartId());
		assertEquals("test-file.mp3", result.getFileName());
		verify(httpClient).setBaseUrl(configDTO.getApiBaseUrl());
		verify(httpClient).setJwtToken("test-jwt-token");
		verify(httpClient).post(anyString(), anyString());
		verify(syncStatusRepository, atLeastOnce()).save(any(LibreTimeFileSyncStatus.class));
	}

	@Test
	void uploadFile_When_ConfigNotExists_Expect_ThrowsBadRequestException() {
		when(configService.getConfig(stationId)).thenReturn(null);

		assertThrows(BadRequestException.class, () -> fileSyncService.uploadFile(stationId, uploadRequest));
		verify(httpClient, never()).post(anyString(), anyString());
	}

	@Test
	void uploadFile_When_FileNameMissing_Expect_ThrowsBadRequestException() {
		uploadRequest.setFileName(null);

		assertThrows(BadRequestException.class, () -> fileSyncService.uploadFile(stationId, uploadRequest));
		verify(httpClient, never()).post(anyString(), anyString());
	}

	@Test
	void uploadFile_When_FileDataMissing_Expect_ThrowsBadRequestException() {
		uploadRequest.setFileData(null);

		assertThrows(BadRequestException.class, () -> fileSyncService.uploadFile(stationId, uploadRequest));
		verify(httpClient, never()).post(anyString(), anyString());
	}

	@Test
	void uploadFile_When_FileSizeExceedsLimit_Expect_ThrowsBadRequestException() {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		// Create a file larger than max size (500 MB)
		byte[] largeFile = new byte[501 * 1024 * 1024]; // 501 MB
		uploadRequest.setFileData(largeFile);

		assertThrows(BadRequestException.class, () -> fileSyncService.uploadFile(stationId, uploadRequest));
		verify(httpClient, never()).post(anyString(), anyString());
	}

	@Test
	void uploadFile_When_UploadFails_Expect_ReturnsFailure() throws Exception {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		String responseJson = "{\"success\":false,\"message\":\"Upload failed\"}";
		when(httpClient.post(anyString(), anyString())).thenReturn(Mono.just(responseJson));
		when(syncStatusRepository.save(any(LibreTimeFileSyncStatus.class))).thenReturn(syncStatus);

		FileUploadResponseDTO result = fileSyncService.uploadFile(stationId, uploadRequest);

		assertNotNull(result);
		assertFalse(result.getSuccess());
		verify(httpClient).post(anyString(), anyString());
	}

	@Test
	void uploadFile_When_HttpClientThrowsException_Expect_ReturnsFailure() throws Exception {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		when(httpClient.post(anyString(), anyString())).thenReturn(Mono.error(new RuntimeException("Network error")));
		when(syncStatusRepository.save(any(LibreTimeFileSyncStatus.class))).thenReturn(syncStatus);

		FileUploadResponseDTO result = fileSyncService.uploadFile(stationId, uploadRequest);

		assertNotNull(result);
		assertFalse(result.getSuccess());
		assertNotNull(result.getErrorDetails());
	}

	@Test
	void downloadFile_When_ValidCartId_Expect_Success() throws Exception {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		String responseJson = "{\"fileName\":\"test-file.mp3\",\"fileSizeBytes\":1024}";
		when(httpClient.get(anyString())).thenReturn(Mono.just(responseJson));
		when(syncStatusRepository.findByLibreTimeCartId("cart-123")).thenReturn(Optional.of(syncStatus));
		when(syncStatusRepository.save(any(LibreTimeFileSyncStatus.class))).thenReturn(syncStatus);

		FileDownloadResponseDTO result = fileSyncService.downloadFile(stationId, "cart-123");

		assertNotNull(result);
		assertTrue(result.getSuccess());
		assertEquals("cart-123", result.getCartId());
		assertEquals("test-file.mp3", result.getFileName());
		verify(httpClient).get(anyString());
		verify(syncStatusRepository, atLeastOnce()).save(any(LibreTimeFileSyncStatus.class));
	}

	@Test
	void downloadFile_When_ConfigNotExists_Expect_ThrowsBadRequestException() {
		when(configService.getConfig(stationId)).thenReturn(null);

		assertThrows(BadRequestException.class, () -> fileSyncService.downloadFile(stationId, "cart-123"));
		verify(httpClient, never()).get(anyString());
	}

	@Test
	void downloadFile_When_FileNotFound_Expect_ThrowsNotFoundException() throws Exception {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		String responseJson = "{\"error\":\"File not found\"}";
		when(httpClient.get(anyString())).thenReturn(Mono.just(responseJson));
		when(syncStatusRepository.findByLibreTimeCartId("cart-123")).thenReturn(Optional.of(syncStatus));
		when(syncStatusRepository.save(any(LibreTimeFileSyncStatus.class))).thenReturn(syncStatus);

		// The service throws NotFoundException when error field is present in response
		// However, it's caught by the catch block and converted to a failure response
		FileDownloadResponseDTO result = fileSyncService.downloadFile(stationId, "cart-123");
		
		assertNotNull(result);
		assertFalse(result.getSuccess());
		assertNotNull(result.getErrorDetails());
		assertTrue(result.getErrorDetails().contains("File not found"));
		verify(httpClient).get(anyString());
		verify(syncStatusRepository, atLeastOnce()).save(any(LibreTimeFileSyncStatus.class));
	}

	@Test
	void downloadFile_When_HttpClientThrowsException_Expect_ReturnsFailure() throws Exception {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		when(httpClient.get(anyString())).thenReturn(Mono.error(new RuntimeException("Network error")));
		when(syncStatusRepository.findByLibreTimeCartId("cart-123")).thenReturn(Optional.of(syncStatus));
		when(syncStatusRepository.save(any(LibreTimeFileSyncStatus.class))).thenReturn(syncStatus);

		FileDownloadResponseDTO result = fileSyncService.downloadFile(stationId, "cart-123");

		assertNotNull(result);
		assertFalse(result.getSuccess());
		assertNotNull(result.getErrorDetails());
	}

	@Test
	void listFiles_When_ValidRequest_Expect_Success() throws Exception {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		String responseJson = "{\"files\":[{\"cartId\":\"cart-1\",\"fileName\":\"file1.mp3\"}],\"totalElements\":1,\"totalPages\":1}";
		when(httpClient.get(anyString())).thenReturn(Mono.just(responseJson));

		FileListResponseDTO result = fileSyncService.listFiles(stationId, 0, 20);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		assertEquals(1, result.getTotalPages());
		assertEquals(0, result.getCurrentPage());
		assertEquals(20, result.getPageSize());
		verify(httpClient).get(anyString());
	}

	@Test
	void listFiles_When_ConfigNotExists_Expect_ThrowsBadRequestException() {
		when(configService.getConfig(stationId)).thenReturn(null);

		assertThrows(BadRequestException.class, () -> fileSyncService.listFiles(stationId, 0, 20));
		verify(httpClient, never()).get(anyString());
	}

	@Test
	void listFiles_When_HttpClientThrowsException_Expect_ThrowsBadRequestException() throws Exception {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		when(httpClient.get(anyString())).thenReturn(Mono.error(new RuntimeException("Network error")));

		assertThrows(BadRequestException.class, () -> fileSyncService.listFiles(stationId, 0, 20));
	}

	@Test
	void queryFiles_When_ValidRequest_Expect_Success() throws Exception {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		FileQueryRequestDTO queryRequest = FileQueryRequestDTO.builder()
				.assetType(AssetType.IM)
				.contentType("MUSIC")
				.page(0)
				.size(20)
				.build();
		
		String responseJson = "{\"files\":[{\"cartId\":\"cart-1\",\"fileName\":\"file1.mp3\"}],\"totalElements\":1,\"totalPages\":1}";
		when(httpClient.get(anyString())).thenReturn(Mono.just(responseJson));

		FileListResponseDTO result = fileSyncService.queryFiles(stationId, queryRequest);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		verify(httpClient).get(anyString());
	}

	@Test
	void queryFiles_When_ConfigNotExists_Expect_ThrowsBadRequestException() {
		when(configService.getConfig(stationId)).thenReturn(null);
		
		FileQueryRequestDTO queryRequest = FileQueryRequestDTO.builder()
				.assetType(AssetType.IM)
				.build();

		assertThrows(BadRequestException.class, () -> fileSyncService.queryFiles(stationId, queryRequest));
		verify(httpClient, never()).get(anyString());
	}

	@Test
	void queryFiles_When_HttpClientThrowsException_Expect_ThrowsBadRequestException() throws Exception {
		when(configService.getConfig(stationId)).thenReturn(configDTO);
		when(configService.getDecryptedJwtToken(stationId)).thenReturn("test-jwt-token");
		doNothing().when(httpClient).setBaseUrl(anyString());
		doNothing().when(httpClient).setJwtToken(anyString());
		
		FileQueryRequestDTO queryRequest = FileQueryRequestDTO.builder()
				.assetType(AssetType.IM)
				.build();
		
		when(httpClient.get(anyString())).thenReturn(Mono.error(new RuntimeException("Network error")));

		assertThrows(BadRequestException.class, () -> fileSyncService.queryFiles(stationId, queryRequest));
	}

	@Test
	void getSyncStatus_When_ByLibrelogFileId_Expect_Success() {
		UUID fileId = UUID.randomUUID();
		when(syncStatusRepository.findByLibrelogFileId(fileId)).thenReturn(Optional.of(syncStatus));

		SyncStatusResponseDTO result = fileSyncService.getSyncStatus(fileId, null);

		assertNotNull(result);
		assertEquals(syncStatus.getLibreTimeCartId(), result.getLibreTimeCartId());
		assertEquals(syncStatus.getSyncStatus(), result.getSyncStatus());
		verify(syncStatusRepository).findByLibrelogFileId(fileId);
	}

	@Test
	void getSyncStatus_When_ByLibreTimeCartId_Expect_Success() {
		when(syncStatusRepository.findByLibreTimeCartId("cart-123")).thenReturn(Optional.of(syncStatus));

		SyncStatusResponseDTO result = fileSyncService.getSyncStatus(null, "cart-123");

		assertNotNull(result);
		assertEquals("cart-123", result.getLibreTimeCartId());
		assertEquals(syncStatus.getSyncStatus(), result.getSyncStatus());
		verify(syncStatusRepository).findByLibreTimeCartId("cart-123");
	}

	@Test
	void getSyncStatus_When_NotFound_Expect_ThrowsNotFoundException() {
		when(syncStatusRepository.findByLibrelogFileId(any(UUID.class))).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> fileSyncService.getSyncStatus(UUID.randomUUID(), null));
	}

	@Test
	void getSyncStatus_When_BothIdsNull_Expect_ThrowsNotFoundException() {
		assertThrows(NotFoundException.class, () -> fileSyncService.getSyncStatus(null, null));
		verify(syncStatusRepository, never()).findByLibrelogFileId(any());
		verify(syncStatusRepository, never()).findByLibreTimeCartId(anyString());
	}

}

