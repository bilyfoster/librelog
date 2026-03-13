package com.onelpro.librelog.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing copy/script for a spot.
 * This contains the script text that voice talent or DJs read for advertisements.
 */
@Entity
@Table(name = "spot_copies", indexes = {
		@Index(name = "ix_spot_copies_campaign", columnList = "campaign_id"),
		@Index(name = "ix_spot_copies_version", columnList = "campaign_id, version_number")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotCopy {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "campaign_id", nullable = false)
	private Campaign campaign;

	@Column(name = "version_number", nullable = false)
	@Builder.Default
	private Integer versionNumber = 1;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "script_text", columnDefinition = "TEXT", nullable = false)
	private String scriptText;

	@Column(name = "instructions", columnDefinition = "TEXT")
	private String instructions; // Delivery instructions (tone, pace, emphasis)

	@Column(name = "duration_seconds")
	private Integer durationSeconds; // Target duration when read

	@Column(name = "status")
	@Builder.Default
	private String status = "DRAFT"; // DRAFT, PENDING_APPROVAL, APPROVED, REJECTED

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approved_by")
	private User approvedBy;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(name = "rejection_reason", columnDefinition = "TEXT")
	private String rejectionReason;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}
