package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.SyncType;
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
 * Entity representing a sync operation history record.
 */
@Entity
@Table(name = "libretime_sync_history")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LibreTimeSyncHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "sync_type", nullable = false, length = 50)
	private SyncType syncType;

	@Column(name = "status", nullable = false, length = 50)
	private String status; // started, completed, failed, cancelled

	@Column(name = "items_total", nullable = false)
	@Builder.Default
	private Integer itemsTotal = 0;

	@Column(name = "items_succeeded", nullable = false)
	@Builder.Default
	private Integer itemsSucceeded = 0;

	@Column(name = "items_failed", nullable = false)
	@Builder.Default
	private Integer itemsFailed = 0;

	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "error_summary", columnDefinition = "TEXT")
	private String errorSummary;

	@Column(name = "initiated_by", nullable = false)
	private UUID initiatedBy;

	@Column(name = "details", columnDefinition = "TEXT")
	private String details; // JSON string for additional details

}

