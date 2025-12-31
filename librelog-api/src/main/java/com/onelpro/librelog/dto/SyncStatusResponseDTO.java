package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.SyncDirection;
import com.onelpro.librelog.enums.SyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for file sync status response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatusResponseDTO {

	private UUID id;
	private UUID librelogFileId;
	private String libreTimeCartId;
	private String fileName;
	private SyncDirection syncDirection;
	private SyncStatus syncStatus;
	private LocalDateTime lastSyncAt;
	private String syncError;
	private Long fileSizeBytes;
	private String fileHash;
	private Boolean metadataSynced;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

}

