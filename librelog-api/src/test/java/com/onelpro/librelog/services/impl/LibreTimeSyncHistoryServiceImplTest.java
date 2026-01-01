package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.SyncHistoryResponseDTO;
import com.onelpro.librelog.dto.SyncStatisticsResponseDTO;
import com.onelpro.librelog.enums.SyncDirection;
import com.onelpro.librelog.enums.SyncStatus;
import com.onelpro.librelog.enums.SyncType;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.models.LibreTimeFileSyncStatus;
import com.onelpro.librelog.models.LibreTimeSyncHistory;
import com.onelpro.librelog.repositories.LibreTimeFileSyncStatusRepository;
import com.onelpro.librelog.repositories.LibreTimeSyncHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibreTimeSyncHistoryServiceImplTest {

	@Mock
	private LibreTimeSyncHistoryRepository historyRepository;

	@Mock
	private LibreTimeFileSyncStatusRepository fileSyncStatusRepository;

	@InjectMocks
	private LibreTimeSyncHistoryServiceImpl syncHistoryService;

	private UUID userId;
	private UUID historyId;
	private LibreTimeSyncHistory syncHistory;
	private LibreTimeFileSyncStatus fileSyncStatus;

	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();
		historyId = UUID.randomUUID();

		syncHistory = LibreTimeSyncHistory.builder()
				.id(historyId)
				.syncType(SyncType.FILE_UPLOAD)
				.status("started")
				.itemsTotal(0)
				.itemsSucceeded(0)
				.itemsFailed(0)
				.startedAt(LocalDateTime.now())
				.initiatedBy(userId)
				.details("{\"test\":\"data\"}")
				.build();

		fileSyncStatus = LibreTimeFileSyncStatus.builder()
				.id(UUID.randomUUID())
				.fileName("test-file.mp3")
				.syncDirection(SyncDirection.LIBRELOG_TO_LIBRETIME)
				.syncStatus(SyncStatus.SYNCED)
				.lastSyncAt(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
	}

	@Test
	void createSyncHistory_When_ValidRequest_Expect_ReturnsHistory() {
		when(historyRepository.save(any(LibreTimeSyncHistory.class))).thenAnswer(invocation -> {
			LibreTimeSyncHistory history = invocation.getArgument(0);
			history.setId(historyId);
			return history;
		});

		SyncHistoryResponseDTO result = syncHistoryService.createSyncHistory(
				SyncType.FILE_UPLOAD, userId, "{\"test\":\"data\"}");

		assertNotNull(result);
		assertEquals(SyncType.FILE_UPLOAD, result.getSyncType());
		assertEquals("started", result.getStatus());
		assertEquals(userId, result.getInitiatedBy());
		assertEquals(0, result.getItemsTotal());
		assertNotNull(result.getStartedAt());
		verify(historyRepository).save(any(LibreTimeSyncHistory.class));
	}

	@Test
	void createSyncHistory_When_NoDetails_Expect_ReturnsHistory() {
		when(historyRepository.save(any(LibreTimeSyncHistory.class))).thenAnswer(invocation -> {
			LibreTimeSyncHistory history = invocation.getArgument(0);
			history.setId(historyId);
			return history;
		});

		SyncHistoryResponseDTO result = syncHistoryService.createSyncHistory(
				SyncType.BATCH_SYNC, userId, null);

		assertNotNull(result);
		assertEquals(SyncType.BATCH_SYNC, result.getSyncType());
		assertNull(result.getDetails());
		verify(historyRepository).save(any(LibreTimeSyncHistory.class));
	}

	@Test
	void completeSyncHistory_When_HistoryExists_Expect_UpdatesHistory() {
		when(historyRepository.findById(historyId)).thenReturn(Optional.of(syncHistory));
		when(historyRepository.save(any(LibreTimeSyncHistory.class))).thenReturn(syncHistory);

		syncHistoryService.completeSyncHistory(historyId, "completed", 10, 2, "Some errors occurred");

		verify(historyRepository).findById(historyId);
		verify(historyRepository).save(any(LibreTimeSyncHistory.class));
		assertEquals("completed", syncHistory.getStatus());
		assertEquals(10, syncHistory.getItemsSucceeded());
		assertEquals(2, syncHistory.getItemsFailed());
		assertEquals(12, syncHistory.getItemsTotal());
		assertNotNull(syncHistory.getCompletedAt());
	}

	@Test
	void completeSyncHistory_When_HistoryNotExists_Expect_ThrowsNotFoundException() {
		when(historyRepository.findById(historyId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> 
				syncHistoryService.completeSyncHistory(historyId, "completed", 10, 2, null));
		verify(historyRepository).findById(historyId);
		verify(historyRepository, never()).save(any());
	}

	@Test
	void completeSyncHistory_When_NoErrors_Expect_UpdatesWithoutErrorSummary() {
		when(historyRepository.findById(historyId)).thenReturn(Optional.of(syncHistory));
		when(historyRepository.save(any(LibreTimeSyncHistory.class))).thenReturn(syncHistory);

		syncHistoryService.completeSyncHistory(historyId, "completed", 5, 0, null);

		assertNull(syncHistory.getErrorSummary());
		verify(historyRepository).save(any(LibreTimeSyncHistory.class));
	}

	@Test
	void getSyncHistory_When_HistoryExists_Expect_ReturnsHistory() {
		when(historyRepository.findById(historyId)).thenReturn(Optional.of(syncHistory));

		SyncHistoryResponseDTO result = syncHistoryService.getSyncHistory(historyId);

		assertNotNull(result);
		assertEquals(historyId, result.getId());
		assertEquals(SyncType.FILE_UPLOAD, result.getSyncType());
		verify(historyRepository).findById(historyId);
	}

	@Test
	void getSyncHistory_When_HistoryNotExists_Expect_ThrowsNotFoundException() {
		when(historyRepository.findById(historyId)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> syncHistoryService.getSyncHistory(historyId));
		verify(historyRepository).findById(historyId);
	}

	@Test
	void getSyncHistoryByDateRange_When_RecordsExist_Expect_ReturnsList() {
		LocalDateTime startDate = LocalDateTime.now().minusDays(7);
		LocalDateTime endDate = LocalDateTime.now();
		
		when(historyRepository.findByStartedAtBetween(startDate, endDate))
				.thenReturn(Arrays.asList(syncHistory));

		List<SyncHistoryResponseDTO> result = syncHistoryService.getSyncHistoryByDateRange(startDate, endDate);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(historyId, result.get(0).getId());
		verify(historyRepository).findByStartedAtBetween(startDate, endDate);
	}

	@Test
	void getSyncHistoryByDateRange_When_NoRecords_Expect_ReturnsEmptyList() {
		LocalDateTime startDate = LocalDateTime.now().minusDays(7);
		LocalDateTime endDate = LocalDateTime.now();
		
		when(historyRepository.findByStartedAtBetween(startDate, endDate))
				.thenReturn(List.of());

		List<SyncHistoryResponseDTO> result = syncHistoryService.getSyncHistoryByDateRange(startDate, endDate);

		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(historyRepository).findByStartedAtBetween(startDate, endDate);
	}

	@Test
	void getSyncHistoryByType_When_RecordsExist_Expect_ReturnsList() {
		when(historyRepository.findBySyncType(SyncType.FILE_UPLOAD))
				.thenReturn(Arrays.asList(syncHistory));

		List<SyncHistoryResponseDTO> result = syncHistoryService.getSyncHistoryByType(SyncType.FILE_UPLOAD);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(SyncType.FILE_UPLOAD, result.get(0).getSyncType());
		verify(historyRepository).findBySyncType(SyncType.FILE_UPLOAD);
	}

	@Test
	void getSyncHistoryByType_When_NoRecords_Expect_ReturnsEmptyList() {
		when(historyRepository.findBySyncType(SyncType.MANUAL))
				.thenReturn(List.of());

		List<SyncHistoryResponseDTO> result = syncHistoryService.getSyncHistoryByType(SyncType.MANUAL);

		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(historyRepository).findBySyncType(SyncType.MANUAL);
	}

	@Test
	void getSyncHistoryByUser_When_RecordsExist_Expect_ReturnsList() {
		when(historyRepository.findByInitiatedBy(userId))
				.thenReturn(Arrays.asList(syncHistory));

		List<SyncHistoryResponseDTO> result = syncHistoryService.getSyncHistoryByUser(userId);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(userId, result.get(0).getInitiatedBy());
		verify(historyRepository).findByInitiatedBy(userId);
	}

	@Test
	void getSyncHistoryByUser_When_NoRecords_Expect_ReturnsEmptyList() {
		UUID otherUserId = UUID.randomUUID();
		when(historyRepository.findByInitiatedBy(otherUserId))
				.thenReturn(List.of());

		List<SyncHistoryResponseDTO> result = syncHistoryService.getSyncHistoryByUser(otherUserId);

		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(historyRepository).findByInitiatedBy(otherUserId);
	}

	@Test
	void getSyncStatistics_When_NoDateRange_Expect_ReturnsStatistics() {
		LibreTimeFileSyncStatus syncedFile = LibreTimeFileSyncStatus.builder()
				.id(UUID.randomUUID())
				.syncStatus(SyncStatus.SYNCED)
				.lastSyncAt(LocalDateTime.now())
				.build();
		LibreTimeFileSyncStatus failedFile = LibreTimeFileSyncStatus.builder()
				.id(UUID.randomUUID())
				.syncStatus(SyncStatus.FAILED)
				.lastSyncAt(LocalDateTime.now())
				.build();

		LibreTimeSyncHistory completedHistory = LibreTimeSyncHistory.builder()
				.id(UUID.randomUUID())
				.status("completed")
				.startedAt(LocalDateTime.now())
				.build();

		when(fileSyncStatusRepository.findAll()).thenReturn(Arrays.asList(syncedFile, failedFile));
		when(historyRepository.findAll()).thenReturn(Arrays.asList(completedHistory));

		SyncStatisticsResponseDTO result = syncHistoryService.getSyncStatistics(null, null);

		assertNotNull(result);
		assertEquals(2, result.getTotalFiles());
		assertEquals(1, result.getSyncedFiles());
		assertEquals(1, result.getFailedFiles());
		assertEquals(1, result.getTotalSyncOperations());
		assertEquals(1, result.getSuccessfulSyncOperations());
		assertEquals(50.0, result.getSuccessRate(), 0.1);
		verify(fileSyncStatusRepository).findAll();
		verify(historyRepository).findAll();
	}

	@Test
	void getSyncStatistics_When_WithDateRange_Expect_ReturnsFilteredStatistics() {
		LocalDateTime startDate = LocalDateTime.now().minusDays(7);
		LocalDateTime endDate = LocalDateTime.now();

		LibreTimeFileSyncStatus syncedFile = LibreTimeFileSyncStatus.builder()
				.id(UUID.randomUUID())
				.syncStatus(SyncStatus.SYNCED)
				.lastSyncAt(LocalDateTime.now().minusDays(3))
				.build();

		LibreTimeSyncHistory completedHistory = LibreTimeSyncHistory.builder()
				.id(UUID.randomUUID())
				.status("completed")
				.startedAt(LocalDateTime.now().minusDays(3))
				.build();

		when(fileSyncStatusRepository.findAll()).thenReturn(Arrays.asList(syncedFile));
		when(historyRepository.findByStartedAtBetween(startDate, endDate))
				.thenReturn(Arrays.asList(completedHistory));

		SyncStatisticsResponseDTO result = syncHistoryService.getSyncStatistics(startDate, endDate);

		assertNotNull(result);
		assertEquals(1, result.getTotalFiles());
		assertEquals(1, result.getSyncedFiles());
		assertEquals(1, result.getTotalSyncOperations());
		verify(fileSyncStatusRepository).findAll();
		verify(historyRepository).findByStartedAtBetween(startDate, endDate);
	}

	@Test
	void getSyncStatistics_When_NoFiles_Expect_ReturnsZeroStatistics() {
		when(fileSyncStatusRepository.findAll()).thenReturn(List.of());
		when(historyRepository.findAll()).thenReturn(List.of());

		SyncStatisticsResponseDTO result = syncHistoryService.getSyncStatistics(null, null);

		assertNotNull(result);
		assertEquals(0, result.getTotalFiles());
		assertEquals(0, result.getSyncedFiles());
		assertEquals(0, result.getTotalSyncOperations());
		assertEquals(0.0, result.getSuccessRate());
	}

	@Test
	void getSyncStatistics_When_MixedStatuses_Expect_CalculatesCorrectly() {
		LibreTimeFileSyncStatus syncedFile = LibreTimeFileSyncStatus.builder()
				.id(UUID.randomUUID())
				.syncStatus(SyncStatus.SYNCED)
				.lastSyncAt(LocalDateTime.now())
				.build();
		LibreTimeFileSyncStatus pendingFile = LibreTimeFileSyncStatus.builder()
				.id(UUID.randomUUID())
				.syncStatus(SyncStatus.PENDING)
				.lastSyncAt(LocalDateTime.now())
				.build();
		LibreTimeFileSyncStatus conflictedFile = LibreTimeFileSyncStatus.builder()
				.id(UUID.randomUUID())
				.syncStatus(SyncStatus.CONFLICT)
				.lastSyncAt(LocalDateTime.now())
				.build();

		when(fileSyncStatusRepository.findAll())
				.thenReturn(Arrays.asList(syncedFile, pendingFile, conflictedFile));
		when(historyRepository.findAll()).thenReturn(List.of());

		SyncStatisticsResponseDTO result = syncHistoryService.getSyncStatistics(null, null);

		assertNotNull(result);
		assertEquals(3, result.getTotalFiles());
		assertEquals(1, result.getSyncedFiles());
		assertEquals(1, result.getPendingFiles());
		assertEquals(1, result.getConflictedFiles());
		assertEquals(0, result.getFailedFiles());
	}

	@Test
	void getSyncStatistics_When_FilesOutsideDateRange_Expect_ExcludesThem() {
		LocalDateTime startDate = LocalDateTime.now().minusDays(7);
		LocalDateTime endDate = LocalDateTime.now();

		LibreTimeFileSyncStatus inRangeFile = LibreTimeFileSyncStatus.builder()
				.id(UUID.randomUUID())
				.syncStatus(SyncStatus.SYNCED)
				.lastSyncAt(LocalDateTime.now().minusDays(3))
				.build();
		LibreTimeFileSyncStatus outOfRangeFile = LibreTimeFileSyncStatus.builder()
				.id(UUID.randomUUID())
				.syncStatus(SyncStatus.SYNCED)
				.lastSyncAt(LocalDateTime.now().minusDays(10))
				.build();

		when(fileSyncStatusRepository.findAll())
				.thenReturn(Arrays.asList(inRangeFile, outOfRangeFile));
		when(historyRepository.findByStartedAtBetween(startDate, endDate))
				.thenReturn(List.of());

		SyncStatisticsResponseDTO result = syncHistoryService.getSyncStatistics(startDate, endDate);

		assertNotNull(result);
		assertEquals(1, result.getTotalFiles()); // Only in-range file
		assertEquals(1, result.getSyncedFiles());
	}

	@Test
	void getSyncStatistics_When_FilesWithNullLastSyncAt_Expect_ExcludesThem() {
		LibreTimeFileSyncStatus fileWithSync = LibreTimeFileSyncStatus.builder()
				.id(UUID.randomUUID())
				.syncStatus(SyncStatus.SYNCED)
				.lastSyncAt(LocalDateTime.now())
				.build();
		LibreTimeFileSyncStatus fileWithoutSync = LibreTimeFileSyncStatus.builder()
				.id(UUID.randomUUID())
				.syncStatus(SyncStatus.PENDING)
				.lastSyncAt(null)
				.build();

		LocalDateTime startDate = LocalDateTime.now().minusDays(7);
		LocalDateTime endDate = LocalDateTime.now();

		when(fileSyncStatusRepository.findAll())
				.thenReturn(Arrays.asList(fileWithSync, fileWithoutSync));
		when(historyRepository.findByStartedAtBetween(startDate, endDate))
				.thenReturn(List.of());

		SyncStatisticsResponseDTO result = syncHistoryService.getSyncStatistics(startDate, endDate);

		assertNotNull(result);
		assertEquals(1, result.getTotalFiles()); // Only file with lastSyncAt
	}

}

