package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.dto.SyncHistoryResponseDTO;
import com.onelpro.librelog.dto.SyncStatisticsResponseDTO;
import com.onelpro.librelog.enums.SyncStatus;
import com.onelpro.librelog.enums.SyncType;
import com.onelpro.librelog.models.LibreTimeFileSyncStatus;
import com.onelpro.librelog.models.LibreTimeSyncHistory;
import com.onelpro.librelog.repositories.LibreTimeFileSyncStatusRepository;
import com.onelpro.librelog.repositories.LibreTimeSyncHistoryRepository;
import com.onelpro.librelog.services.LibreTimeSyncHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the LibreTime sync history service.
 * Handles sync history tracking and statistics calculation.
 */
@Service
public class LibreTimeSyncHistoryServiceImpl implements LibreTimeSyncHistoryService {

	private static final Logger logger = LoggerFactory.getLogger(LibreTimeSyncHistoryServiceImpl.class);

	private final LibreTimeSyncHistoryRepository historyRepository;
	private final LibreTimeFileSyncStatusRepository fileSyncStatusRepository;

	public LibreTimeSyncHistoryServiceImpl(
			LibreTimeSyncHistoryRepository historyRepository,
			LibreTimeFileSyncStatusRepository fileSyncStatusRepository) {
		this.historyRepository = historyRepository;
		this.fileSyncStatusRepository = fileSyncStatusRepository;
	}

	@Override
	@Transactional
	public SyncHistoryResponseDTO createSyncHistory(SyncType syncType, UUID initiatedBy, String details) {
		logger.info("Creating sync history record for type: {}, initiated by: {}", syncType, initiatedBy);

		LibreTimeSyncHistory history = LibreTimeSyncHistory.builder()
				.syncType(syncType)
				.status("started")
				.itemsTotal(0)
				.itemsSucceeded(0)
				.itemsFailed(0)
				.startedAt(LocalDateTime.now())
				.initiatedBy(initiatedBy)
				.details(details)
				.build();

		history = historyRepository.save(history);
		logger.info("Sync history record created with ID: {}", history.getId());
		return mapToDTO(history);
	}

	@Override
	@Transactional
	public void completeSyncHistory(UUID historyId, String status, Integer itemsSucceeded, Integer itemsFailed, String errorSummary) {
		logger.info("Completing sync history record: {} with status: {}", historyId, status);

		LibreTimeSyncHistory history = historyRepository.findById(historyId)
				.orElseThrow(() -> new com.onelpro.librelog.exceptions.NotFoundException("Sync history not found: " + historyId));

		history.setStatus(status);
		history.setItemsSucceeded(itemsSucceeded);
		history.setItemsFailed(itemsFailed);
		history.setItemsTotal(itemsSucceeded + itemsFailed);
		history.setCompletedAt(LocalDateTime.now());
		history.setErrorSummary(errorSummary);

		historyRepository.save(history);
		logger.info("Sync history record completed: {}", historyId);
	}

	@Override
	public SyncHistoryResponseDTO getSyncHistory(UUID historyId) {
		logger.debug("Fetching sync history record: {}", historyId);
		return historyRepository.findById(historyId)
				.map(this::mapToDTO)
				.orElseThrow(() -> new com.onelpro.librelog.exceptions.NotFoundException("Sync history not found: " + historyId));
	}

	@Override
	public List<SyncHistoryResponseDTO> getSyncHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
		logger.debug("Fetching sync history records from {} to {}", startDate, endDate);
		return historyRepository.findByStartedAtBetween(startDate, endDate).stream()
				.map(this::mapToDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<SyncHistoryResponseDTO> getSyncHistoryByType(SyncType syncType) {
		logger.debug("Fetching sync history records for type: {}", syncType);
		return historyRepository.findBySyncType(syncType).stream()
				.map(this::mapToDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<SyncHistoryResponseDTO> getSyncHistoryByUser(UUID userId) {
		logger.debug("Fetching sync history records for user: {}", userId);
		return historyRepository.findByInitiatedBy(userId).stream()
				.map(this::mapToDTO)
				.collect(Collectors.toList());
	}

	@Override
	public SyncStatisticsResponseDTO getSyncStatistics(LocalDateTime startDate, LocalDateTime endDate) {
		logger.info("Calculating sync statistics from {} to {}", startDate, endDate);

		// Get file sync status counts
		List<LibreTimeFileSyncStatus> allFileStatuses = fileSyncStatusRepository.findAll();
		
		// Filter by date range if provided
		if (startDate != null && endDate != null) {
			allFileStatuses = allFileStatuses.stream()
					.filter(status -> {
						if (status.getLastSyncAt() == null) {
							return false;
						}
						return !status.getLastSyncAt().isBefore(startDate) && !status.getLastSyncAt().isAfter(endDate);
					})
					.collect(Collectors.toList());
		}

		int totalFiles = allFileStatuses.size();
		long syncedFiles = allFileStatuses.stream()
				.filter(s -> s.getSyncStatus() == SyncStatus.SYNCED)
				.count();
		long failedFiles = allFileStatuses.stream()
				.filter(s -> s.getSyncStatus() == SyncStatus.FAILED)
				.count();
		long pendingFiles = allFileStatuses.stream()
				.filter(s -> s.getSyncStatus() == SyncStatus.PENDING)
				.count();
		long conflictedFiles = allFileStatuses.stream()
				.filter(s -> s.getSyncStatus() == SyncStatus.CONFLICT)
				.count();

		// Get sync history statistics
		List<LibreTimeSyncHistory> allHistory;
		if (startDate != null && endDate != null) {
			allHistory = historyRepository.findByStartedAtBetween(startDate, endDate);
		} else {
			allHistory = historyRepository.findAll();
		}

		int totalSyncOperations = allHistory.size();
		long successfulSyncOperations = allHistory.stream()
				.filter(h -> "completed".equals(h.getStatus()))
				.count();
		long failedSyncOperations = allHistory.stream()
				.filter(h -> "failed".equals(h.getStatus()))
				.count();

		// Calculate success rate
		double successRate = 0.0;
		if (totalFiles > 0) {
			successRate = (syncedFiles * 100.0) / totalFiles;
		}

		// Get last sync time
		LocalDateTime lastSyncAt = allFileStatuses.stream()
				.filter(s -> s.getLastSyncAt() != null)
				.map(LibreTimeFileSyncStatus::getLastSyncAt)
				.max(LocalDateTime::compareTo)
				.orElse(null);

		return SyncStatisticsResponseDTO.builder()
				.totalFiles(totalFiles)
				.syncedFiles((int) syncedFiles)
				.failedFiles((int) failedFiles)
				.pendingFiles((int) pendingFiles)
				.conflictedFiles((int) conflictedFiles)
				.successRate(successRate)
				.totalSyncOperations(totalSyncOperations)
				.successfulSyncOperations((int) successfulSyncOperations)
				.failedSyncOperations((int) failedSyncOperations)
				.lastSyncAt(lastSyncAt)
				.calculatedAt(LocalDateTime.now())
				.build();
	}

	private SyncHistoryResponseDTO mapToDTO(LibreTimeSyncHistory history) {
		return SyncHistoryResponseDTO.builder()
				.id(history.getId())
				.syncType(history.getSyncType())
				.status(history.getStatus())
				.itemsTotal(history.getItemsTotal())
				.itemsSucceeded(history.getItemsSucceeded())
				.itemsFailed(history.getItemsFailed())
				.startedAt(history.getStartedAt())
				.completedAt(history.getCompletedAt())
				.errorSummary(history.getErrorSummary())
				.initiatedBy(history.getInitiatedBy())
				.details(history.getDetails())
				.build();
	}

}

