package com.onelpro.librelog.enums;

/**
 * Represents the status of a file synchronization operation between LibreLog and LibreTime.
 */
public enum SyncStatus {
	PENDING,
	SYNCING,
	SYNCED,
	FAILED,
	CONFLICT
}

