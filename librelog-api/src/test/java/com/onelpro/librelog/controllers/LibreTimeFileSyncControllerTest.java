package com.onelpro.librelog.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelpro.librelog.dto.*;
import com.onelpro.librelog.enums.AssetType;
import com.onelpro.librelog.enums.SyncDirection;
import com.onelpro.librelog.enums.SyncStatus;
import com.onelpro.librelog.config.GlobalExceptionHandler;
import com.onelpro.librelog.services.LibreTimeFileSyncService;
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
class LibreTimeFileSyncControllerTest {

	@Mock
	private LibreTimeFileSyncService fileSyncService;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;
	private UUID stationId;

	@BeforeEach
	void setUp() {
		LibreTimeFileSyncController controller = new LibreTimeFileSyncController(fileSyncService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
		objectMapper = new ObjectMapper();
		stationId = UUID.randomUUID();
	}

	@Test
	void uploadFile_When_ValidRequest_Expect_ReturnsOk() throws Exception {
		FileUploadRequestDTO request = FileUploadRequestDTO.builder()
				.fileName("test-file.mp3")
				.fileData("test data".getBytes())
				.assetType(AssetType.IM)
				.build();

		FileUploadResponseDTO response = FileUploadResponseDTO.builder()
				.cartId("cart-123")
				.fileName("test-file.mp3")
				.success(true)
				.message("File uploaded successfully")
				.fileSizeBytes(9L)
				.uploadedAt(LocalDateTime.now())
				.build();

		when(fileSyncService.uploadFile(eq(stationId), any(FileUploadRequestDTO.class)))
				.thenReturn(response);

		mockMvc.perform(post("/api/libretime/files/upload")
						.param("stationId", stationId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.cartId").value("cart-123"));
	}

	@Test
	void uploadFile_When_InvalidRequest_Expect_ReturnsBadRequest() throws Exception {
		FileUploadRequestDTO request = new FileUploadRequestDTO(); // Missing required fields

		mockMvc.perform(post("/api/libretime/files/upload")
						.param("stationId", stationId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void downloadFile_When_FileExists_Expect_ReturnsOk() throws Exception {
		FileDownloadResponseDTO response = FileDownloadResponseDTO.builder()
				.cartId("cart-123")
				.fileName("test-file.mp3")
				.success(true)
				.message("File downloaded successfully")
				.fileSizeBytes(1024L)
				.downloadedAt(LocalDateTime.now())
				.build();

		when(fileSyncService.downloadFile(stationId, "cart-123")).thenReturn(response);

		mockMvc.perform(get("/api/libretime/files/download/cart-123")
						.param("stationId", stationId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.cartId").value("cart-123"));
	}

	@Test
	void listFiles_When_Requested_Expect_ReturnsList() throws Exception {
		FileListResponseDTO.FileInfoDTO fileInfo = FileListResponseDTO.FileInfoDTO.builder()
				.cartId("cart-1")
				.fileName("file1.mp3")
				.fileSizeBytes(1024L)
				.build();

		FileListResponseDTO response = FileListResponseDTO.builder()
				.files(Arrays.asList(fileInfo))
				.totalElements(1)
				.totalPages(1)
				.currentPage(0)
				.pageSize(20)
				.hasNext(false)
				.hasPrevious(false)
				.build();

		when(fileSyncService.listFiles(stationId, 0, 20)).thenReturn(response);

		mockMvc.perform(get("/api/libretime/files")
						.param("stationId", stationId.toString())
						.param("page", "0")
						.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.files").isArray());
	}

	@Test
	void queryFiles_When_ValidRequest_Expect_ReturnsList() throws Exception {
		FileQueryRequestDTO queryRequest = FileQueryRequestDTO.builder()
				.assetType(AssetType.IM)
				.contentType("MUSIC")
				.page(0)
				.size(20)
				.build();

		FileListResponseDTO.FileInfoDTO fileInfo = FileListResponseDTO.FileInfoDTO.builder()
				.cartId("cart-1")
				.fileName("file1.mp3")
				.build();

		FileListResponseDTO response = FileListResponseDTO.builder()
				.files(Arrays.asList(fileInfo))
				.totalElements(1)
				.totalPages(1)
				.currentPage(0)
				.pageSize(20)
				.build();

		when(fileSyncService.queryFiles(eq(stationId), any(FileQueryRequestDTO.class)))
				.thenReturn(response);

		mockMvc.perform(post("/api/libretime/files/query")
						.param("stationId", stationId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(queryRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void getSyncStatus_When_StatusExists_Expect_ReturnsStatus() throws Exception {
		SyncStatusResponseDTO response = SyncStatusResponseDTO.builder()
				.id(UUID.randomUUID())
				.libreTimeCartId("cart-123")
				.fileName("test-file.mp3")
				.syncStatus(SyncStatus.SYNCED)
				.syncDirection(SyncDirection.LIBRELOG_TO_LIBRETIME)
				.lastSyncAt(LocalDateTime.now())
				.build();

		when(fileSyncService.getSyncStatus(null, "cart-123")).thenReturn(response);

		mockMvc.perform(get("/api/libretime/files/sync-status")
						.param("libreTimeCartId", "cart-123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.libreTimeCartId").value("cart-123"))
				.andExpect(jsonPath("$.syncStatus").value("SYNCED"));
	}

}

