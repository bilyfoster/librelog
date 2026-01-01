package com.onelpro.librelog.models;

import com.onelpro.librelog.enums.ConflictResolution;
import com.onelpro.librelog.enums.SyncDirection;
import com.onelpro.librelog.enums.SyncFrequency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * Entity representing LibreTime API integration configuration settings.
 */
@Entity
@Table(name = "libretime_integration_config")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LibreTimeIntegrationConfig {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "api_base_url", nullable = false, length = 500)
	private String apiBaseUrl;

	@Column(name = "jwt_token", nullable = false, length = 1000)
	private String jwtToken;

	@Column(name = "sync_enabled", nullable = false)
	private Boolean syncEnabled;

	@Enumerated(EnumType.STRING)
	@Column(name = "sync_frequency", nullable = false, length = 50)
	private SyncFrequency syncFrequency;

	@Enumerated(EnumType.STRING)
	@Column(name = "sync_direction", nullable = false, length = 50)
	private SyncDirection syncDirection;

	@Enumerated(EnumType.STRING)
	@Column(name = "conflict_resolution", nullable = false, length = 50)
	private ConflictResolution conflictResolution;

	@Column(name = "webhook_url", length = 500)
	private String webhookUrl;

	@Column(name = "webhook_secret", length = 500)
	private String webhookSecret;

	@Column(name = "webhook_enabled", nullable = false)
	private Boolean webhookEnabled;

	@Column(name = "max_file_size_mb", nullable = false)
	private Integer maxFileSizeMb;

	@Column(name = "supported_formats", columnDefinition = "TEXT")
	private String supportedFormats; // JSON array stored as string

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@ManyToOne
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	@ManyToOne
	@JoinColumn(name = "updated_by")
	private User updatedBy;

	@ManyToOne
	@JoinColumn(name = "station_id", nullable = false)
	private Station station;

}

