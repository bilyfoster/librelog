package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.EndpointStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for LibreTime API endpoint information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpointResponseDTO {

	private UUID id;
	private String endpointPath;
	private String httpMethod;
	private String resourceType;
	private EndpointStatus status;
	private LocalDateTime lastTestedAt;
	private Integer responseTimeMs;
	private Boolean requiresAuthentication;
	private String description;
	private String documentationUrl;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

}

