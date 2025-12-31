package com.onelpro.librelog.repositories;

import com.onelpro.librelog.enums.SyncDirection;
import com.onelpro.librelog.enums.SyncStatus;
import com.onelpro.librelog.models.LibreTimeFileSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for LibreTimeFileSyncStatus entity operations.
 */
@Repository
public interface LibreTimeFileSyncStatusRepository extends JpaRepository<LibreTimeFileSyncStatus, UUID> {

	/**
	 * Finds sync status by LibreLog file ID.
	 * 
	 * @param librelogFileId The LibreLog file ID
	 * @return Optional containing the sync status if found
	 */
	Optional<LibreTimeFileSyncStatus> findByLibrelogFileId(UUID librelogFileId);

	/**
	 * Finds sync status by LibreTime cart ID.
	 * 
	 * @param libreTimeCartId The LibreTime cart ID
	 * @return Optional containing the sync status if found
	 */
	Optional<LibreTimeFileSyncStatus> findByLibreTimeCartId(String libreTimeCartId);

	/**
	 * Finds all sync statuses by sync status.
	 * 
	 * @param syncStatus The sync status to filter by
	 * @return List of sync statuses
	 */
	List<LibreTimeFileSyncStatus> findBySyncStatus(SyncStatus syncStatus);

	/**
	 * Finds all sync statuses by sync direction.
	 * 
	 * @param syncDirection The sync direction to filter by
	 * @return List of sync statuses
	 */
	List<LibreTimeFileSyncStatus> findBySyncDirection(SyncDirection syncDirection);

	/**
	 * Finds all sync statuses by sync status and sync direction.
	 * 
	 * @param syncStatus The sync status to filter by
	 * @param syncDirection The sync direction to filter by
	 * @return List of sync statuses
	 */
	List<LibreTimeFileSyncStatus> findBySyncStatusAndSyncDirection(SyncStatus syncStatus, SyncDirection syncDirection);

	/**
	 * Finds sync status by file hash (for conflict detection).
	 * 
	 * @param fileHash The file hash to search for
	 * @return List of sync statuses with matching hash
	 */
	List<LibreTimeFileSyncStatus> findByFileHash(String fileHash);

}

