package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.SyncType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for sync history information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncHistoryResponseDTO {

	private UUID id;
	private SyncType syncType;
	private String status;
	private Integer itemsTotal;
	private Integer itemsSucceeded;
	private Integer itemsFailed;
	private LocalDateTime startedAt;
	private LocalDateTime completedAt;
	private String errorSummary;
	private UUID initiatedBy;
	private String details;

}

