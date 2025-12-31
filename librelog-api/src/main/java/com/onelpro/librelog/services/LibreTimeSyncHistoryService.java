package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.SyncHistoryResponseDTO;
import com.onelpro.librelog.dto.SyncStatisticsResponseDTO;
import com.onelpro.librelog.enums.SyncType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for LibreTime sync history management.
 */
public interface LibreTimeSyncHistoryService {

	/**
	 * Creates a new sync history record.
	 * 
	 * @param syncType The type of sync operation
	 * @param initiatedBy The user ID who initiated the sync
	 * @param details Optional JSON details
	 * @return The created sync history record
	 */
	SyncHistoryResponseDTO createSyncHistory(SyncType syncType, UUID initiatedBy, String details);

	/**
	 * Updates a sync history record with completion status.
	 * 
	 * @param historyId The history record ID
	 * @param status The final status (completed, failed, cancelled)
	 * @param itemsSucceeded Number of items that succeeded
	 * @param itemsFailed Number of items that failed
	 * @param errorSummary Optional error summary
	 */
	void completeSyncHistory(UUID historyId, String status, Integer itemsSucceeded, Integer itemsFailed, String errorSummary);

	/**
	 * Gets a sync history record by ID.
	 * 
	 * @param historyId The history record ID
	 * @return The sync history record
	 */
	SyncHistoryResponseDTO getSyncHistory(UUID historyId);

	/**
	 * Gets sync history records within a date range.
	 * 
	 * @param startDate Start date
	 * @param endDate End date
	 * @return List of sync history records
	 */
	List<SyncHistoryResponseDTO> getSyncHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate);

	/**
	 * Gets sync history records by sync type.
	 * 
	 * @param syncType The sync type
	 * @return List of sync history records
	 */
	List<SyncHistoryResponseDTO> getSyncHistoryByType(SyncType syncType);

	/**
	 * Gets sync history records by user.
	 * 
	 * @param userId The user ID
	 * @return List of sync history records
	 */
	List<SyncHistoryResponseDTO> getSyncHistoryByUser(UUID userId);

	/**
	 * Gets sync statistics.
	 * 
	 * @param startDate Optional start date for statistics calculation
	 * @param endDate Optional end date for statistics calculation
	 * @return Sync statistics
	 */
	SyncStatisticsResponseDTO getSyncStatistics(LocalDateTime startDate, LocalDateTime endDate);

}

