package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for LibreTime API connection test response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResponseDTO {

	private Boolean success;
	private String message;
	private Integer responseTimeMs;
	private LocalDateTime testedAt;
	private String errorDetails; // Only populated if connection failed

}

