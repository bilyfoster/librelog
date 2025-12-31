package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.SyncDirection;
import com.onelpro.librelog.enums.SyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing the synchronization status of a file between LibreLog and LibreTime.
 */
@Entity
@Table(name = "libretime_file_sync_status")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LibreTimeFileSyncStatus {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "librelog_file_id")
	private UUID librelogFileId;

	@Column(name = "libretime_cart_id", length = 255)
	private String libreTimeCartId;

	@Column(name = "file_name", nullable = false, length = 500)
	private String fileName;

	@Enumerated(EnumType.STRING)
	@Column(name = "sync_direction", nullable = false, length = 50)
	private SyncDirection syncDirection;

	@Enumerated(EnumType.STRING)
	@Column(name = "sync_status", nullable = false, length = 50)
	private SyncStatus syncStatus;

	@Column(name = "last_sync_at")
	private LocalDateTime lastSyncAt;

	@Column(name = "sync_error", columnDefinition = "TEXT")
	private String syncError;

	@Column(name = "file_size_bytes")
	private Long fileSizeBytes;

	@Column(name = "file_hash", length = 255)
	private String fileHash;

	@Column(name = "metadata_synced", nullable = false)
	private Boolean metadataSynced;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

