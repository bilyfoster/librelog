package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.ConflictResolution;
import com.onelpro.librelog.enums.SyncDirection;
import com.onelpro.librelog.enums.SyncFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for LibreTime integration configuration response.
 * Sensitive fields (JWT token, webhook secret) are masked for security.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibreTimeIntegrationConfigResponseDTO {

	private UUID id;
	private String apiBaseUrl;
	private String jwtToken; // Masked (shows only last 4 characters)
	private Boolean syncEnabled;
	private SyncFrequency syncFrequency;
	private SyncDirection syncDirection;
	private ConflictResolution conflictResolution;
	private String webhookUrl;
	private String webhookSecret; // Masked (shows only last 4 characters)
	private Boolean webhookEnabled;
	private Integer maxFileSizeMb;
	private List<String> supportedFormats;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private UUID createdById;
	private UUID updatedById;

}

