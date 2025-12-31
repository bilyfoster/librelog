package com.onelpro.librelog.enums;

/**
 * Represents the conflict resolution strategies for handling synchronization conflicts between LibreLog and LibreTime.
 */
public enum ConflictResolution {
	LAST_WRITE_WINS,
	MANUAL,
	LIBRELOG_PRIORITY,
	LIBRETIME_PRIORITY
}

