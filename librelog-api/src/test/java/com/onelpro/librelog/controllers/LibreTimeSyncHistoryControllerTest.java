package com.onelpro.librelog.controllers;

import com.onelpro.librelog.config.GlobalExceptionHandler;
import com.onelpro.librelog.dto.SyncHistoryResponseDTO;
import com.onelpro.librelog.dto.SyncStatisticsResponseDTO;
import com.onelpro.librelog.enums.SyncType;
import com.onelpro.librelog.services.LibreTimeSyncHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LibreTimeSyncHistoryControllerTest {

	@Mock
	private LibreTimeSyncHistoryService syncHistoryService;

	private MockMvc mockMvc;
	private UUID historyId;
	private UUID userId;

	@BeforeEach
	void setUp() {
		LibreTimeSyncHistoryController controller = new LibreTimeSyncHistoryController(syncHistoryService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
		historyId = UUID.randomUUID();
		userId = UUID.randomUUID();
	}

	@Test
	void getSyncHistory_When_HistoryExists_Expect_ReturnsHistory() throws Exception {
		SyncHistoryResponseDTO history = SyncHistoryResponseDTO.builder()
				.id(historyId)
				.syncType(SyncType.FILE_UPLOAD)
				.status("SYNCED")
				.itemsTotal(1)
				.itemsSucceeded(1)
				.itemsFailed(0)
				.startedAt(LocalDateTime.now().minusMinutes(5))
				.completedAt(LocalDateTime.now())
				.build();

		when(syncHistoryService.getSyncHistory(historyId)).thenReturn(history);

		mockMvc.perform(get("/api/libretime/sync-history/{historyId}", historyId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(historyId.toString()))
				.andExpect(jsonPath("$.syncType").value("FILE_UPLOAD"))
				.andExpect(jsonPath("$.status").value("SYNCED"));
	}

	@Test
	void getSyncHistory_When_WithDateRange_Expect_ReturnsList() throws Exception {
		SyncHistoryResponseDTO history = SyncHistoryResponseDTO.builder()
				.id(historyId)
				.syncType(SyncType.FILE_UPLOAD)
				.status("SYNCED")
				.itemsTotal(1)
				.itemsSucceeded(1)
				.itemsFailed(0)
				.startedAt(LocalDateTime.now().minusMinutes(5))
				.completedAt(LocalDateTime.now())
				.build();

		LocalDateTime startDate = LocalDateTime.now().minusDays(7);
		LocalDateTime endDate = LocalDateTime.now();

		when(syncHistoryService.getSyncHistoryByDateRange(startDate, endDate))
				.thenReturn(Arrays.asList(history));

		mockMvc.perform(get("/api/libretime/sync-history")
						.param("startDate", startDate.toString())
						.param("endDate", endDate.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].id").value(historyId.toString()));
	}

	@Test
	void getSyncHistory_When_WithSyncType_Expect_ReturnsFilteredList() throws Exception {
		SyncHistoryResponseDTO history = SyncHistoryResponseDTO.builder()
				.id(historyId)
				.syncType(SyncType.FILE_UPLOAD)
				.status("SYNCED")
				.itemsTotal(1)
				.itemsSucceeded(1)
				.itemsFailed(0)
				.startedAt(LocalDateTime.now().minusMinutes(5))
				.completedAt(LocalDateTime.now())
				.build();

		when(syncHistoryService.getSyncHistoryByType(SyncType.FILE_UPLOAD))
				.thenReturn(Arrays.asList(history));

		mockMvc.perform(get("/api/libretime/sync-history")
						.param("syncType", "FILE_UPLOAD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].syncType").value("FILE_UPLOAD"));
	}

	@Test
	void getSyncHistory_When_WithUserId_Expect_ReturnsFilteredList() throws Exception {
		SyncHistoryResponseDTO history = SyncHistoryResponseDTO.builder()
				.id(historyId)
				.initiatedBy(userId)
				.syncType(SyncType.FILE_UPLOAD)
				.status("SYNCED")
				.itemsTotal(1)
				.itemsSucceeded(1)
				.startedAt(LocalDateTime.now().minusMinutes(5))
				.completedAt(LocalDateTime.now())
				.build();

		when(syncHistoryService.getSyncHistoryByUser(userId))
				.thenReturn(Arrays.asList(history));

		mockMvc.perform(get("/api/libretime/sync-history")
						.param("userId", userId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].initiatedBy").value(userId.toString()));
	}

	@Test
	void getSyncStatistics_When_Requested_Expect_ReturnsStatistics() throws Exception {
		SyncStatisticsResponseDTO statistics = SyncStatisticsResponseDTO.builder()
				.totalFiles(100)
				.syncedFiles(90)
				.failedFiles(5)
				.pendingFiles(5)
				.successRate(90.0)
				.lastSyncAt(LocalDateTime.now())
				.calculatedAt(LocalDateTime.now())
				.build();

		when(syncHistoryService.getSyncStatistics(any(), any()))
				.thenReturn(statistics);

		mockMvc.perform(get("/api/libretime/sync-history/statistics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalFiles").value(100))
				.andExpect(jsonPath("$.syncedFiles").value(90))
				.andExpect(jsonPath("$.successRate").value(90.0));
	}

	@Test
	void getSyncStatistics_When_WithDateRange_Expect_ReturnsFilteredStatistics() throws Exception {
		SyncStatisticsResponseDTO statistics = SyncStatisticsResponseDTO.builder()
				.totalFiles(50)
				.syncedFiles(45)
				.failedFiles(3)
				.pendingFiles(2)
				.successRate(90.0)
				.lastSyncAt(LocalDateTime.now())
				.calculatedAt(LocalDateTime.now())
				.build();

		LocalDateTime startDate = LocalDateTime.now().minusDays(7);
		LocalDateTime endDate = LocalDateTime.now();

		when(syncHistoryService.getSyncStatistics(startDate, endDate))
				.thenReturn(statistics);

		mockMvc.perform(get("/api/libretime/sync-history/statistics")
						.param("startDate", startDate.toString())
						.param("endDate", endDate.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalFiles").value(50));
	}

}

