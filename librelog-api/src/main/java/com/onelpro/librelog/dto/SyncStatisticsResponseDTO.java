package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for sync statistics information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatisticsResponseDTO {

	private Integer totalFiles;
	private Integer syncedFiles;
	private Integer failedFiles;
	private Integer pendingFiles;
	private Integer conflictedFiles;
	private Double successRate; // Percentage (0-100)
	private Integer totalSyncOperations;
	private Integer successfulSyncOperations;
	private Integer failedSyncOperations;
	private LocalDateTime lastSyncAt;
	private LocalDateTime calculatedAt;

}

