package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.ConflictResolution;
import com.onelpro.librelog.enums.SyncDirection;
import com.onelpro.librelog.enums.SyncFrequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for creating or updating LibreTime integration configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibreTimeIntegrationConfigRequestDTO {

	@NotBlank(message = "API base URL is required")
	private String apiBaseUrl;

	@NotBlank(message = "JWT token is required")
	private String jwtToken;

	@NotNull(message = "Sync enabled flag is required")
	private Boolean syncEnabled;

	@NotNull(message = "Sync frequency is required")
	private SyncFrequency syncFrequency;

	@NotNull(message = "Sync direction is required")
	private SyncDirection syncDirection;

	@NotNull(message = "Conflict resolution strategy is required")
	private ConflictResolution conflictResolution;

	private String webhookUrl;

	private String webhookSecret;

	@NotNull(message = "Webhook enabled flag is required")
	private Boolean webhookEnabled;

	@NotNull(message = "Max file size is required")
	@Min(value = 1, message = "Max file size must be at least 1 MB")
	@Max(value = 5000, message = "Max file size must not exceed 5000 MB")
	private Integer maxFileSizeMb;

	private List<String> supportedFormats; // JSON array will be converted to string

}

