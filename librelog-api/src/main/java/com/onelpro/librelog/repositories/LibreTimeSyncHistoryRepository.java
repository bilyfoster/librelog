package com.onelpro.librelog.repositories;

import com.onelpro.librelog.enums.SyncType;
import com.onelpro.librelog.models.LibreTimeSyncHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for LibreTimeSyncHistory entity operations.
 */
@Repository
public interface LibreTimeSyncHistoryRepository extends JpaRepository<LibreTimeSyncHistory, UUID> {

	/**
	 * Finds all sync history records by sync type.
	 * 
	 * @param syncType The sync type
	 * @return List of sync history records
	 */
	List<LibreTimeSyncHistory> findBySyncType(SyncType syncType);

	/**
	 * Finds all sync history records by status.
	 * 
	 * @param status The status
	 * @return List of sync history records
	 */
	List<LibreTimeSyncHistory> findByStatus(String status);

	/**
	 * Finds all sync history records within a date range.
	 * 
	 * @param startDate Start date
	 * @param endDate End date
	 * @return List of sync history records
	 */
	List<LibreTimeSyncHistory> findByStartedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

	/**
	 * Finds all sync history records initiated by a specific user.
	 * 
	 * @param userId The user ID
	 * @return List of sync history records
	 */
	List<LibreTimeSyncHistory> findByInitiatedBy(UUID userId);

	/**
	 * Finds sync history records by sync type and status.
	 * 
	 * @param syncType The sync type
	 * @param status The status
	 * @return List of sync history records
	 */
	List<LibreTimeSyncHistory> findBySyncTypeAndStatus(SyncType syncType, String status);

	/**
	 * Finds sync history records by sync type within a date range.
	 * 
	 * @param syncType The sync type
	 * @param startDate Start date
	 * @param endDate End date
	 * @return List of sync history records
	 */
	List<LibreTimeSyncHistory> findBySyncTypeAndStartedAtBetween(
			SyncType syncType, LocalDateTime startDate, LocalDateTime endDate);

	/**
	 * Finds the most recent sync history record for a sync type.
	 * 
	 * @param syncType The sync type
	 * @return Optional sync history record
	 */
	java.util.Optional<LibreTimeSyncHistory> findFirstBySyncTypeOrderByStartedAtDesc(SyncType syncType);

}

